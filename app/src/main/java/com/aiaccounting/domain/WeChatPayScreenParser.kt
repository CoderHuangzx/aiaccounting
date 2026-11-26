package com.aiaccounting.domain

/**
 * 微信「支付成功」页面的读屏解析。
 *
 * 为什么需要它：扫个人收款码付款时，微信**不发系统通知**（通知只覆盖商户收款），
 * 所以通知捕捉这条路对这类支付是断的，只能靠读屏。
 *
 * 页面长这样（2026-11-26 真机）：
 * ```
 * 支付成功
 * 只是不想失去您（*萍）
 * ¥ 3.50
 * 使用零钱支付
 * 交易状态  支付成功，对方已收款
 * ```
 * 交易详情页则带「转账单号」「支付方式 零钱」。
 */
object WeChatPayScreenParser {

    data class Result(
        val amountCents: Long,
        val type: TxType,
        val merchant: String?,
        val evidence: String,
    )

    /** 付出去的页面。 */
    private val EXPENSE_WORDS = listOf("支付成功", "支付完成", "付款成功")

    /**
     * 收到的页面：别人转给你 / 收款到账。
     * 真机文案：「你已收款，资金已存入零钱 ¥1800.00」。
     */
    private val INCOME_WORDS = listOf("你已收款", "收款到账", "收款成功", "已收款", "资金已存入零钱")

    /** 页面特征：防止在聊天列表里误触。 */
    private val PAGE_WORDS = listOf(
        "使用零钱支付", "对方已收款", "转账单号", "支付方式", "扫二维码付款", "转账时间",
        "收款时间", "零钱余额", "账单详情", "完成",
    )

    private val AMOUNT = Regex("""[¥￥]\s*(\d{1,7}(?:\.\d{1,2})?)""")
    private val SIGNED_AMOUNT = Regex("""-\s*(\d{1,7}\.\d{2})""")
    private val PAYEE = Regex("""付款[-－—]给\s*([^\s，。]{1,20})""")

    /** 金额后面常跟「使用零钱支付」这类说明，先掐掉。 */
    private val TRAILING = Regex("""(使用|零钱|微信|余额).*""")

    fun parse(texts: List<String>): Result? {
        val all = texts.joinToString(" ")
        // 广告一律放过
        if (listOf("立减", "优惠券", "红包已过期", "推荐").any { all.contains(it) }) return null

        val expense = EXPENSE_WORDS.any { all.contains(it) }
        val income = INCOME_WORDS.any { all.contains(it) }
        // 「支付成功，对方已收款」同时命中两边，付款优先
        val type = when {
            expense -> TxType.EXPENSE
            income -> TxType.INCOME
            else -> return null
        }
        if (PAGE_WORDS.none { all.contains(it) }) return null

        val amountText = AMOUNT.find(all)?.groupValues?.get(1)
            ?: SIGNED_AMOUNT.find(all)?.groupValues?.get(1)
            ?: return null
        val cents = parseCents(amountText) ?: return null
        if (cents <= 0 || cents > 100_000_000L) return null

        return Result(
            amountCents = cents,
            type = type,
            merchant = if (type == TxType.EXPENSE) extractPayee(texts, all) else null,
            evidence = if (type == TxType.EXPENSE) {
                "微信支付成功 ¥" + formatCents(cents)
            } else {
                "微信收款 ¥" + formatCents(cents)
            },
        )
    }

    /**
     * 收款方：优先「付款给XXX」，否则取金额上面那行文本（支付成功页的昵称行）。
     */
    private fun extractPayee(texts: List<String>, all: String): String? {
        PAYEE.find(all)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }

        val amountIndex = texts.indexOfFirst { AMOUNT.containsMatchIn(it) }
        if (amountIndex > 0) {
            val candidate = texts[amountIndex - 1].trim()
            if (isPlausibleName(candidate)) return candidate.take(20)
        }
        return null
    }

    private fun isPlausibleName(text: String): Boolean {
        if (text.length !in 1..20) return false
        if (text.any { it.isDigit() }) return false
        if (text.contains("¥") || text.contains("￥")) return false
        val noise = listOf("支付", "交易", "订单", "查看", "留言", "完成", "零钱", "状态", "方式")
        return noise.none { text.contains(it) }
    }

    private fun parseCents(raw: String): Long? {
        val cleaned = TRAILING.replace(raw, "").trim()
        val value = cleaned.toDoubleOrNull() ?: return null
        return Math.round(value * 100)
    }

    private fun formatCents(cents: Long): String {
        val yuan = cents / 100
        val fen = cents % 100
        return "$yuan.${fen.toString().padStart(2, '0')}"
    }
}
