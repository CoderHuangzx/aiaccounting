package com.aiaccounting.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** 微信「支付成功」页面的读屏解析（2026-11-26 真机文案）。 */
class WeChatPayScreenParserTest {

    @Test
    fun `支付成功页能读出金额与收款方`() {
        val texts = listOf(
            "支付成功",
            "只是不想失去您（*萍）",
            "¥ 3.50",
            "使用零钱支付",
            "交易状态",
            "支付成功，对方已收款",
            "完成",
        )
        val parsed = WeChatPayScreenParser.parse(texts)
        assertEquals(350L, parsed?.amountCents)
        assertEquals("只是不想失去您（*萍）", parsed?.merchant)
    }

    @Test
    fun `交易详情页按带负号的金额识别`() {
        val texts = listOf(
            "主页",
            "交易详情",
            "-3.50",
            "扫二维码付款-给只是不想失去您",
            "当前状态",
            "支付成功",
            "收款方备注",
            "二维码收款",
            "支付方式",
            "零钱",
            "转账时间",
            "2026年11月25日 08:21:45",
            "转账单号",
            "10001073012026112500209018632084",
        )
        val parsed = WeChatPayScreenParser.parse(texts)
        assertEquals(350L, parsed?.amountCents)
        assertEquals("只是不想失去您", parsed?.merchant)
    }

    @Test
    fun `微信收款码页面算支出而不是收入`() {
        val texts = listOf("支付成功", "某商户", "¥ 12.80", "使用零钱支付")
        val parsed = WeChatPayScreenParser.parse(texts)
        assertEquals(1280L, parsed?.amountCents)
    }

    // ── 收款页面（2026-11-26 真机：你已收款 ¥1800.00）──────────────────

    @Test
    fun `收款页面记成收入`() {
        val texts = listOf(
            "你已收款，资金已存入零钱",
            "¥ 1800.00",
            "零钱余额",
            "转账时间",
            "2026年11月26日 17:28:11",
            "收款时间",
            "2026年11月26日 17:28:30",
            "账单详情",
        )
        val parsed = WeChatPayScreenParser.parse(texts)
        assertEquals(180000L, parsed?.amountCents)
        assertEquals(TxType.INCOME, parsed?.type)
    }

    @Test
    fun `收款到账也能认出是收入`() {
        val texts = listOf("收款到账", "¥ 20.00", "零钱余额", "账单详情")
        val parsed = WeChatPayScreenParser.parse(texts)
        assertEquals(2000L, parsed?.amountCents)
        assertEquals(TxType.INCOME, parsed?.type)
    }

    @Test
    fun `付款页仍然是支出`() {
        val texts = listOf("支付成功", "只是不想失去您（*萍）", "¥ 3.50", "使用零钱支付", "完成")
        assertEquals(TxType.EXPENSE, WeChatPayScreenParser.parse(texts)?.type)
    }

    @Test
    fun `普通聊天页面不会被当成支付`() {
        val texts = listOf("只是不想失去您", "晚上一起吃饭吗", "19:30", "3.50")
        assertNull(WeChatPayScreenParser.parse(texts))
    }

    @Test
    fun `聊天里提到支付成功但没有金额也不记`() {
        val texts = listOf("支付成功了吗？", "好的", "完成了")
        assertNull(WeChatPayScreenParser.parse(texts))
    }

    @Test
    fun `营销文案不记`() {
        val texts = listOf("支付成功", "立减5元", "¥ 3.50", "使用零钱支付")
        assertNull(WeChatPayScreenParser.parse(texts))
    }

    @Test
    fun `金额为0不记`() {
        val texts = listOf("支付成功", "¥ 0.00", "使用零钱支付")
        assertNull(WeChatPayScreenParser.parse(texts))
    }
}
