package com.aiaccounting.domain

/**
 * 从订单页面上的文字里认出「下了单 / 退了款」。
 *
 * 淘宝、京东、抖音这类 App 内下单，付款后往往要等确认收货（或 7 天）才真正扣款，
 * 通知也是那时才来。想「下单就记」只能读屏——所以这里的输入是**页面上当前可见的文案**。
 *
 * 两条硬规则（都是踩过坑之后加的）：
 * 1. 金额必须**跟着标签**出现（实付款 / 确认收货后付 / 退款金额 …）。
 *    页面上随便一个「¥xx」可能是商品原价、赠品价，绝不能当订单金额。
 * 2. 必须同时命中状态词和金额，缺一个就放弃；认出来的只进待确认收件箱。
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
        /** 订单编号（页面上有就带上，用来防重复记账）。 */
        val orderNo: String?,
        /** 命中的关键句，写进备注方便回溯。 */
        val evidence: String,
    )

    /** 下单/付款成功的状态词。 */
    private val ORDER_WORDS = listOf(
        "订单提交成功", "下单成功", "支付成功", "付款成功", "已付款", "等待发货", "待发货",
        "打包中", "待收货", "确认收货后付", "订单已完成", "订单完成",
    )

    /** 退款的状态词。 */
    private val REFUND_WORDS = listOf(
        "退款成功", "退款已原路退回", "退款完成", "已退款",
    )

    /** 这些页面上的钱不能算「下单」：失败、取消、关闭。 */
    private val REJECT_WORDS = listOf(
        "支付失败", "付款失败", "订单已取消", "交易关闭", "已关闭", "已失效",
    )

    /**
     * 金额必须带这些标签之一，按优先级从前往后找。
     * 「确认收货后付」是抖音/淘宝先用后付的实付金额标签。
     */
    private val AMOUNT_LABELS = listOf(
        "实付款", "实付金额", "实付", "确认收货后付", "应付款", "订单金额", "付款金额", "合计", "总计",
    )

    private val REFUND_LABELS = listOf("退款金额", "退款总额", "实际退款")

    private const val MAX_REASONABLE_CENTS = 100_000_000L // 100 万元，超了视为误抓

    /** 订单编号：一般 15~25 位纯数字，可能带「订单编号」前缀，也可能被空格分组（美团）。 */
    private val ORDER_NO_PATTERN = Regex("""(?:订单编号|订单号码|订单号|交易单号)[：:\s]*([0-9][0-9\s]{9,30})""")

    /**
     * @param texts 页面上抓到的所有文本片段（NodeInfo 的 text / contentDescription）
     */
    fun parse(texts: List<String>): Event? {
        val cleaned = texts.map { it.trim() }.filter { it.isNotEmpty() }
        if (cleaned.isEmpty()) return null
        val joined = cleaned.joinToString(" ")

        if (REJECT_WORDS.any { joined.contains(it) }) return null

        val refundHit = REFUND_WORDS.firstOrNull { joined.contains(it) }
        if (refundHit != null) {
            val amount = amountAfterLabels(cleaned, joined, REFUND_LABELS) ?: return null
            return Event(
                kind = Kind.REFUND,
                type = TxType.INCOME,
                amountCents = amount,
                merchant = extractMerchant(cleaned),
                orderNo = extractOrderNo(joined),
                evidence = refundHit,
            )
        }

        val orderHit = ORDER_WORDS.firstOrNull { joined.contains(it) } ?: return null
        val amount = amountAfterLabels(cleaned, joined, AMOUNT_LABELS) ?: return null
        return Event(
            kind = Kind.ORDER,
            type = TxType.EXPENSE,
            amountCents = amount,
            merchant = extractMerchant(cleaned),
            orderNo = extractOrderNo(joined),
            evidence = orderHit,
        )
    }

    /**
     * 只在「标签 + 金额」相邻时取值：
     * - 同一段文字里既有标签又有数字，直接取；
     * - 标签独占一段时，看它后面 1~2 段里有没有金额。
     */
    private fun amountAfterLabels(texts: List<String>, joined: String, labels: List<String>): Long? {
        for (label in labels) {
            // 情况一：标签后面紧跟金额（同一段或整页拼接后紧邻）
            val inline = Regex("""${Regex.escape(label)}[^0-9¥￥]{0,4}[¥￥]?\s*([0-9][0-9,]*(?:\.[0-9]{1,2})?)""")
                .find(joined)
            inline?.let { match ->
                parseCents(match.groupValues[1])?.let { return it }
            }

            // 情况二：标签是独立一段，金额在后面几段里
            val index = texts.indexOfFirst { it == label || it.startsWith(label) }
            if (index >= 0) {
                for (offset in 1..3) {
                    val next = texts.getOrNull(index + offset) ?: break
                    val amount = Regex("""[¥￥]?\s*([0-9][0-9,]*(?:\.[0-9]{1,2})?)\s*元?""")
                        .find(next)?.groupValues?.get(1)
                    parseCents(amount)?.let { return it }
                }
            }
        }
        return null
    }

    private fun parseCents(raw: String?): Long? {
        if (raw.isNullOrBlank()) return null
        val value = raw.replace(",", "").toDoubleOrNull() ?: return null
        if (value <= 0.0) return null
        val cents = Math.round(value * 100)
        if (cents > MAX_REASONABLE_CENTS) return null
        return cents
    }

    private fun extractOrderNo(joined: String): String? =
        ORDER_NO_PATTERN.find(joined)
            ?.groupValues?.get(1)
            ?.filter { !it.isWhitespace() }
            ?.takeIf { it.length >= 10 }

    /** 店铺名一般出现在「XX旗舰店 / XX专营店」这类片段里。 */
    private fun extractMerchant(texts: List<String>): String? {
        val shopPattern = Regex("""^(.{2,24}?(?:旗舰店|专营店|专卖店|官方店|小店|超市|便利店))$""")
        return texts.firstOrNull { shopPattern.matches(it) }
            ?.let { shopPattern.find(it)?.groupValues?.get(1) }
    }
}
