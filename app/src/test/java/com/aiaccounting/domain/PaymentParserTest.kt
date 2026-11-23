package com.aiaccounting.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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

    // ── 电商 / 外卖渠道 ─────────────────────────────────────────────────

    private fun parseMeituan(title: String, text: String) =
        PaymentParser.parse("com.sankuai.meituan.takeoutnew", title, text)

    private fun parseJd(title: String, text: String) =
        PaymentParser.parse("com.jingdong.app.mall", title, text)

    private fun parseTaobao(title: String, text: String) =
        PaymentParser.parse("com.taobao.taobao", title, text)

    private fun parseDouyin(title: String, text: String) =
        PaymentParser.parse("com.ss.android.ugc.aweme", title, text)

    @Test
    fun `美团支付成功能认出金额`() {
        val parsed = parseMeituan("美团外卖", "支付成功，本单实付¥22.90")
        assertEquals(2290L, parsed?.amountCents)
        assertEquals(TxType.EXPENSE, parsed?.type)
        assertEquals(PaymentParser.Channel.MEITUAN, parsed?.channel)
        assertEquals("美团", parsed?.merchant)
    }

    @Test
    fun `京东已付款能认出来`() {
        val parsed = parseJd("京东", "您的订单已付款，实付 ¥99.00")
        assertEquals(9900L, parsed?.amountCents)
        assertEquals(PaymentParser.Channel.JD, parsed?.channel)
    }

    @Test
    fun `淘宝支付成功默认走支付宝账户`() {
        val parsed = parseTaobao("淘宝", "支付成功 实付款 ¥59.90")
        assertEquals(5990L, parsed?.amountCents)
        assertEquals(PaymentParser.Channel.TAOBAO, parsed?.channel)
        assertEquals("支付宝", parsed?.accountName)
    }

    @Test
    fun `抖音订单支付成功`() {
        val parsed = parseDouyin("抖音商城", "订单支付成功，实付¥39.90")
        assertEquals(3990L, parsed?.amountCents)
        assertEquals(PaymentParser.Channel.DOUYIN, parsed?.channel)
    }

    @Test
    fun `文案里写了支付方式就按文案算账户`() {
        val parsed = parseMeituan("美团", "支付成功，实付¥30.00（支付宝）")
        assertEquals("支付宝", parsed?.accountName)
    }

    @Test
    fun `电商的待支付 失败 取消 退款都不记账`() {
        assertNull(parseJd("京东", "您的订单待支付 ¥99.00"))
        assertNull(parseMeituan("美团外卖", "支付失败，请重新支付 ¥22.90"))
        assertNull(parseMeituan("美团", "订单已取消 ¥22.90"))
        assertNull(parseTaobao("淘宝", "退款成功 ¥59.90"))
        assertNull(parseDouyin("抖音商城", "订单已关闭 ¥39.90"))
    }

    @Test
    fun `电商没有成功词就不算账`() {
        // 只是推送「您有一笔新订单」，还没付钱
        assertNull(parseMeituan("美团", "您有一笔新订单，去支付"))
    }

    @Test
    fun `包名到渠道的映射`() {
        assertEquals(PaymentParser.Channel.WECHAT, PaymentParser.channelOf("com.tencent.mm"))
        assertEquals(PaymentParser.Channel.ALIPAY, PaymentParser.channelOf("com.eg.android.AlipayGphone"))
        assertEquals(PaymentParser.Channel.TAOBAO, PaymentParser.channelOf("com.taobao.taobao"))
        assertEquals(PaymentParser.Channel.JD, PaymentParser.channelOf("com.jingdong.app.mall"))
        assertEquals(PaymentParser.Channel.MEITUAN, PaymentParser.channelOf("com.sankuai.meituan"))
        assertEquals(PaymentParser.Channel.MEITUAN, PaymentParser.channelOf("com.sankuai.meituan.takeoutnew"))
        assertEquals(PaymentParser.Channel.DOUYIN, PaymentParser.channelOf("com.ss.android.ugc.aweme"))
        assertEquals(PaymentParser.Channel.UNKNOWN, PaymentParser.channelOf("com.example.app"))
        assertEquals(PaymentParser.Channel.UNKNOWN, PaymentParser.channelOf(null))
    }

    @Test
    fun `六个 App 都在监听名单里`() {
        listOf(
            "com.tencent.mm",
            "com.eg.android.AlipayGphone",
            "com.taobao.taobao",
            "com.jingdong.app.mall",
            "com.sankuai.meituan",
            "com.ss.android.ugc.aweme",
        ).forEach { pkg ->
            assertTrue("$pkg 应该在监听名单里", PaymentParser.isWatched(pkg))
        }
        assertFalse(PaymentParser.isWatched("com.example.app"))
        assertFalse(PaymentParser.isWatched(null))
    }
}
