package com.aiaccounting.data

import com.aiaccounting.data.local.AccountEntity
import com.aiaccounting.data.local.AppDatabase
import com.aiaccounting.data.local.CategoryEntity
import com.aiaccounting.data.local.MerchantRuleEntity
import com.aiaccounting.data.local.RawCaptureEntity
import com.aiaccounting.data.local.TransactionEntity
import com.aiaccounting.domain.AlipayBillParser
import com.aiaccounting.domain.BillRow
import com.aiaccounting.domain.BackupCsv
import com.aiaccounting.domain.DedupKey
import com.aiaccounting.domain.DiagnosticsCsv
import com.aiaccounting.domain.MerchantMatcher
import com.aiaccounting.domain.PaymentParser
import com.aiaccounting.domain.Tx
import com.aiaccounting.domain.TxSource
import com.aiaccounting.domain.TxType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.map
import androidx.room.withTransaction

class AccountingRepository(
    private val db: AppDatabase,
    private val now: () -> Long = System::currentTimeMillis,
) {

    private val transactionDao = db.transactionDao()
    private val categoryDao = db.categoryDao()
    private val accountDao = db.accountDao()
    private val rawCaptureDao = db.rawCaptureDao()
    private val merchantRuleDao = db.merchantRuleDao()

    /** 种子写入必须串行：Application、界面、后台服务会并发调用。 */
    private val seedMutex = kotlinx.coroutines.sync.Mutex()

    /**
     * 首次启动写入种子数据；已写过则什么都不做。
     *
     * 加锁是必要的：之前没锁，全新安装时分类被写了两遍（真机出现过 30 个分类）。
     */
    suspend fun ensureSeeded() = seedMutex.withLock {
        if (categoryDao.count() == 0) categoryDao.insertAll(SeedData.allCategories)
        if (accountDao.count() == 0) accountDao.insertAll(SeedData.accounts)
    }

    // ── 自动捕捉 ────────────────────────────────────────────────────────

    fun observePending(): Flow<List<Tx>> = combine(
        transactionDao.observePending(),
        categoryDao.observeAll(),
        accountDao.observeAll(),
    ) { transactions, categories, accounts ->
        val categoryMap = categories.associateBy { it.id }
        val accountMap = accounts.associateBy { it.id }
        transactions.map { it.toTx(categoryMap[it.categoryId], accountMap[it.accountId]) }
    }

    suspend fun confirmTransaction(id: Long) =
        transactionDao.updateStatus(id, "CONFIRMED", now())

    suspend fun ignoreTransaction(id: Long) = transactionDao.deleteById(id)

    suspend fun recordRawCapture(
        packageName: String,
        title: String?,
        text: String?,
        parsed: Boolean,
        reason: String? = null,
    ) {
        rawCaptureDao.insert(
            RawCaptureEntity(
                packageName = packageName,
                title = title,
                text = text,
                capturedAt = now(),
                parsed = parsed,
                reason = reason,
            ),
        )
    }

    suspend fun latestRawCaptures(limit: Int = 20): List<RawCaptureEntity> =
        rawCaptureDao.latest(limit)

    suspend fun accountIdByName(name: String): Long? =
        accountDao.all().firstOrNull { it.name == name }?.id

    suspend fun defaultCategoryId(type: TxType): Long? =
        categoryDao.byKind(type.name).firstOrNull { it.name == "其他" }?.id
            ?: categoryDao.byKind(type.name).firstOrNull()?.id

    // ── 商户 → 分类自学习 ───────────────────────────────────────────────

    /** 记住「这个商户属于哪个分类」。商户名为空或归一化后为空就跳过。 */
    suspend fun saveMerchantRule(merchant: String?, categoryId: Long) {
        val pattern = MerchantMatcher.normalize(merchant.orEmpty())
        if (pattern.isEmpty()) return
        val existing = merchantRuleDao.all().firstOrNull { it.pattern == pattern }
        merchantRuleDao.upsert(
            MerchantRuleEntity(
                id = existing?.id ?: 0,
                pattern = pattern,
                categoryId = categoryId,
                hitCount = (existing?.hitCount ?: 0) + 1,
                updatedAt = now(),
            ),
        )
    }

    /** 优先用学到的规则，其次回落到该类型的默认分类（其他）。 */
    suspend fun resolveCategoryId(merchant: String?, type: TxType): Long? {
        val rules = merchantRuleDao.all().map { MerchantMatcher.Rule(it.pattern, it.categoryId) }
        val matched = MerchantMatcher.bestMatch(rules, merchant)
        if (matched != null && categoryDao.byKind(type.name).any { it.id == matched }) {
            return matched
        }
        return defaultCategoryId(type)
    }

    suspend fun merchantRuleCount(): Int = merchantRuleDao.count()

    // ── 账单导入 ────────────────────────────────────────────────────────

    data class BillImportPreview(
        val total: Int,
        val importable: List<BillRow>,
        val duplicates: Int,
    )

    /** 先算一遍「能导几笔、重复几笔」，让用户确认后再落库。 */
    suspend fun previewBillImport(
        rows: List<BillRow>,
        windowMillis: Long = 10 * 60 * 1000,
    ): BillImportPreview {
        val existingKeys = mutableSetOf<String>()
        val importable = rows.filter { row ->
            val key = dedupKeyOf(row)
            val duplicated = when {
                key != null && transactionDao.findByDedupKey(key) != null -> true
                else -> transactionDao.findSimilar(
                    row.amountCents,
                    row.occurredAt - windowMillis,
                    row.occurredAt + windowMillis,
                ) != null
            }
            if (!duplicated) existingKeys.add(key ?: "") else Unit
            !duplicated
        }
        return BillImportPreview(
            total = rows.size,
            importable = importable,
            duplicates = rows.size - importable.size,
        )
    }

    /** 落库：来源标记 IMPORT，直接确认（账单本身就是已完成的交易）。 */
    suspend fun applyBillImport(
        rows: List<BillRow>,
        accountName: String = "支付宝",
    ): Int {
        if (rows.isEmpty()) return 0
        ensureSeeded()
        val accountId = accountIdByName(accountName)
            ?: accountDao.all().firstOrNull()?.id
            ?: return 0

        var inserted = 0
        db.withTransaction {
            rows.forEach { row ->
                // 商户之前学过分类就直接命中，否则落到「其他」
                val merchant = row.counterparty ?: row.description
                val categoryId = resolveCategoryId(merchant, row.type) ?: return@forEach
                transactionDao.insert(
                    TransactionEntity(
                        type = row.type.name,
                        amountCents = row.amountCents,
                        occurredAt = row.occurredAt,
                        categoryId = categoryId,
                        accountId = accountId,
                        merchant = merchant,
                        note = row.description?.takeIf { it != row.counterparty },
                        source = TxSource.IMPORT.name,
                        dedupKey = dedupKeyOf(row),
                        status = "CONFIRMED",
                    ),
                )
                inserted++
            }
        }
        return inserted
    }

    private fun dedupKeyOf(row: BillRow): String? =
        row.tradeNo?.let { DedupKey.fromChannel(it) }

    fun observeTransactions(): Flow<List<Tx>> = combine(
        transactionDao.observeAll(),
        categoryDao.observeAll(),
        accountDao.observeAll(),
    ) { transactions, categories, accounts ->
        val categoryMap = categories.associateBy { it.id }
        val accountMap = accounts.associateBy { it.id }
        transactions.map { it.toTx(categoryMap[it.categoryId], accountMap[it.accountId]) }
    }

    fun observeCategories(kind: TxType): Flow<List<CategoryEntity>> =
        categoryDao.observeByKind(kind.name)

    fun observeAccounts(): Flow<List<AccountEntity>> = accountDao.observeAll()

    suspend fun categoriesOf(kind: TxType): List<CategoryEntity> = categoryDao.byKind(kind.name)

    // ── 分类管理 ────────────────────────────────────────────────────────

    /** 新建分类。同名（同类型）已存在时返回 null。 */
    suspend fun addCategory(name: String, emoji: String, kind: TxType): CategoryEntity? {
        val trimmed = name.trim().take(8)
        if (trimmed.isEmpty()) return null
        if (categoryDao.byKind(kind.name).any { it.name == trimmed }) return null
        val order = categoryDao.maxSortOrder(kind.name) + 1
        val id = categoryDao.insert(
            CategoryEntity(
                name = trimmed,
                emoji = emoji,
                kind = kind.name,
                sortOrder = order,
                isBuiltIn = false,
            ),
        )
        return categoryDao.findById(id)
    }

    /** 改名 / 换图标。 */
    suspend fun updateCategory(id: Long, name: String, emoji: String): Boolean {
        val existing = categoryDao.findById(id) ?: return false
        val trimmed = name.trim().take(8)
        if (trimmed.isEmpty()) return false
        categoryDao.update(existing.copy(name = trimmed, emoji = emoji))
        return true
    }

    /** 删除分类；有记录时拒绝，避免把账目变成孤儿。 */
    suspend fun deleteCategory(id: Long): DeleteCategoryResult {
        val existing = categoryDao.findById(id) ?: return DeleteCategoryResult.NotFound
        if (existing.isBuiltIn) return DeleteCategoryResult.BuiltIn
        val used = categoryDao.usageCount(id)
        if (used > 0) return DeleteCategoryResult.InUse(used)
        categoryDao.deleteById(id)
        return DeleteCategoryResult.Deleted
    }

    suspend fun categoryUsage(id: Long): Int = categoryDao.usageCount(id)

    /** 已学习的商户规则（设置里可查看、可删除）。 */
    suspend fun merchantRules(): List<MerchantRuleEntity> = merchantRuleDao.all()

    fun observeMerchantRules(): Flow<List<MerchantRuleEntity>> = merchantRuleDao.observeAll()

    // ── 诊断数据（商户规则 / 捕捉日志）─────────────────────────────────

    /** 导出「商户 → 分类」规则，供云备份使用。 */
    suspend fun exportRulesCsv(): String {
        val categories = categoryDao.all().associateBy { it.id }
        val rows = merchantRuleDao.all()
            .sortedByDescending { it.hitCount }
            .map { rule ->
                DiagnosticsCsv.RuleRow(
                    merchant = rule.pattern,
                    categoryName = categories[rule.categoryId]?.name ?: "其他",
                    hitCount = rule.hitCount,
                )
            }
        return DiagnosticsCsv.encodeRules(rows)
    }

    /**
     * 导入商户规则。按分类名匹配到本机分类，整表替换（规则本来就是学习结果，不需要合并）。
     * @return 实际写入的条数
     */
    suspend fun importRulesCsv(text: String): Int {
        val rows = runCatching { DiagnosticsCsv.decodeRules(text) }.getOrNull() ?: return 0
        if (rows.isEmpty()) return 0
        val categories = categoryDao.all()
        var imported = 0
        db.withTransaction {
            merchantRuleDao.clear()
            rows.forEach { row ->
                val category = categories.firstOrNull { it.name == row.categoryName } ?: return@forEach
                merchantRuleDao.upsert(
                    MerchantRuleEntity(
                        pattern = row.merchant,
                        categoryId = category.id,
                        hitCount = row.hitCount,
                        updatedAt = now(),
                    ),
                )
                imported++
            }
        }
        return imported
    }

    /** 导出最近的捕捉日志（诊断用，不进自动备份）。 */
    suspend fun exportCapturesCsv(limit: Int = 300): String {
        val rows = rawCaptureDao.latest(limit).map { capture ->
            DiagnosticsCsv.CaptureRow(
                capturedAt = capture.capturedAt,
                appLabel = PaymentParser.channelOf(capture.packageName).label,
                packageName = capture.packageName,
                title = capture.title,
                text = capture.text,
                parsed = capture.parsed,
                reason = capture.reason,
            )
        }
        return DiagnosticsCsv.encodeCaptures(rows.sortedBy { it.capturedAt })
    }

    suspend fun deleteMerchantRule(pattern: String) {
        merchantRuleDao.deleteByPattern(pattern)
    }

    sealed interface DeleteCategoryResult {
        data object Deleted : DeleteCategoryResult
        data object BuiltIn : DeleteCategoryResult
        data object NotFound : DeleteCategoryResult
        data class InUse(val count: Int) : DeleteCategoryResult
    }

    suspend fun accounts(): List<AccountEntity> = accountDao.all()

    /** 手动记一笔。手动录入没有渠道单号，dedupKey 留空。 */
    suspend fun addManualTransaction(
        type: TxType,
        amountCents: Long,
        categoryId: Long,
        accountId: Long,
        occurredAt: Long = now(),
        merchant: String? = null,
        note: String? = null,
    ): Long = transactionDao.insert(
        TransactionEntity(
            type = type.name,
            amountCents = amountCents,
            occurredAt = occurredAt,
            categoryId = categoryId,
            accountId = accountId,
            merchant = merchant,
            note = note,
            source = TxSource.MANUAL.name,
            dedupKey = null,
            status = "CONFIRMED",
        ),
    )

    /**
     * 编辑已有记录：只改用户可改的字段，来源、去重指纹、**商户名**保持原样。
     * 分类被改过的话，顺手把这个商户 → 分类记下来（自学习）。
     */
    suspend fun updateTransaction(
        id: Long,
        type: TxType,
        amountCents: Long,
        categoryId: Long,
        accountId: Long,
        occurredAt: Long,
        note: String? = null,
    ) {
        val existing = transactionDao.findById(id) ?: return
        transactionDao.update(
            existing.copy(
                type = type.name,
                amountCents = amountCents,
                categoryId = categoryId,
                accountId = accountId,
                occurredAt = occurredAt,
                note = note,
                updatedAt = now(),
            ),
        )
        if (existing.categoryId != categoryId) {
            saveMerchantRule(existing.merchant, categoryId)
        }
    }

    suspend fun deleteTransaction(id: Long) = transactionDao.deleteById(id)

    /**
     * 自动捕捉（通知 / 导入）入库入口。
     * 命中已有指纹时直接返回已有记录的 id，不重复插入——这是三路来源收敛的关键。
     */
    suspend fun addCapturedTransaction(
        type: TxType,
        amountCents: Long,
        categoryId: Long,
        accountId: Long,
        occurredAt: Long,
        merchant: String?,
        note: String? = null,
        source: TxSource = TxSource.NOTIFICATION,
        tradeNo: String? = null,
    ): Long {
        val key = tradeNo?.let { DedupKey.fromChannel(it) }
            ?: DedupKey.fallback(amountCents, occurredAt, merchant)

        transactionDao.findByDedupKey(key)?.let { return it.id }

        // 第二道判重：同一笔订单可能被通知和读屏各抓一次（商户名不同、时间差几十分钟），
        // 光靠指纹拦不住，这里按「金额 + 2 小时窗 + 自动来源」再兜一次。
        transactionDao.findAutoDuplicate(
            amount = amountCents,
            from = occurredAt - CROSS_SOURCE_WINDOW_MS,
            to = occurredAt + CROSS_SOURCE_WINDOW_MS,
        )?.let { return it.id }

        return transactionDao.insert(
            TransactionEntity(
                type = type.name,
                amountCents = amountCents,
                occurredAt = occurredAt,
                categoryId = categoryId,
                accountId = accountId,
                merchant = merchant,
                note = note,
                source = source.name,
                dedupKey = key,
                status = "PENDING",
            ),
        )
    }

    suspend fun transactionCount(): Int = transactionDao.count()

    private companion object {
        /**
         * 跨来源判重的时间窗。
         *
         * 通知和读屏抓到同一笔可能相差几十分钟（取决于用户什么时候点开订单页），
         * 10 分钟不够用；放太大又会把「同一天两笔同价订单」误合并。
         * 2 小时是当前取舍——宁可少记一笔重复的，也不要把两笔真账并成一笔。
         */
        const val CROSS_SOURCE_WINDOW_MS = 2 * 60 * 60 * 1000L
    }

    /** 导出全部流水为 CSV 文本（用分类名/账户名，换设备也能恢复）。 */
    suspend fun exportBackupCsv(): String {
        val categories = categoryDao.all().associateBy { it.id }
        val accounts = accountDao.all().associateBy { it.id }
        val rows = transactionDao.all().map { entity ->
            BackupCsv.Row(
                occurredAt = entity.occurredAt,
                type = runCatching { TxType.valueOf(entity.type) }.getOrDefault(TxType.EXPENSE),
                amountCents = entity.amountCents,
                category = categories[entity.categoryId]?.name ?: "其他",
                account = accounts[entity.accountId]?.name ?: "现金",
                merchant = entity.merchant,
                note = entity.note,
                source = runCatching { TxSource.valueOf(entity.source) }.getOrDefault(TxSource.MANUAL),
                dedupKey = entity.dedupKey,
            )
        }
        return BackupCsv.encode(rows)
    }

    /**
     * 用备份文件覆盖当前流水（原子操作：先清空再写入）。
     * @return 写入的条数
     */
    suspend fun importBackupCsv(text: String): Int {
        val rows = BackupCsv.decode(text)
        val categories = categoryDao.all()
        val accounts = accountDao.all()

        fun resolveCategory(name: String, type: TxType): Long? =
            categories.firstOrNull { it.name == name && it.kind == type.name }?.id
                ?: categories.firstOrNull { it.name == "其他" && it.kind == type.name }?.id
                ?: categories.firstOrNull { it.kind == type.name }?.id

        fun resolveAccount(name: String): Long? =
            accounts.firstOrNull { it.name == name }?.id ?: accounts.firstOrNull()?.id

        db.withTransaction {
            transactionDao.deleteAll()
            rows.forEach { row ->
                val categoryId = resolveCategory(row.category, row.type) ?: return@forEach
                val accountId = resolveAccount(row.account) ?: return@forEach
                transactionDao.insert(
                    TransactionEntity(
                        type = row.type.name,
                        amountCents = row.amountCents,
                        occurredAt = row.occurredAt,
                        categoryId = categoryId,
                        accountId = accountId,
                        merchant = row.merchant,
                        note = row.note,
                        source = row.source.name,
                        dedupKey = row.dedupKey,
                        status = "CONFIRMED",
                    ),
                )
            }
        }
        return rows.size
    }

    private fun TransactionEntity.toTx(category: CategoryEntity?, account: AccountEntity?): Tx = Tx(
        id = id,
        type = runCatching { TxType.valueOf(type) }.getOrDefault(TxType.EXPENSE),
        amountCents = amountCents,
        occurredAt = occurredAt,
        categoryId = categoryId,
        categoryName = category?.name ?: "未分类",
        categoryEmoji = category?.emoji ?: "❓",
        accountId = accountId,
        accountName = account?.name ?: "未知账户",
        merchant = merchant,
        note = note,
        source = runCatching { TxSource.valueOf(source) }.getOrDefault(TxSource.MANUAL),
    )
}
