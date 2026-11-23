package com.aiaccounting.data

import com.aiaccounting.data.local.AccountEntity
import com.aiaccounting.data.local.AppDatabase
import com.aiaccounting.data.local.CategoryEntity
import com.aiaccounting.data.local.TransactionEntity
import com.aiaccounting.domain.BackupCsv
import com.aiaccounting.domain.DedupKey
import com.aiaccounting.domain.Tx
import com.aiaccounting.domain.TxSource
import com.aiaccounting.domain.TxType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import androidx.room.withTransaction

class AccountingRepository(
    private val db: AppDatabase,
    private val now: () -> Long = System::currentTimeMillis,
) {

    private val transactionDao = db.transactionDao()
    private val categoryDao = db.categoryDao()
    private val accountDao = db.accountDao()

    /** 首次启动写入种子数据；已写过则什么都不做。 */
    suspend fun ensureSeeded() {
        if (categoryDao.count() == 0) categoryDao.insertAll(SeedData.allCategories)
        if (accountDao.count() == 0) accountDao.insertAll(SeedData.accounts)
    }

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

    /** 编辑已有记录：只改用户可改的字段，来源与去重指纹保持原样。 */
    suspend fun updateTransaction(
        id: Long,
        type: TxType,
        amountCents: Long,
        categoryId: Long,
        accountId: Long,
        occurredAt: Long,
        merchant: String? = null,
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
                merchant = merchant,
                note = note,
                updatedAt = now(),
            ),
        )
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
