package com.aiaccounting.domain

/** 记账方向。转账在 MVP 阶段不做，后续以 TRANSFER 扩展。 */
enum class TxType { EXPENSE, INCOME }

/** 记录来源：手动 / 通知捕获 / 账单导入。三路来源靠 dedupKey 收敛。 */
enum class TxSource { MANUAL, NOTIFICATION, IMPORT }

/**
 * 界面与统计层使用的记账条目。
 * 已经拼好分类名称、图标与账户名称，避免在 UI 层再查表。
 */
data class Tx(
    val id: Long,
    val type: TxType,
    val amountCents: Long,
    val occurredAt: Long,
    val categoryId: Long,
    val categoryName: String,
    val categoryEmoji: String,
    val accountId: Long,
    val accountName: String,
    val merchant: String? = null,
    val note: String? = null,
    val source: TxSource = TxSource.MANUAL,
)
