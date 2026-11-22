package com.aiaccounting.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val emoji: String,
    val sortOrder: Int = 0,
    val archived: Boolean = false,
)

@Entity(tableName = "categories", indices = [Index("kind")])
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val emoji: String,
    val kind: String,
    val sortOrder: Int = 0,
)

/**
 * 一条流水。
 *
 * 与 MVP 文档对齐的字段：
 * - [source] 手动 / 通知 / 导入，三路来源共用一张表；
 * - [dedupKey] 去重指纹，通知与导入必须写入，手动可空；
 * - [status] PENDING 表示还待在收件箱里确认，手动记账直接 CONFIRMED。
 */
@Entity(
    tableName = "transactions",
    indices = [Index("occurredAt"), Index("categoryId"), Index("dedupKey")],
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val amountCents: Long,
    val occurredAt: Long,
    val categoryId: Long,
    val accountId: Long,
    val merchant: String? = null,
    val note: String? = null,
    val source: String = "MANUAL",
    val dedupKey: String? = null,
    val status: String = "CONFIRMED",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
