package com.aiaccounting.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class RelativeTimeTest {

    private val now = 1_800_000_000_000L
    private val minute = 60_000L
    private val hour = 60 * minute
    private val day = 24 * hour

    @Test
    fun `一分钟内显示刚刚`() {
        assertEquals("刚刚", relativeTime(now - 30_000, now))
    }

    @Test
    fun `分钟级`() {
        assertEquals("5 分钟前", relativeTime(now - 5 * minute, now))
        assertEquals("59 分钟前", relativeTime(now - 59 * minute, now))
    }

    @Test
    fun `小时级`() {
        assertEquals("1 小时前", relativeTime(now - hour, now))
        assertEquals("23 小时前", relativeTime(now - 23 * hour, now))
    }

    @Test
    fun `天级`() {
        assertEquals("1 天前", relativeTime(now - day, now))
        assertEquals("29 天前", relativeTime(now - 29 * day, now))
    }

    @Test
    fun `月级`() {
        assertEquals("1 个月前", relativeTime(now - 40 * day, now))
    }
}
