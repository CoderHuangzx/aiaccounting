package com.aiaccounting.domain

/**
 * 账单文件里的一笔交易。支付宝 CSV 和微信 XLSX 解析后都归到这个结构，
 * 后面的去重、预览、落库逻辑就能共用。
 */
data class BillRow(
    val occurredAt: Long,
    val amountCents: Long,
    val type: TxType,
    /** 交易对方，通常是商户名。 */
    val counterparty: String?,
    /** 商品名称 / 交易说明。 */
    val description: String?,
    /** 交易单号，用来做去重指纹。 */
    val tradeNo: String?,
)
