package com.aiaccounting.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class WeChatBillParserTest {

    private val zone: ZoneId = ZoneId.of("Asia/Shanghai")

    private val header = listOf(
        "交易时间", "交易类型", "交易对方", "商品", "收/支", "金额(元)",
        "支付方式", "当前状态", "交易单号", "商户单号", "备注",
    )

    /** 结构照抄真实导出：前面一堆说明行，表头在第 18 行，时间是 Excel 序列号。 */
    private val rows: List<List<String>> = listOf(
        listOf("微信支付账单明细"),
        listOf("微信昵称：[测试]"),
        listOf("起始时间：[2026-09-25 00:00:00] 终止时间：[2026-10-25 00:14:10]"),
        listOf("导出类型：[全部账单]"),
        listOf("共3笔记录"),
        listOf("----------------------微信支付账单明细列表--------------------"),
        header,
        listOf("46319.380428240744", "商户消费", "窝里快购", "先购后付", "支出", "2.72", "零钱", "支付成功", "4200003237202610247579225936", "SHH1", "/"),
        listOf("46318.56334490741", "转账", "何致远", "转账备注:微信转账", "收入", "3200", "/", "已存入零钱", "1000050001202610230031464407531", "/", "/"),
        listOf("46290.83652777778", "商户消费", "美宜佳", "美宜佳店", "/", "2", "零钱", "充值完成", "420000315920261025", "/", "/"),
        listOf("46318.905381944445", "商户消费", "腾讯", "QQ阅读", "支出", "18", "零钱", "已全额退款", "4200003256202610237274409301", "/", "/"),
    )

    @Test
    fun `只收支出与收入，跳过中性交易和退款`() {
        val parsed = WeChatBillParser.parse(rows, zone)

        assertEquals(2, parsed.size)
        assertEquals(272L, parsed[0].amountCents)
        assertEquals(TxType.EXPENSE, parsed[0].type)
        assertEquals(320000L, parsed[1].amountCents)
        assertEquals(TxType.INCOME, parsed[1].type)
    }

    @Test
    fun `Excel 序列号时间换算正确`() {
        val parsed = WeChatBillParser.parse(rows, zone)

        val expected = LocalDate.of(2026, 10, 24).atTime(9, 7, 49)
            .atZone(zone).toInstant().toEpochMilli()
        assertEquals(expected, parsed[0].occurredAt)
    }

    @Test
    fun `序列号换算工具单独可用`() {
        val expected = LocalDate.of(2026, 10, 24).atTime(9, 7, 49)
            .atZone(zone).toInstant().toEpochMilli()
        assertEquals(expected, WeChatBillParser.excelSerialToMillis(46319.380428240744, zone))
    }

    @Test
    fun `交易对方与商品名取出来，斜杠视为空`() {
        val parsed = WeChatBillParser.parse(rows, zone)
        assertEquals("窝里快购", parsed[0].counterparty)
        assertEquals("先购后付", parsed[0].description)
        assertEquals("4200003237202610247579225936", parsed[0].tradeNo)
    }

    @Test
    fun `金额带货币符号或千分位也能解析`() {
        val withSymbol = listOf(
            header,
            listOf("46319.38", "商户消费", "某商户", "商品", "支出", "¥1,234.50", "零钱", "支付成功", "T1", "/", "/"),
        )
        val parsed = WeChatBillParser.parse(withSymbol, zone)
        assertEquals(123450L, parsed.single().amountCents)
    }

    @Test
    fun `文本日期也能兼容`() {
        val textDate = listOf(
            header,
            listOf("2026-10-24 09:07:49", "商户消费", "某商户", "商品", "支出", "12.00", "零钱", "支付成功", "T2", "/", "/"),
        )
        val parsed = WeChatBillParser.parse(textDate, zone)
        assertEquals(
            LocalDate.of(2026, 10, 24).atTime(9, 7, 49).atZone(zone).toInstant().toEpochMilli(),
            parsed.single().occurredAt,
        )
    }

    @Test
    fun `不是微信账单时报错`() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            WeChatBillParser.parse(listOf(listOf("a", "b")), zone)
        }
        assertTrue(error.message!!.contains("交易时间"))
    }

    @Test
    fun `缺关键列时报错`() {
        val broken = listOf(listOf("交易时间", "收/支"))
        assertThrows(IllegalArgumentException::class.java) {
            WeChatBillParser.parse(broken, zone)
        }
    }
}
