package com.aiaccounting.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * 用真实通知的常见文案做样本。文案会变，所以这里既覆盖「能认出来」的，
 * 也覆盖「必须放过」的（聊天消息、红包、转账等）。
 */
class PaymentParserTest {

    private fun parseWeChat(title: String, text: String) =
        PaymentParser.parse("com.tencent.mm", title, text)

    private fun parseAlipay(title: String, text: String) =
        PaymentParser.parse("com.eg.android.AlipayGphone", title, text)

    // ── 微信 ────────────────────────────────────────────────────────────

    @Test
    fun `微信支付凭证能认出金额`() {
        val parsed = parseWeChat("微信支付", "已支付¥52.50")
        assertEquals(5250L, parsed?.amountCents)
        assertEquals(TxType.EXPENSE, parsed?.type)
        assertEquals(PaymentParser.Channel.WECHAT, parsed?.channel)
    }

    @Test
    fun `微信中文金额写法`() {
        val parsed = parseWeChat("微信支付", "你已成功支付128.00元")
        assertEquals(12800L, parsed?.amountCents)
        assertEquals(TxType.EXPENSE, parsed?.type)
    }

    @Test
    fun `微信收款算收入`() {
        val parsed = parseWeChat("微信支付", "收款到账0.01元")
        assertEquals(1L, parsed?.amountCents)
        assertEquals(TxType.INCOME, parsed?.type)
    }

    @Test
    fun `微信普通聊天消息不能被当成账`() {
        assertNull(parseWeChat("小王", "晚上吃饭吗？我请你"))
        assertNull(parseWeChat("工作群", "这个月工资发了吗"))
    }

    @Test
    fun `微信红包与转账放过`() {
        assertNull(parseWeChat("微信支付", "你收到一个红包¥8.88"))
        assertNull(parseWeChat("微信支付", "你已向张三转账¥100.00"))
    }

    // ── 支付宝 ──────────────────────────────────────────────────────────

    @Test
    fun `支付宝支出通知`() {
        val parsed = parseAlipay("支付宝", "你有一笔 52.50 元的支出")
        assertEquals(5250L, parsed?.amountCents)
        assertEquals(TxType.EXPENSE, parsed?.type)
        assertEquals(PaymentParser.Channel.ALIPAY, parsed?.channel)
    }

    @Test
    fun `支付宝带商户名的写法`() {
        val parsed = parseAlipay("支付宝", "你在「瑞幸咖啡」消费35.00元")
        assertEquals(3500L, parsed?.amountCents)
        assertEquals("瑞幸咖啡", parsed?.merchant)
    }

    @Test
    fun `支付宝收款算收入`() {
        val parsed = parseAlipay("支付宝", "你收到一笔转账，收款1,234.50元")
        // 转账属于放过词，这里应为 null
        assertNull(parsed)

        val income = parseAlipay("支付宝", "收款到账 12.00 元")
        assertEquals(1200L, income?.amountCents)
        assertEquals(TxType.INCOME, income?.type)
    }

    @Test
    fun `千分位金额能正确换算`() {
        val parsed = parseAlipay("支付宝", "你有一笔 1,234.50 元的支出")
        assertEquals(123450L, parsed?.amountCents)
    }

    // ── 其他 ────────────────────────────────────────────────────────────

    @Test
    fun `其它应用的通知一律忽略`() {
        assertNull(PaymentParser.parse("com.example.shop", "支付成功", "已支付¥10.00"))
        assertNull(PaymentParser.parse(null, "支付", "¥10"))
    }

    @Test
    fun `没有金额或没有收支方向时返回 null`() {
        assertNull(parseWeChat("微信支付", "你有新的支付消息"))
        assertNull(parseWeChat("微信支付", "已支付"))
    }

    @Test
    fun `空标题空正文返回 null`() {
        assertNull(PaymentParser.parse("com.tencent.mm", null, null))
        assertNull(PaymentParser.parse("com.tencent.mm", "", ""))
    }
}
