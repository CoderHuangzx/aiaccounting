package com.aiaccounting.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class BackupCsvTest {

    private val zone: ZoneId = ZoneId.of("Asia/Shanghai")

    private fun millisOf(date: String, hour: Int, minute: Int): Long =
        LocalDate.parse(date).atTime(hour, minute).atZone(zone).toInstant().toEpochMilli()

    private fun row(
        amount: Long = 5200,
        merchant: String? = "瑞幸咖啡",
        note: String? = null,
        type: TxType = TxType.EXPENSE,
        source: TxSource = TxSource.MANUAL,
        dedupKey: String? = null,
    ) = BackupCsv.Row(
        occurredAt = millisOf("2026-11-20", 9, 30),
        type = type,
        amountCents = amount,
        category = "餐饮",
        account = "微信",
        merchant = merchant,
        note = note,
        source = source,
        dedupKey = dedupKey,
    )

    @Test
    fun `表头是固定的中文列名`() {
        val csv = BackupCsv.encode(listOf(row()))
        assertEquals(BackupCsv.HEADER, csv.lineSequence().first())
    }

    @Test
    fun `编码后再解码字段一致`() {
        val original = row(amount = 12345, note = "和同事一起", dedupKey = "ch:00012345")
        val decoded = BackupCsv.decode(BackupCsv.encode(listOf(original)), zone)

        assertEquals(1, decoded.size)
        val item = decoded.first()
        assertEquals(original.occurredAt, item.occurredAt)
        assertEquals(original.amountCents, item.amountCents)
        assertEquals("餐饮", item.category)
        assertEquals("微信", item.account)
        assertEquals("瑞幸咖啡", item.merchant)
        assertEquals("和同事一起", item.note)
        assertEquals(TxType.EXPENSE, item.type)
        assertEquals(TxSource.MANUAL, item.source)
        assertEquals("ch:00012345", item.dedupKey)
    }

    @Test
    fun `字段里的逗号引号换行都能转义`() {
        val tricky = row(merchant = "老王,烧烤", note = "他说\"下次我请\"\n还有别的事")
        val csv = BackupCsv.encode(listOf(tricky))
        val decoded = BackupCsv.decode(csv, zone).first()

        assertEquals("老王,烧烤", decoded.merchant)
        assertEquals("他说\"下次我请\"\n还有别的事", decoded.note)
    }

    @Test
    fun `空商户空备注解码为 null`() {
        val decoded = BackupCsv.decode(BackupCsv.encode(listOf(row(merchant = null, note = null))), zone).first()
        assertNull(decoded.merchant)
        assertNull(decoded.note)
    }

    @Test
    fun `收入与通知来源能正确往返`() {
        val decoded = BackupCsv.decode(
            BackupCsv.encode(listOf(row(type = TxType.INCOME, source = TxSource.NOTIFICATION))),
            zone,
        ).first()
        assertEquals(TxType.INCOME, decoded.type)
        assertEquals(TxSource.NOTIFICATION, decoded.source)
    }

    @Test
    fun `空文本解码为空列表`() {
        assertTrue(BackupCsv.decode("   ").isEmpty())
    }

    @Test
    fun `不是备份文件时报错`() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            BackupCsv.decode("name,age\n张三,18")
        }
        assertTrue("错误信息应提到表头", error.message!!.contains("表头"))
    }

    @Test
    fun `金额格式不对时报出具体行号`() {
        val bad = BackupCsv.HEADER + "\n2026-11-20 09:30,支出,abc,餐饮,微信,,,手动,"
        val error = assertThrows(IllegalArgumentException::class.java) {
            BackupCsv.decode(bad, zone)
        }
        assertTrue("错误信息应包含行号", error.message!!.contains("第 2 行"))
    }
}
