package com.aiaccounting.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class ReportCalculatorTest {

    private val zone: ZoneId = ZoneId.of("Asia/Shanghai")
    private val today = LocalDate.of(2026, 11, 22)

    private fun tx(
        id: Long,
        type: TxType,
        cents: Long,
        date: LocalDate,
        categoryId: Long,
        categoryName: String,
    ): Tx = Tx(
        id = id,
        type = type,
        amountCents = cents,
        occurredAt = date.atTime(12, 0).atZone(zone).toInstant().toEpochMilli(),
        categoryId = categoryId,
        categoryName = categoryName,
        categoryEmoji = "🍜",
        accountId = 1L,
        accountName = "微信",
    )

    private val sample = listOf(
        tx(1, TxType.EXPENSE, 10_000, LocalDate.of(2026, 11, 5), 1L, "餐饮"),
        tx(2, TxType.EXPENSE, 5_000, LocalDate.of(2026, 11, 22), 2L, "交通"),
        tx(3, TxType.INCOME, 100_000, LocalDate.of(2026, 11, 10), 3L, "工资"),
        tx(4, TxType.EXPENSE, 20_000, LocalDate.of(2026, 10, 15), 1L, "餐饮"),
        // 范围外：不应计入
        tx(5, TxType.EXPENSE, 99_999, LocalDate.of(2026, 12, 1), 1L, "餐饮"),
    )

    @Test
    fun `汇总区间内的收支与结余`() {
        val range = ReportPeriods.of(PeriodUnit.MONTH, today)
        val data = ReportCalculator.build(sample, range, today, zone)

        assertEquals(15_000L, data.expenseCents)
        assertEquals(100_000L, data.incomeCents)
        assertEquals(85_000L, data.netCents)
        assertTrue(data.hasData)
    }

    @Test
    fun `日均按已过天数计算`() {
        val range = ReportPeriods.of(PeriodUnit.MONTH, today)
        val data = ReportCalculator.build(sample, range, today, zone)

        // 11 月已过 22 天：15000 / 22 = 681（整除）
        assertEquals(681L, data.averagePerDayCents)
    }

    @Test
    fun `与上期比较`() {
        val range = ReportPeriods.of(PeriodUnit.MONTH, today)
        val data = ReportCalculator.build(sample, range, today, zone)

        assertEquals(20_000L, data.previousExpenseCents)
        assertEquals(-0.25f, data.changeRatio, 0.0001f)
    }

    @Test
    fun `趋势桶落在正确的日期`() {
        val range = ReportPeriods.of(PeriodUnit.MONTH, today)
        val data = ReportCalculator.build(sample, range, today, zone)

        assertEquals(30, data.buckets.size)
        assertEquals(10_000L, data.buckets[4].cents)   // 11-05
        assertEquals(5_000L, data.buckets[21].cents)   // 11-22
        assertEquals(0L, data.buckets[0].cents)        // 11-01
    }

    @Test
    fun `分类占比按金额倒序`() {
        val range = ReportPeriods.of(PeriodUnit.MONTH, today)
        val data = ReportCalculator.build(sample, range, today, zone)

        assertEquals(2, data.categories.size)
        assertEquals("餐饮", data.categories[0].name)
        assertEquals(10_000L, data.categories[0].cents)
        assertEquals(0.6666f, data.categories[0].ratio, 0.001f)
        assertEquals("交通", data.categories[1].name)
    }

    @Test
    fun `空区间没有数据`() {
        val range = ReportPeriods.of(PeriodUnit.MONTH, today)
        val data = ReportCalculator.build(
            listOf(tx(9, TxType.EXPENSE, 100, LocalDate.of(2020, 1, 1), 1L, "餐饮")),
            range,
            today,
            zone,
        )

        assertFalse(data.hasData)
        assertEquals(0L, data.expenseCents)
        assertEquals(0L, data.incomeCents)
    }
    @Test
    fun `sub categories roll up to their parent`() {
        val day = LocalDate.of(2026, 11, 22)
        fun lunch(cents: Long) = Tx(
            id = 100,
            type = TxType.EXPENSE,
            amountCents = cents,
            occurredAt = day.atTime(12, 0).atZone(zone).toInstant().toEpochMilli(),
            categoryId = 11L,
            categoryName = "Lunch",
            categoryEmoji = "X",
            parentCategoryName = "Food",
            parentCategoryEmoji = "F",
            accountId = 1L,
            accountName = "WeChat",
        )

        val data = ReportCalculator.build(
            txs = listOf(lunch(1_000), lunch(2_000)),
            range = ReportPeriods.of(PeriodUnit.MONTH, today),
            today = today,
            zone = zone,
        )

        val slice = data.categories.single { it.name == "Food" }
        assertEquals(3_000L, slice.cents)
    }
}