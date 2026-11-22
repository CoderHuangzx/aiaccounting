package com.aiaccounting.domain

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId

/**
 * 记一笔时正在编辑的日期时间。
 *
 * 单独抽出来是因为滚动选择器需要频繁改年/月/日：改月之后原来的 31 号可能要退回 30 号，
 * 这类「天数越界」的处理必须可测，不能埋在 UI 里。
 */
data class DateTimeDraft(
    val year: Int,
    val month: Int,
    val day: Int,
    val hour: Int,
    val minute: Int,
) {
    val daysInMonth: Int get() = YearMonth.of(year, month).lengthOfMonth()

    val date: LocalDate get() = LocalDate.of(year, month, day.coerceIn(1, daysInMonth))

    fun withYear(value: Int): DateTimeDraft = copy(year = value).clampDay()

    fun withMonth(value: Int): DateTimeDraft = copy(month = value.coerceIn(1, 12)).clampDay()

    fun withDay(value: Int): DateTimeDraft = copy(day = value.coerceIn(1, daysInMonth))

    fun withHour(value: Int): DateTimeDraft = copy(hour = value.coerceIn(0, 23))

    fun withMinute(value: Int): DateTimeDraft = copy(minute = value.coerceIn(0, 59))

    fun toMillis(zone: ZoneId = ZoneId.systemDefault()): Long =
        LocalDate.of(year, month, date.dayOfMonth)
            .atTime(LocalTime.of(hour, minute))
            .atZone(zone)
            .toInstant()
            .toEpochMilli()

    private fun clampDay(): DateTimeDraft = copy(day = day.coerceIn(1, daysInMonth))

    companion object {
        fun from(millis: Long, zone: ZoneId = ZoneId.systemDefault()): DateTimeDraft {
            val zoned = Instant.ofEpochMilli(millis).atZone(zone)
            return DateTimeDraft(
                year = zoned.year,
                month = zoned.monthValue,
                day = zoned.dayOfMonth,
                hour = zoned.hour,
                minute = zoned.minute,
            )
        }
    }
}
