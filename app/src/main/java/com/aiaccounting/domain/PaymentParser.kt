package com.aiaccounting.domain

/**
 * 从支付/购物 App 的通知里解析出一笔账。
 *
 * 关键约束：通知文案不是稳定接口，各版本会变；微信的普通聊天消息也可能带「支付」二字。
 * 所以这里宁可漏判（返回 null 让人工补），也不要误判——一条错账比十条漏账更伤信任。
 *
 * 分两类渠道：
 * - 支付渠道（微信/支付宝）：文案里有明确的「微信支付」「支付宝」来源标识才认；
 * - 电商/外卖渠道（淘宝/京东/美团/抖音）：认「支付成功 / 已付款」这类成功词，
 *   并且排除「待支付 / 支付失败 / 已取消 / 退款」，解析出来的一律进待确认收件箱。
 */
object PaymentParser {

    enum class Channel(val label: String, val accountName: String?) {
        WECHAT("微信", "微信"),
        ALIPAY("支付宝", "支付宝"),
        TAOBAO("淘宝", "支付宝"),
        JD("京东", "微信"),
        MEITUAN("美团", "微信"),
        DOUYIN("抖音", "微信"),
        UNKNOWN("未知", null),
    }

    data class Parsed(
        val amountCents: Long,
        val type: TxType,
        val merchant: String?,
        val channel: Channel,
        /** 付款账户名，按渠道默认值 + 文案里的支付方式推断。 */
        val accountName: String?,
    )

    /** 这些包名会进入监听范围；不在名单里的一概不看。 */
    val WATCHED_PACKAGES = setOf(
        "com.tencent.mm",                     // 微信
        "com.eg.android.AlipayGphone",        // 支付宝
        "com.taobao.taobao",                  // 淘宝
        "com.jingdong.app.mall",              // 京东
        "com.sankuai.meituan",                // 美团
        "com.sankuai.meituan.takeoutnew",     // 美团外卖
        "com.ss.android.ugc.aweme",           // 抖音
    )

    /** 明确表示「钱进来了」的词。 */
    private val INCOME_WORDS = listOf("收款", "到账", "入账", "收到", "收入", "退款", "退回")

    /** 明确表示「钱出去了」的词。 */
    private val EXPENSE_WORDS = listOf("支付", "付款", "消费", "支出", "扣款", "扣费", "转出")

    /** 这些场景金额含义不明确，直接放过，交给人工。 */
    private val IGNORE_WORDS = listOf("红包", "转账", "提现", "零钱通", "理财", "还信用卡", "账单已出")

    /** 电商渠道必须命中其中一个，才算「已经付过钱」。 */
    private val PAID_WORDS = listOf("支付成功", "付款成功", "已付款", "已支付", "支付完成", "交易成功")

    /** 电商渠道的反向词：没付钱或钱又退回来了，都不记。 */
    private val NOT_PAID_WORDS = listOf(
        "待支付", "未支付", "支付失败", "付款失败", "已取消", "取消订单", "退款", "退货", "已关闭",
    )

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
        Regex("""(?:商户|商家|店铺)[：:]\s*([^\s,，。]{1,20})"""),
    )

    fun channelOf(packageName: String?): Channel {
        val pkg = packageName ?: return Channel.UNKNOWN
        return when {
            pkg == "com.tencent.mm" -> Channel.WECHAT
            pkg == "com.eg.android.AlipayGphone" -> Channel.ALIPAY
            pkg.startsWith("com.taobao") -> Channel.TAOBAO
            pkg.startsWith("com.jingdong") -> Channel.JD
            pkg.startsWith("com.sankuai.meituan") -> Channel.MEITUAN
            pkg.startsWith("com.ss.android.ugc.aweme") -> Channel.DOUYIN
            else -> Channel.UNKNOWN
        }
    }

    fun isWatched(packageName: String?): Boolean =
        packageName != null && channelOf(packageName) != Channel.UNKNOWN

    fun parse(packageName: String?, title: String?, text: String?): Parsed? {
        val channel = channelOf(packageName)
        if (channel == Channel.UNKNOWN) return null

        val haystack = listOfNotNull(title, text).joinToString(" ").trim()
        if (haystack.isEmpty()) return null

        // 必须是「钱真的付了」的通知，否则普通消息、催付广告都会被误抓
        val looksLikePayment = when (channel) {
            Channel.WECHAT -> haystack.contains("微信支付") || haystack.contains("支付凭证")
            Channel.ALIPAY -> haystack.contains("支付宝") || haystack.contains("支付助手") || haystack.contains("账单")
            else -> PAID_WORDS.any { haystack.contains(it) } &&
                NOT_PAID_WORDS.none { haystack.contains(it) }
        }
        if (!looksLikePayment) return null

        if (IGNORE_WORDS.any { haystack.contains(it) }) return null
        if (NOT_PAID_WORDS.any { haystack.contains(it) }) return null

        val type = when {
            INCOME_WORDS.any { haystack.contains(it) } -> TxType.INCOME
            EXPENSE_WORDS.any { haystack.contains(it) } -> TxType.EXPENSE
            channel != Channel.WECHAT && channel != Channel.ALIPAY -> TxType.EXPENSE
            else -> return null
        }

        val amountCents = extractAmount(haystack) ?: return null
        if (amountCents <= 0) return null

        return Parsed(
            amountCents = amountCents,
            type = type,
            merchant = extractMerchant(haystack) ?: channel.label,
            channel = channel,
            accountName = resolveAccount(haystack, channel),
        )
    }

    /** 文案里写了支付方式就以文案为准，否则用渠道默认值。 */
    private fun resolveAccount(text: String, channel: Channel): String? = when {
        text.contains("花呗") || text.contains("余额宝") || text.contains("支付宝") -> "支付宝"
        text.contains("零钱") || text.contains("微信支付") || text.contains("微信支付分") -> "微信"
        text.contains("银行卡") || text.contains("储蓄卡") || text.contains("信用卡") -> "银行卡"
        else -> channel.accountName
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
