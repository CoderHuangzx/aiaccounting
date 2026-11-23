package com.aiaccounting.domain

/**
 * 从订单页面上的文字里认出「下了单 / 退了款」。
 *
 * 淘宝、京东、抖音这类 App 内下单，付款后往往要等确认收货（或 7 天）才真正扣款，
 * 通知也是那时才来。想「下单就记」只能读屏——所以这里的输入是**页面上当前可见的文案**。
 *
 * 保守原则不变：必须同时命中「状态词」和「金额」，缺一个就放弃，
 * 认出来的也只进待确认收件箱，人工过一眼。
 */
object OrderScreenParser {

    enum class Kind(val label: String) {
        ORDER("下单"),
        REFUND("退款"),
    }

    data class Event(
        val kind: Kind,
        /** 记账时的收支方向：下单=支出，退款=收入。 */
        val type: TxType,
        val amountCents: Long,
        val merchant: String?,
        /** 命中的关键句，写进备注方便回溯。 */
        val evidence: String,
    )

    /** 下单/付款成功的状态词。 */
    private val ORDER_WORDS = listOf(
        "订单提交成功", "下单成功", "支付成功", "付款成功", "已付款", "等待发货", "待发货", "实付款",
    )

    /** 退款的状态词。 */
    private val REFUND_WORDS = listOf(
        "退款成功", "退款已原路退回", "退款完成", "已退款", "退款金额",
    )

    /** 这些页面上的钱不能算「下单」：失败、取消、关闭。 */
    private val REJECT_WORDS = listOf(
        "支付失败", "付款失败", "订单已取消", "交易关闭", "已关闭", "已失效",
    )

    private val AMOUNT_PATTERNS = listOf(
        Regex("""(?:实付款|实付|合计|退款金额|退款)[^0-9¥￥]{0,6}[¥￥]?\s*([0-9]+(?:,[0-9]{3})*(?:\.[0-9]{1,2})?)"""),
        Regex("""[¥￥]\s*([0-9]+(?:,[0-9]{3})*(?:\.[0-9]{1,2})?)"""),
        Regex("""([0-9]+(?:,[0-9]{3})*(?:\.[0-9]{1,2})?)\s*元"""),
    )

    /**
     * @param texts 页面上抓到的所有文本片段（NodeInfo 的 text / contentDescription）
     */
    fun parse(texts: List<String>): Event? {
        val joined = texts.filter { it.isNotBlank() }.joinToString(" ")
        if (joined.isBlank()) return null

        // 先排除还没付钱 / 已关闭的页面
        if (REJECT_WORDS.any { joined.contains(it) }) return null

        val refundHit = REFUND_WORDS.firstOrNull { joined.contains(it) }
        if (refundHit != null) {
            val amount = extractAmount(texts, joined) ?: return null
            return Event(
                kind = Kind.REFUND,
                type = TxType.INCOME,
                amountCents = amount,
                merchant = null,
                evidence = refundHit,
            )
        }

        val orderHit = ORDER_WORDS.firstOrNull { joined.contains(it) } ?: return null
        val amount = extractAmount(texts, joined) ?: return null
        return Event(
            kind = Kind.ORDER,
            type = TxType.EXPENSE,
            amountCents = amount,
            merchant = extractMerchant(texts),
            evidence = orderHit,
        )
    }

    private fun extractAmount(texts: List<String>, joined: String): Long? {
        // 优先在「实付款 / 合计 / 退款金额」附近找，命中不了再看整页的第一个金额
        for (pattern in AMOUNT_PATTERNS) {
            val match = pattern.find(joined) ?: continue
            val raw = match.groupValues[1].replace(",", "")
            val value = raw.toDoubleOrNull() ?: continue
            if (value <= 0.0) continue
            return Math.round(value * 100)
        }
        return null
    }

    /** 店铺名一般出现在「XX旗舰店 / XX专营店」这类片段里。 */
    private fun extractMerchant(texts: List<String>): String? {
        val shopPattern = Regex("""^(.{2,24}?(?:旗舰店|专营店|专卖店|官方店|小店|超市|便利店))$""")
        return texts.asSequence()
            .map { it.trim() }
            .firstOrNull { shopPattern.matches(it) }
            ?.let { shopPattern.find(it)?.groupValues?.get(1) }
    }
}
