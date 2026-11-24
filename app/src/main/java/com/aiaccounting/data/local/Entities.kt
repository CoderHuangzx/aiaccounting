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
    /** 内置分类：允许改名换图标，但不允许删除；用户自己加的为 false。 */
    val isBuiltIn: Boolean = true,
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

/**
 * 通知原文留档。
 *
 * 支付 App 的通知文案会随版本变化，规则调歪了必须有原始文本才能回溯，
 * 否则「为什么这笔没记上」永远查不清。
 */
@Entity(tableName = "raw_captures", indices = [Index("capturedAt")])
data class RawCaptureEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val title: String?,
    val text: String?,
    val capturedAt: Long,
    val parsed: Boolean,
    val reason: String? = null,
)

/**
 * 商户 → 分类的学习结果。
 *
 * 用户改过一次某商户的分类，就往这里记一条；下次该商户再进来直接落到对应分类，
 * 不用再手工挑。pattern 存归一化后的商户名，天然去重。
 */
@Entity(tableName = "merchant_rules", indices = [Index(value = ["pattern"], unique = true)])
data class MerchantRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pattern: String,
    val categoryId: Long,
    val hitCount: Int = 1,
    val updatedAt: Long = System.currentTimeMillis(),
)


