package com.aiaccounting.domain

import com.aiaccounting.core.DateLabels
import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class PeriodUnit(val label: String) {
    WEEK("周报"),
    MONTH("月报"),
    YEAR("年报"),
}

/** 趋势图的一根柱子：标签 + 该桶的日期范围。 */
data class BucketSpec(
    val label: String,
    val start: LocalDate,
    val end: LocalDate,
)

/** 一次报表查询的时间范围，含上一周期（用于「比上期」比较）。 */
data class PeriodRange(
    val unit: PeriodUnit,
    val start: LocalDate,
    val end: LocalDate,
    val title: String,
    val previousStart: LocalDate,
    val previousEnd: LocalDate,
    val buckets: List<BucketSpec>,
) {
    val days: Int get() = (ChronoUnit.DAYS.between(start, end) + 1).toInt()

    fun contains(date: LocalDate): Boolean = !date.isBefore(start) && !date.isAfter(end)

    fun containsPrevious(date: LocalDate): Boolean =
        !date.isBefore(previousStart) && !date.isAfter(previousEnd)
}

object ReportPeriods {

    private val WEEK_LABELS = arrayOf("一", "二", "三", "四", "五", "六", "日")

    fun of(unit: PeriodUnit, anchor: LocalDate): PeriodRange = when (unit) {
        PeriodUnit.WEEK -> week(anchor)
        PeriodUnit.MONTH -> month(anchor)
        PeriodUnit.YEAR -> year(anchor)
    }

    /** ‹ › 翻页。 */
    fun shift(unit: PeriodUnit, anchor: LocalDate, delta: Int): LocalDate = when (unit) {
        PeriodUnit.WEEK -> anchor.plusWeeks(delta.toLong())
        PeriodUnit.MONTH -> anchor.plusMonths(delta.toLong())
        PeriodUnit.YEAR -> anchor.plusYears(delta.toLong())
    }

    private fun week(anchor: LocalDate): PeriodRange {
        val start = anchor.minusDays((anchor.dayOfWeek.value - 1).toLong())
        val end = start.plusDays(6)
        val buckets = (0..6).map { offset ->
            val day = start.plusDays(offset.toLong())
            BucketSpec(WEEK_LABELS[offset], day, day)
        }
        val title = "${start.year}.${DateLabels.monthDay(start)} ~ ${DateLabels.monthDay(end)}"
        return PeriodRange(
            unit = PeriodUnit.WEEK,
            start = start,
            end = end,
            title = title,
            previousStart = start.minusWeeks(1),
            previousEnd = start.minusDays(1),
            buckets = buckets,
        )
    }

    private fun month(anchor: LocalDate): PeriodRange {
        val start = anchor.withDayOfMonth(1)
        val end = start.plusMonths(1).minusDays(1)
        val dayCount = end.dayOfMonth
        val buckets = (1..dayCount).map { day ->
            val date = start.withDayOfMonth(day)
            BucketSpec(day.toString(), date, date)
        }
        return PeriodRange(
            unit = PeriodUnit.MONTH,
            start = start,
            end = end,
            title = DateLabels.month(start),
            previousStart = start.minusMonths(1),
            previousEnd = start.minusDays(1),
            buckets = buckets,
        )
    }

    private fun year(anchor: LocalDate): PeriodRange {
        val start = anchor.withDayOfMonth(1).withMonth(1)
        val end = start.plusYears(1).minusDays(1)
        val buckets = (1..12).map { month ->
            val monthStart = start.withMonth(month)
            BucketSpec("${month}月", monthStart, monthStart.plusMonths(1).minusDays(1))
        }
        return PeriodRange(
            unit = PeriodUnit.YEAR,
            start = start,
            end = end,
            title = "${start.year}年",
            previousStart = start.minusYears(1),
            previousEnd = start.minusDays(1),
            buckets = buckets,
        )
    }
}
