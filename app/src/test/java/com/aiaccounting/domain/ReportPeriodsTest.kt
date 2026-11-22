package com.aiaccounting.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ReportPeriodsTest {

    private val anchor = LocalDate.of(2026, 11, 22)

    @Test
    fun `周报从周一开始到周日结束`() {
        val range = ReportPeriods.of(PeriodUnit.WEEK, anchor)

        assertEquals(LocalDate.of(2026, 11, 16), range.start)
        assertEquals(LocalDate.of(2026, 11, 22), range.end)
        assertEquals(7, range.days)
        assertEquals("2026.11.16 ~ 11.22", range.title)
        assertEquals(7, range.buckets.size)
        assertEquals("一", range.buckets.first().label)
        assertEquals("日", range.buckets.last().label)

        assertEquals(LocalDate.of(2026, 11, 9), range.previousStart)
        assertEquals(LocalDate.of(2026, 11, 15), range.previousEnd)
    }

    @Test
    fun `月报覆盖整月天数`() {
        val range = ReportPeriods.of(PeriodUnit.MONTH, anchor)

        assertEquals(LocalDate.of(2026, 11, 1), range.start)
        assertEquals(LocalDate.of(2026, 11, 30), range.end)
        assertEquals(30, range.buckets.size)
        assertEquals("2026年11月", range.title)
        assertEquals(LocalDate.of(2026, 10, 1), range.previousStart)
        assertEquals(LocalDate.of(2026, 10, 31), range.previousEnd)
    }

    @Test
    fun `闰年二月天数正确`() {
        val range = ReportPeriods.of(PeriodUnit.MONTH, LocalDate.of(2028, 2, 10))
        assertEquals(29, range.days)
        assertEquals(29, range.buckets.size)
    }

    @Test
    fun `年报十二个月桶`() {
        val range = ReportPeriods.of(PeriodUnit.YEAR, anchor)

        assertEquals(LocalDate.of(2026, 1, 1), range.start)
        assertEquals(LocalDate.of(2026, 12, 31), range.end)
        assertEquals(12, range.buckets.size)
        assertEquals("1月", range.buckets.first().label)
        assertEquals("12月", range.buckets.last().label)
        assertEquals("2026年", range.title)
    }

    @Test
    fun `翻页`() {
        assertEquals(LocalDate.of(2026, 11, 29), ReportPeriods.shift(PeriodUnit.WEEK, anchor, 1))
        assertEquals(LocalDate.of(2026, 10, 22), ReportPeriods.shift(PeriodUnit.MONTH, anchor, -1))
        assertEquals(LocalDate.of(2027, 11, 22), ReportPeriods.shift(PeriodUnit.YEAR, anchor, 1))
    }

    @Test
    fun `contains 边界包含`() {
        val range = ReportPeriods.of(PeriodUnit.MONTH, anchor)
        assertTrue(range.contains(LocalDate.of(2026, 11, 1)))
        assertTrue(range.contains(LocalDate.of(2026, 11, 30)))
        assertTrue(!range.contains(LocalDate.of(2026, 10, 31)))
        assertTrue(!range.contains(LocalDate.of(2026, 12, 1)))
    }
}
