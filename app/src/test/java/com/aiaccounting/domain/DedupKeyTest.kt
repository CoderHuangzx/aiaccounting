package com.aiaccounting.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DedupKeyTest {

    @Test
    fun `渠道单号取尾八位`() {
        val key = DedupKey.fromChannel("2026112200012345")
        assertEquals("ch:00012345", key)
    }

    @Test
    fun `单号太短视为无效`() {
        assertNull(DedupKey.fromChannel("12"))
        assertNull(DedupKey.fromChannel("   "))
    }

    @Test
    fun `同一时间窗内的兜底指纹一致`() {
        val window = 10 * 60 * 1000L
        val base = (1_700_000_000_000L / window) * window + 1_000L
        val a = DedupKey.fallback(2_500L, base, "瑞幸咖啡")
        val b = DedupKey.fallback(2_500L, base + 500_000L, "瑞幸咖啡")
        assertEquals(a, b)
    }

    @Test
    fun `金额或商户不同则指纹不同`() {
        val t = 1_700_000_000_000L
        assertNotEquals(
            DedupKey.fallback(2_500L, t, "瑞幸咖啡"),
            DedupKey.fallback(2_600L, t, "瑞幸咖啡"),
        )
        assertNotEquals(
            DedupKey.fallback(2_500L, t, "瑞幸咖啡"),
            DedupKey.fallback(2_500L, t, "星巴克"),
        )
    }

    @Test
    fun `指纹带 auto 前缀便于区分来源`() {
        assertTrue(DedupKey.fallback(1L, 1L, null).startsWith("auto:"))
    }
}
