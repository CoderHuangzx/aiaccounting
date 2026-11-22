package com.aiaccounting.domain

/**
 * 去重指纹。
 *
 * 同一笔消费可能同时来自通知、账单导入、手动补记，必须先有稳定的指纹才能收敛。
 * 优先使用渠道单号（支付宝订单号 / 微信交易单号）后若干位；拿不到单号时退化为
 * 「金额 + 10 分钟时间窗 + 商户前缀」。
 */
object DedupKey {

    private const val WINDOW_MILLIS = 10 * 60 * 1000L

    fun fromChannel(tradeNo: String): String? {
        val trimmed = tradeNo.trim()
        if (trimmed.length < 4) return null
        return "ch:" + trimmed.takeLast(8)
    }

    fun fallback(amountCents: Long, occurredAt: Long, merchant: String?): String {
        val window = occurredAt / WINDOW_MILLIS
        val prefix = merchant?.trim()?.take(6)?.lowercase().orEmpty()
        return "auto:$amountCents:$window:$prefix"
    }
}
