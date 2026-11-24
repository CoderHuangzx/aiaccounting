package com.aiaccounting.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    /** 正式账本只包含已确认的记录；待确认的走 [observePending]。 */
    @Query("SELECT * FROM transactions WHERE status = 'CONFIRMED' ORDER BY occurredAt DESC, id DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY occurredAt DESC, id DESC")
    suspend fun all(): List<TransactionEntity>

    @Insert
    suspend fun insert(entity: TransactionEntity): Long

    @Update
    suspend fun update(entity: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun findById(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE dedupKey = :key LIMIT 1")
    suspend fun findByDedupKey(key: String): TransactionEntity?

    /** 账单导入时用「金额 + 时间窗」兜底判重，能抓到手动记的、通知记的同金额记录。 */
    @Query("SELECT * FROM transactions WHERE amountCents = :amount AND occurredAt BETWEEN :from AND :to LIMIT 1")
    suspend fun findSimilar(amount: Long, from: Long, to: Long): TransactionEntity?

    /**
     * 跨来源判重：同金额、同时间窗、**自动捕捉来的**记录。
     *
     * 同一笔美团订单可能被两条通道各抓一次——通知在扣款时来（商户名是「美团」），
     * 读屏在用户后来打开订单页时触发（商户名是店铺名或空）。指纹里的商户不同、
     * 时间也可能差几十分钟，所以单独用「金额 + 时间窗」兜这道底。
     */
    @Query(
        "SELECT * FROM transactions WHERE amountCents = :amount " +
            "AND occurredAt BETWEEN :from AND :to AND source = 'NOTIFICATION' LIMIT 1",
    )
    suspend fun findAutoDuplicate(amount: Long, from: Long, to: Long): TransactionEntity?

    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun count(): Int

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()

    @Query("SELECT * FROM transactions WHERE status = 'PENDING' ORDER BY occurredAt DESC, id DESC")
    fun observePending(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE status = 'PENDING' ORDER BY occurredAt DESC, id DESC")
    suspend fun pending(): List<TransactionEntity>

    @Query("UPDATE transactions SET status = :status, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, updatedAt: Long)
}

@Dao
interface RawCaptureDao {
    @Insert
    suspend fun insert(entity: RawCaptureEntity): Long

    @Query("SELECT * FROM raw_captures ORDER BY capturedAt DESC LIMIT :limit")
    suspend fun latest(limit: Int): List<RawCaptureEntity>

    @Query("SELECT * FROM raw_captures ORDER BY capturedAt DESC LIMIT :limit")
    fun observeLatest(limit: Int): Flow<List<RawCaptureEntity>>

    @Query("DELETE FROM raw_captures")
    suspend fun clear()
}

@Dao
interface MerchantRuleDao {

    @Query("SELECT * FROM merchant_rules")
    suspend fun all(): List<MerchantRuleEntity>

    @Query("SELECT * FROM merchant_rules ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<MerchantRuleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: MerchantRuleEntity)

    @Query("DELETE FROM merchant_rules")
    suspend fun clear()

    @Query("SELECT COUNT(*) FROM merchant_rules")
    suspend fun count(): Int
}

@Dao
interface CategoryDao {

    @Query("SELECT * FROM categories ORDER BY sortOrder, id")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE kind = :kind ORDER BY sortOrder, id")
    fun observeByKind(kind: String): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE kind = :kind ORDER BY sortOrder, id")
    suspend fun byKind(kind: String): List<CategoryEntity>

    @Query("SELECT * FROM categories ORDER BY sortOrder, id")
    suspend fun all(): List<CategoryEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<CategoryEntity>)

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun count(): Int
}

@Dao
interface AccountDao {

    @Query("SELECT * FROM accounts ORDER BY sortOrder, id")
    fun observeAll(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts ORDER BY sortOrder, id")
    suspend fun all(): List<AccountEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<AccountEntity>)

    @Query("SELECT COUNT(*) FROM accounts")
    suspend fun count(): Int
}
