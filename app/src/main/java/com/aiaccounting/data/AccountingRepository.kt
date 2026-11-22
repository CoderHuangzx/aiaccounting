package com.aiaccounting.data

import com.aiaccounting.data.local.AccountEntity
import com.aiaccounting.data.local.AppDatabase
import com.aiaccounting.data.local.CategoryEntity
import com.aiaccounting.data.local.TransactionEntity
import com.aiaccounting.domain.DedupKey
import com.aiaccounting.domain.Tx
import com.aiaccounting.domain.TxSource
import com.aiaccounting.domain.TxType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

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
