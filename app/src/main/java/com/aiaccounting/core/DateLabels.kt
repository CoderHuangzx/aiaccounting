package com.aiaccounting.core

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object DateLabels {

    private val WEEKDAYS = arrayOf("星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日")
    private val DAY = DateTimeFormatter.ofPattern("yyyy.MM.dd")
    private val MONTH_DAY = DateTimeFormatter.ofPattern("MM.dd")
    private val MONTH = DateTimeFormatter.ofPattern("yyyy年M月")
    private val TIME = DateTimeFormatter.ofPattern("HH:mm")

    fun weekday(date: LocalDate): String = WEEKDAYS[date.dayOfWeek.value - 1]

    fun day(date: LocalDate): String = date.format(DAY)

    fun monthDay(date: LocalDate): String = date.format(MONTH_DAY)

    fun month(date: LocalDate): String = date.format(MONTH)

    fun time(millis: Long, zone: ZoneId): String =
        Instant.ofEpochMilli(millis).atZone(zone).format(TIME)

    fun dayHeader(date: LocalDate, today: LocalDate): String = when (date) {
        today -> "今天 ${weekday(date)}"
        today.minusDays(1) -> "昨天 ${weekday(date)}"
        else -> "${day(date)} ${weekday(date)}"
    }
}
