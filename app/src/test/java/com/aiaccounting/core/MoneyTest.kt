package com.aiaccounting.core

import org.junit.Assert.assertEquals
import org.junit.Test

class MoneyTest {

    @Test
    fun `format 千分位与两位小数`() {
        assertEquals("0.00", Money.format(0))
        assertEquals("0.05", Money.format(5))
        assertEquals("12.50", Money.format(1250))
        assertEquals("1,234.56", Money.format(123456))
        assertEquals("1,000,000.00", Money.format(100_000_000))
    }

    @Test
    fun `format 负数`() {
        assertEquals("-12.50", Money.format(-1250))
        assertEquals("-0.05", Money.format(-5))
    }

    @Test
    fun `plain 不带千分位`() {
        assertEquals("1234.56", Money.plain(123456))
        assertEquals("0.00", Money.plain(0))
        assertEquals("-12.50", Money.plain(-1250))
    }

    @Test
    fun `formatWithSymbol 前缀`() {
        assertEquals("¥52.00", Money.formatWithSymbol(5200))
    }
}
