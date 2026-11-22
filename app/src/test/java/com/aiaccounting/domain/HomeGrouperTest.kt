package com.aiaccounting.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class HomeGrouperTest {

    private val zone: ZoneId = ZoneId.of("Asia/Shanghai")

    private fun tx(
        id: Long,
        type: TxType,
        cents: Long,
        date: LocalDate,
        hour: Int = 12,
        categoryId: Long = 1L,
        categoryName: String = "餐饮",
    ): Tx = Tx(
        id = id,
        type = type,
        amountCents = cents,
        occurredAt = date.atTime(hour, 0).atZone(zone).toInstant().toEpochMilli(),
        categoryId = categoryId,
        categoryName = categoryName,
        categoryEmoji = "🍜",
        accountId = 1L,
        accountName = "微信",
    )

    @Test
    fun `按天倒序分组并汇总当天收支`() {
        val today = LocalDate.of(2026, 11, 22)
        val txs = listOf(
            tx(1, TxType.EXPENSE, 2_750, today, hour = 17),
            tx(2, TxType.EXPENSE, 2_469, today, hour = 11),
            tx(3, TxType.INCOME, 10_000, today.minusDays(1), hour = 9),
            tx(4, TxType.EXPENSE, 4_510, today.minusDays(1), hour = 20),
        )

        val sections = HomeGrouper.group(txs, today, zone)

        assertEquals(2, sections.size)
        assertEquals(today, sections[0].date)
        assertEquals(5_219L, sections[0].expenseCents)
        assertEquals(0L, sections[0].incomeCents)
        assertEquals(2, sections[0].items.size)
        // 同一天内按时间倒序
        assertEquals(1L, sections[0].items[0].id)

        assertEquals(today.minusDays(1), sections[1].date)
        assertEquals(4_510L, sections[1].expenseCents)
        assertEquals(10_000L, sections[1].incomeCents)
    }

    @Test
    fun `今天与昨天使用相对标签`() {
        val today = LocalDate.of(2026, 11, 22)
        val sections = HomeGrouper.group(
            listOf(
                tx(1, TxType.EXPENSE, 100, today),
                tx(2, TxType.EXPENSE, 200, today.minusDays(1)),
                tx(3, TxType.EXPENSE, 300, today.minusDays(5)),
            ),
            today,
            zone,
        )

        assertTrue(sections[0].label.startsWith("今天 "))
        assertTrue(sections[1].label.startsWith("昨天 "))
        assertEquals("2026.11.17 星期二", sections[2].label)
    }

    @Test
    fun `summary 只统计本月且今日单独累计`() {
        val today = LocalDate.of(2026, 11, 22)
        val txs = listOf(
            tx(1, TxType.EXPENSE, 1_000, today),
            tx(2, TxType.EXPENSE, 2_000, today.minusDays(3)),
            tx(3, TxType.INCOME, 50_000, today.minusDays(1)),
            // 上月，不应计入
            tx(4, TxType.EXPENSE, 9_999, LocalDate.of(2026, 10, 31)),
        )

        val summary = HomeGrouper.summary(txs, today, zone)

        assertEquals(1_000L, summary.todayExpenseCents)
        assertEquals(3_000L, summary.monthExpenseCents)
        assertEquals(50_000L, summary.monthIncomeCents)
        assertEquals("2026年11月", summary.monthLabel)
    }

    @Test
    fun `空数据返回空分组`() {
        val summary = HomeGrouper.summary(emptyList(), LocalDate.of(2026, 11, 22), zone)
        assertEquals(0L, summary.monthExpenseCents)
        assertTrue(HomeGrouper.group(emptyList(), LocalDate.of(2026, 11, 22), zone).isEmpty())
    }
}
