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
    fun `抖音先用后付页面取确认收货后付的金额`() {
        // 复刻真机截图：商品价 21.36，实际要付 11.36，订单号很长
        val texts = listOf(
            "打包中",
            "预计11月26日(周四)送达",
            "韩滋兄弟",
            "【整箱20包】麻薯面包球办公室充饥…",
            "¥21.36",
            "确认收货后付",
            "¥11.36",
            "订单编号",
            "6919901061489831637",
        )
        val event = OrderScreenParser.parse(texts)
        assertEquals(OrderScreenParser.Kind.ORDER, event?.kind)
        assertEquals(1136L, event?.amountCents)
        assertEquals("6919901061489831637", event?.orderNo)
    }

    @Test
    fun `页面上的裸价格不会被当订单金额`() {
        // 只有状态词 + 若干没有标签的价格：宁可不记
        val texts = listOf("支付成功", "¥21.36", "¥18.90", "加入购物车")
        assertNull(OrderScreenParser.parse(texts))
    }

    @Test
    fun `记账界面自己的文字不能被误认`() {
        // 真机 bug 复现：读到了我们自己的界面（备注里带「读屏：支付成功」）
        val texts = listOf(
            "今日支出（元）",
            "¥ 10.22",
            "本月支出（元）",
            "10.22",
            "其他 通知记账 23:09 · 微信 · 读屏：支付成功 -5.11",
            "其他 通知记账 23:06 · 微信 · 读屏：支付成功 -4.11",
        )
        assertNull(OrderScreenParser.parse(texts))
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
    fun `交易关闭页面不记`() {
        val texts = listOf("交易关闭", "订单已取消", "实付款 ¥88.00")
        assertNull(OrderScreenParser.parse(texts))
    }

    @Test
    fun `只有状态没有金额不记`() {
        assertNull(OrderScreenParser.parse(listOf("订单提交成功", "查看订单")))
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
    fun `金额大得离谱时放弃`() {
        val texts = listOf("下单成功", "合计 ¥9,999,999.00")
        assertNull(OrderScreenParser.parse(texts))
    }

    @Test
    fun `空文本返回 null`() {
        assertNull(OrderScreenParser.parse(emptyList()))
        assertNull(OrderScreenParser.parse(listOf("", "  ")))
    }

    @Test
    fun `美团订单完成页取实付金额`() {
        // 复刻真机截图：标题「订单已完成」，支付方式里带「美团月付(实付¥21.9)」，订单号被空格分组
        val texts = listOf(
            "尊敬的黒金会员，订单已完成",
            "订单信息",
            "期望时间 立即配送",
            "配送服务 美团快送",
            "订单号码 2002 4026 0119 1056 383",
            "下单时间 2026-11-24 11:27:41",
            "支付方式 美团月付(实付¥21.9)",
        )
        val event = OrderScreenParser.parse(texts)
        assertEquals(OrderScreenParser.Kind.ORDER, event?.kind)
        assertEquals(2190L, event?.amountCents)
        assertEquals("2002402601191056383", event?.orderNo)
    }
}
