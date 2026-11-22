package com.aiaccounting.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AmountInputTest {

    // ── 按键 ────────────────────────────────────────────────────────────

    @Test
    fun `连续数字拼接`() {
        var value = ""
        listOf("1", "2", "3").forEach { value = AmountInput.press(value, it) }
        assertEquals("123", value)
    }

    @Test
    fun `小数点只能出现一次`() {
        assertEquals("12.", AmountInput.press("12", "."))
        assertEquals("12.", AmountInput.press("12.", "."))
        assertEquals("0.", AmountInput.press("", "."))
    }

    @Test
    fun `每段最多两位小数`() {
        assertEquals("12.55", AmountInput.press("12.5", "5"))
        assertEquals("12.55", AmountInput.press("12.55", "5"))
    }

    @Test
    fun `运算符替换与去尾`() {
        assertEquals("12+", AmountInput.press("12", "+"))
        assertEquals("12-", AmountInput.press("12+", "-"))
        assertEquals("12+", AmountInput.press("12.", "+"))
        assertEquals("12", AmountInput.press("12+", "⌫"))
    }

    @Test
    fun `前导零被替换`() {
        assertEquals("5", AmountInput.press("0", "5"))
        assertEquals("1+5", AmountInput.press("1+0", "5"))
    }

    @Test
    fun `清空`() {
        assertEquals("", AmountInput.press("12+8", "C"))
    }

    // ── 求值 ────────────────────────────────────────────────────────────

    @Test
    fun `普通金额换算成分`() {
        assertEquals(1200L, AmountInput.evaluate("12"))
        assertEquals(1250L, AmountInput.evaluate("12.5"))
        assertEquals(1L, AmountInput.evaluate("0.01"))
    }

    @Test
    fun `表达式求和`() {
        assertEquals(2000L, AmountInput.evaluate("12+8"))
        assertEquals(7000L, AmountInput.evaluate("100-30"))
        assertEquals(1250L, AmountInput.evaluate("12.5-"))
        assertEquals(1000L, AmountInput.evaluate("10+"))
    }

    @Test
    fun `非法输入返回 null`() {
        assertNull(AmountInput.evaluate(""))
        assertNull(AmountInput.evaluate("+"))
        assertNull(AmountInput.evaluate("0"))
        assertNull(AmountInput.evaluate("3-5"))
        assertNull(AmountInput.evaluate("abc"))
    }
}
