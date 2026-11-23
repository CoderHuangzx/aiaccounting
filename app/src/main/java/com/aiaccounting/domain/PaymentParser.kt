package com.aiaccounting.domain

/**
 * 从微信 / 支付宝的支付通知里解析出一笔账。
 *
 * 关键约束：通知文案不是稳定接口，各版本会变；而且微信的普通聊天消息也可能带「支付」二字。
 * 所以这里宁可漏判（返回 null 让人工补），也不要误判——一条错账比十条漏账更伤信任。
 */
object PaymentParser {

    enum class Channel { WECHAT, ALIPAY, UNKNOWN }

    data class Parsed(
        val amountCents: Long,
        val type: TxType,
        val merchant: String?,
        val channel: Channel,
    )

    private const val PKG_WECHAT = "com.tencent.mm"
    private const val PKG_ALIPAY = "com.eg.android.AlipayGphone"

    /** 明确表示「钱进来了」的词。 */
    private val INCOME_WORDS = listOf("收款", "到账", "入账", "收到", "收入", "退款", "退回")

    /** 明确表示「钱出去了」的词。 */
    private val EXPENSE_WORDS = listOf("支付", "付款", "消费", "支出", "扣款", "扣费", "转出")

    /** 这些场景金额含义不明确，直接放过，交给人工。 */
    private val IGNORE_WORDS = listOf("红包", "转账", "提现", "零钱通", "理财", "还信用卡", "账单已出")

    /** 金额：带 ¥/￥ 前缀，或带「元」后缀；支持千分位。 */
    private val AMOUNT_PATTERNS = listOf(
        Regex("""[¥￥]\s*([0-9]+(?:,[0-9]{3})*(?:\.[0-9]{1,2})?)"""),
        Regex("""([0-9]+(?:,[0-9]{3})*(?:\.[0-9]{1,2})?)\s*元"""),
    )

    private val MERCHANT_PATTERNS = listOf(
        Regex("""在[「『"]([^」』"]{1,20})[」』"]"""),
        Regex("""[「『]([^」』]{1,20})[」』]"""),
        Regex("""在\s*([^\s,，。]{2,20}?)\s*(?:消费|支付|付款)"""),
        Regex("""向\s*([^\s,，。]{2,20}?)\s*(?:付款|转账|支付)"""),
    )

    fun channelOf(packageName: String?): Channel = when (packageName) {
        PKG_WECHAT -> Channel.WECHAT
        PKG_ALIPAY -> Channel.ALIPAY
        else -> Channel.UNKNOWN
    }

    fun parse(packageName: String?, title: String?, text: String?): Parsed? {
        val channel = channelOf(packageName)
        if (channel == Channel.UNKNOWN) return null

        val haystack = listOfNotNull(title, text).joinToString(" ").trim()
        if (haystack.isEmpty()) return null

        // 必须是「支付类」通知，否则微信的普通消息会被误抓
        val looksLikePayment = when (channel) {
            Channel.WECHAT -> haystack.contains("微信支付") || haystack.contains("支付凭证")
            Channel.ALIPAY -> haystack.contains("支付宝") || haystack.contains("支付助手") || haystack.contains("账单")
            Channel.UNKNOWN -> false
        }
        if (!looksLikePayment) return null

        if (IGNORE_WORDS.any { haystack.contains(it) }) return null

        val type = when {
            INCOME_WORDS.any { haystack.contains(it) } -> TxType.INCOME
            EXPENSE_WORDS.any { haystack.contains(it) } -> TxType.EXPENSE
            else -> return null
        }

        val amountCents = extractAmount(haystack) ?: return null
        if (amountCents <= 0) return null

        return Parsed(
            amountCents = amountCents,
            type = type,
            merchant = extractMerchant(haystack),
            channel = channel,
        )
    }

    private fun extractAmount(text: String): Long? {
        for (pattern in AMOUNT_PATTERNS) {
            val match = pattern.find(text) ?: continue
            val raw = match.groupValues[1].replace(",", "")
            val value = raw.toDoubleOrNull() ?: continue
            if (value <= 0.0) continue
            return Math.round(value * 100)
        }
        return null
    }

    private fun extractMerchant(text: String): String? {
        for (pattern in MERCHANT_PATTERNS) {
            val match = pattern.find(text) ?: continue
            val name = match.groupValues[1].trim()
            if (name.isNotEmpty()) return name
        }
        return null
    }
}
