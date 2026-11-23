package com.aiaccounting.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OrderScreenParserTest {

    @Test
    fun `淘宝下单成功页能认出金额和店铺`() {
        val texts = listOf(
            "订单提交成功",
            "请在24小时内完成付款",
            "三只松鼠旗舰店",
            "三只松鼠黄油水牛乳吐司1000g",
            "实付款",
            "¥128.00",
        )
        val event = OrderScreenParser.parse(texts)
        assertEquals(OrderScreenParser.Kind.ORDER, event?.kind)
        assertEquals(12800L, event?.amountCents)
        assertEquals(TxType.EXPENSE, event?.type)
        assertEquals("三只松鼠旗舰店", event?.merchant)
    }

    @Test
    fun `退款成功页记成收入`() {
        val texts = listOf(
            "退款成功",
            "退款金额",
            "¥59.90",
            "已原路退回",
        )
        val event = OrderScreenParser.parse(texts)
        assertEquals(OrderScreenParser.Kind.REFUND, event?.kind)
        assertEquals(5990L, event?.amountCents)
        assertEquals(TxType.INCOME, event?.type)
    }

    @Test
    fun `下单即记：还没付款也要记`() {
        // 用户明确要求「下单之后就记账」，所以待付款的订单页也要收
        val texts = listOf("订单提交成功", "待付款", "¥128.00")
        val event = OrderScreenParser.parse(texts)
        assertEquals(OrderScreenParser.Kind.ORDER, event?.kind)
        assertEquals(12800L, event?.amountCents)
    }

    @Test
    fun `只有状态没有金额时，待付款页也不记`() {
        assertNull(OrderScreenParser.parse(listOf("待付款", "查看订单")))
    }

    @Test
    fun `交易关闭页面不记`() {
        val texts = listOf("交易关闭", "订单已取消", "¥88.00")
        assertNull(OrderScreenParser.parse(texts))
    }

    @Test
    fun `只有状态没有金额不记`() {
        assertNull(OrderScreenParser.parse(listOf("订单提交成功", "查看订单")))
    }

    @Test
    fun `只有金额没有状态不记`() {
        assertNull(OrderScreenParser.parse(listOf("¥128.00", "商品详情")))
    }

    @Test
    fun `千分位金额能换算`() {
        val texts = listOf("付款成功", "实付款 ¥1,299.00")
        assertEquals(129900L, OrderScreenParser.parse(texts)?.amountCents)
    }

    @Test
    fun `元字单位也能识别`() {
        val texts = listOf("下单成功", "合计 45.50 元")
        assertEquals(4550L, OrderScreenParser.parse(texts)?.amountCents)
    }

    @Test
    fun `空文本返回 null`() {
        assertNull(OrderScreenParser.parse(emptyList()))
        assertNull(OrderScreenParser.parse(listOf("", "  ")))
    }
}
