package com.aiaccounting.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.charset.Charset
import java.time.LocalDate
import java.time.ZoneId

class AlipayBillParserTest {

    private val zone: ZoneId = ZoneId.of("Asia/Shanghai")

    /** 尽量贴近真实导出的结构：说明行 + 表头 + 数据 + 末尾分隔线。 */
    private val sample = """
        支付宝交易记录明细查询
        账号:[test@example.com]
        起始日期:[2026-10-01 00:00:00]    终止日期:[2026-11-01 00:00:00]
        ---------------------------------交易记录明细列表------------------------------------
        交易号,商家订单号,交易创建时间,付款时间,最近修改时间,交易来源地,类型,交易对方,商品名称,金额（元）,收/支,交易状态,服务费（元）,成功退款（元）,备注,资金状态
        2026111322001453091000000001,T200P123,2026-11-13 08:29:54,2026-11-13 08:29:54,2026-11-13 08:29:54,其他,即时到账交易,瑞幸咖啡,瑞幸咖啡,12.00,支出,交易成功,0.00,0.00,,已支出
        2026111322001453091000000002,T200P456,2026-11-14 19:02:11,2026-11-14 19:02:11,2026-11-14 19:02:11,其他,即时到账交易,滴滴出行,快车,18.60,支出,交易成功,0.00,0.00,,已支出
        2026111322001453091000000003,T200P789,2026-11-15 10:00:00,2026-11-15 10:00:00,2026-11-15 10:00:00,其他,即时到账交易,某商户,订单,50.00,支出,交易关闭,0.00,0.00,,已关闭
        2026111322001453091000000004,T200P000,2026-11-16 09:00:00,2026-11-16 09:00:00,2026-11-16 09:00:00,其他,即时到账交易,工资,11月工资,8000.00,收入,交易成功,0.00,0.00,,已收入
        2026111322001453091000000005,T200P111,2026-11-17 12:00:00,2026-11-17 12:00:00,2026-11-17 12:00:00,其他,即时到账交易,余额宝,转入,100.00,不计收支,交易成功,0.00,0.00,,已支出
        --------------------------------------------------------------------------------
    """.trimIndent()

    @Test
    fun `只收交易成功的支出与收入`() {
        val rows = AlipayBillParser.parse(sample, zone)

        assertEquals(3, rows.size)
        assertEquals(listOf(1200L, 1860L, 800000L), rows.map { it.amountCents })
        assertEquals(
            listOf(TxType.EXPENSE, TxType.EXPENSE, TxType.INCOME),
            rows.map { it.type },
        )
    }

    @Test
    fun `交易对方与商品名被正确取出`() {
        val first = AlipayBillParser.parse(sample, zone).first()
        assertEquals("瑞幸咖啡", first.counterparty)
        assertEquals("瑞幸咖啡", first.description)
        assertEquals("2026111322001453091000000001", first.tradeNo)
    }

    @Test
    fun `时间按本地时区换算`() {
        val first = AlipayBillParser.parse(sample, zone).first()
        val expected = LocalDate.of(2026, 11, 13).atTime(8, 29, 54)
            .atZone(zone).toInstant().toEpochMilli()
        assertEquals(expected, first.occurredAt)
    }

    @Test
    fun `关闭与不计收支的行被跳过`() {
        val rows = AlipayBillParser.parse(sample, zone)
        assertTrue(rows.none { it.amountCents == 5000L })   // 交易关闭
        assertTrue(rows.none { it.amountCents == 10000L })  // 不计收支
    }

    @Test
    fun `字段里带逗号也能解析`() {
        val text = sample.replace("瑞幸咖啡,瑞幸咖啡", "\"瑞幸咖啡,南京店\",\"咖啡,大杯\"")
        val first = AlipayBillParser.parse(text, zone).first()
        assertEquals("瑞幸咖啡,南京店", first.counterparty)
        assertEquals("咖啡,大杯", first.description)
    }

    @Test
    fun `不是支付宝账单时报错`() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            AlipayBillParser.parse("日期,类型,金额\n2026-11-13,支出,12.00", zone)
        }
        assertTrue(error.message!!.contains("交易号"))
    }

    @Test
    fun `表头缺列时报错`() {
        val broken = "交易号,交易对方\n1,瑞幸咖啡"
        assertThrows(IllegalArgumentException::class.java) {
            AlipayBillParser.parse(broken, zone)
        }
    }

    @Test
    fun `GBK 编码的文件能正确解码`() {
        val bytes = sample.toByteArray(Charset.forName("GBK"))
        val decoded = AlipayBillParser.decode(bytes)
        val rows = AlipayBillParser.parse(decoded, zone)
        assertEquals(3, rows.size)
        assertEquals("瑞幸咖啡", rows.first().counterparty)
    }

    @Test
    fun `UTF-8 编码的文件也能兜底解码`() {
        val bytes = sample.toByteArray(Charsets.UTF_8)
        val rows = AlipayBillParser.parse(AlipayBillParser.decode(bytes), zone)
        assertEquals(3, rows.size)
    }

    @Test
    fun `空文件返回空列表`() {
        assertTrue(AlipayBillParser.parse("").isEmpty())
    }

    @Test
    fun `交易对方为斜杠时视为空`() {
        val text = sample.replace("即时到账交易,瑞幸咖啡,", "即时到账交易,/,")
        val first = AlipayBillParser.parse(text, zone).first()
        assertNull(first.counterparty)
    }
}
