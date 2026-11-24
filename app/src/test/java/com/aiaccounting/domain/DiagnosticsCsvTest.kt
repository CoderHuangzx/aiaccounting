package com.aiaccounting.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class DiagnosticsCsvTest {

    private val zone: ZoneId = ZoneId.of("Asia/Shanghai")

    @Test
    fun `规则能编码再解码`() {
        val rows = listOf(
            DiagnosticsCsv.RuleRow("瑞幸咖啡", "餐饮", 5),
            DiagnosticsCsv.RuleRow("starbucks", "餐饮", 2),
        )
        val csv = DiagnosticsCsv.encodeRules(rows)
        assertTrue("应有中文表头", csv.startsWith("商户,分类,命中次数"))

        assertEquals(rows, DiagnosticsCsv.decodeRules(csv))
    }

    @Test
    fun `商户名带逗号也能正确往返`() {
        val rows = listOf(DiagnosticsCsv.RuleRow("瑞幸,南京店", "餐饮", 1))
        assertEquals(rows, DiagnosticsCsv.decodeRules(DiagnosticsCsv.encodeRules(rows)))
    }

    @Test
    fun `规则文件表头不对时报错`() {
        assertThrows(IllegalArgumentException::class.java) {
            DiagnosticsCsv.decodeRules("日期,类型\n2026-11-24,支出")
        }
    }

    @Test
    fun `空规则文件返回空列表`() {
        assertTrue(DiagnosticsCsv.decodeRules("").isEmpty())
        assertTrue(DiagnosticsCsv.decodeRules("商户,分类,命中次数\n").isEmpty())
    }

    @Test
    fun `捕捉日志编码包含时间与解析状态`() {
        val at = LocalDate.of(2026, 11, 24).atTime(21, 30, 5)
            .atZone(zone).toInstant().toEpochMilli()
        val rows = listOf(
            DiagnosticsCsv.CaptureRow(
                capturedAt = at,
                appLabel = "美团",
                packageName = "com.sankuai.meituan",
                title = "美团月付",
                text = "成功支付21.90元",
                parsed = true,
                reason = null,
            ),
            DiagnosticsCsv.CaptureRow(
                capturedAt = at + 60_000,
                appLabel = "支付宝",
                packageName = "com.eg.android.AlipayGphone",
                title = "广告",
                text = "低至8.9元起",
                parsed = false,
                reason = "没匹配到支付成功规则",
            ),
        )

        val csv = DiagnosticsCsv.encodeCaptures(rows, zone)
        val lines = csv.lines().filter { it.isNotBlank() }
        assertEquals(3, lines.size)
        assertTrue(lines[0].startsWith("时间,应用,包名"))
        assertTrue(lines[1].contains("2026-11-24 21:30:05"))
        assertTrue(lines[1].contains("美团"))
        assertTrue(lines[1].contains("是"))
        assertTrue(lines[2].contains("否"))
        assertTrue(lines[2].contains("没匹配到支付成功规则"))
    }

    @Test
    fun `正文里有换行也不会把记录拆坏`() {
        val at = LocalDate.of(2026, 11, 24).atTime(10, 0).atZone(zone).toInstant().toEpochMilli()
        val rows = listOf(
            DiagnosticsCsv.CaptureRow(
                capturedAt = at,
                appLabel = "支付宝",
                packageName = "pkg",
                title = "标题",
                text = "第一行\n第二行",
                parsed = false,
                reason = null,
            ),
        )
        val csv = DiagnosticsCsv.encodeCaptures(rows, zone)
        // 用同一个解析器读回来，确认仍是 1 条记录
        val records = CsvCodec.parseNonBlank(csv)
        assertEquals(2, records.size)
        assertEquals("第一行\n第二行", records[1][4])
    }
}
