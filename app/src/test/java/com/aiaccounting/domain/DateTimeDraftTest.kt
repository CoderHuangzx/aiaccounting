package com.aiaccounting.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class DateTimeDraftTest {

    private val zone: ZoneId = ZoneId.of("Asia/Shanghai")

    private fun millisOf(y: Int, m: Int, d: Int, h: Int, min: Int): Long =
        LocalDate.of(y, m, d).atTime(h, min).atZone(zone).toInstant().toEpochMilli()

    @Test
    fun `毫秒与草稿互转`() {
        val millis = millisOf(2026, 11, 22, 23, 53)
        val draft = DateTimeDraft.from(millis, zone)

        assertEquals(2026, draft.year)
        assertEquals(11, draft.month)
        assertEquals(22, draft.day)
        assertEquals(23, draft.hour)
        assertEquals(53, draft.minute)
        assertEquals(millis, draft.toMillis(zone))
    }

    @Test
    fun `闰年二月二十九还能保住`() {
        val draft = DateTimeDraft.from(millisOf(2028, 2, 29, 10, 0), zone)
        assertEquals(29, draft.day)

        // 换成平年，29 号必须被夹到 28
        val clamped = draft.withYear(2027)
        assertEquals(28, clamped.day)
        assertEquals(millisOf(2027, 2, 28, 10, 0), clamped.toMillis(zone))
    }

    @Test
    fun `换到只有三十天的月份时日期夹紧`() {
        val draft = DateTimeDraft(2026, 1, 31, 12, 0).withMonth(4)
        assertEquals(30, draft.day)

        val february = draft.withMonth(2)
        assertEquals(28, february.day)
    }

    @Test
    fun `越界的日时分被夹紧`() {
        val draft = DateTimeDraft(2026, 11, 22, 0, 0)
        assertEquals(30, draft.withDay(40).day)
        assertEquals(1, draft.withDay(-3).day)
        assertEquals(23, draft.withHour(99).hour)
        assertEquals(0, draft.withHour(-1).hour)
        assertEquals(59, draft.withMinute(120).minute)
        assertEquals(0, draft.withMinute(-5).minute)
    }

    @Test
    fun `月份越界被夹紧到一和十二`() {
        val draft = DateTimeDraft(2026, 6, 15, 8, 0)
        assertEquals(12, draft.withMonth(15).month)
        assertEquals(1, draft.withMonth(0).month)
    }

    @Test
    fun `月末跨月换算保留时分`() {
        val draft = DateTimeDraft(2026, 3, 31, 18, 45).withMonth(4)
        assertEquals(millisOf(2026, 4, 30, 18, 45), draft.toMillis(zone))
    }
}
