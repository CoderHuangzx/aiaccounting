package com.aiaccounting.domain

/**
 * 记账键盘的输入状态机。
 *
 * 支持「12+8」这类表达式：串内只允许数字、一个小数点（每段最多两位）和加/减连接符，
 * 求值时按「分」累加，避免浮点误差。
 */
object AmountInput {

    const val MAX_CHARS = 14
    private const val MAX_DECIMALS = 2

    /** 按键输入，key 取值：0-9 / "." / "+" / "-" / "⌫" / "C" */
    fun press(current: String, key: String): String {
        if (key == "C") return ""
        if (key == "⌫") return if (current.isEmpty()) current else current.dropLast(1)
        if (current.length >= MAX_CHARS) return current

        return when (key) {
            "." -> when {
                current.isEmpty() -> "0."
                current.last() == '+' || current.last() == '-' -> current + "0."
                lastSegment(current).contains('.') -> current
                else -> current + "."
            }

            "+", "-" -> when {
                current.isEmpty() -> current
                current.last() == '+' || current.last() == '-' -> current.dropLast(1) + key
                current.last() == '.' -> current.dropLast(1) + key
                else -> current + key
            }

            else -> {
                val segment = lastSegment(current)
                val dot = segment.indexOf('.')
                if (dot >= 0 && segment.length - dot - 1 >= MAX_DECIMALS) return current
                if (segment == "0") current.dropLast(1) + key else current + key
            }
        }
    }

    /**
     * 求值，返回「分」。空串、非法表达式、结果不为正数（记到 0 或负数没有意义）返回 null。
     */
    fun evaluate(expression: String): Long? {
        val cleaned = expression.trimEnd('+', '-', '.')
        if (cleaned.isEmpty()) return null

        var total = 0L
        var sign = 1
        var index = 0
        if (cleaned.startsWith("-")) {
            sign = -1
            index = 1
        }

        val buffer = StringBuilder()
        while (index < cleaned.length) {
            val ch = cleaned[index]
            if (ch == '+' || ch == '-') {
                val cents = segmentToCents(buffer.toString()) ?: return null
                total += sign * cents
                sign = if (ch == '+') 1 else -1
                buffer.setLength(0)
            } else {
                buffer.append(ch)
            }
            index++
        }
        val cents = segmentToCents(buffer.toString()) ?: return null
        total += sign * cents
        return if (total > 0) total else null
    }

    private fun lastSegment(expression: String): String =
        expression.substringAfterLast('+').substringAfterLast('-')

    private fun segmentToCents(raw: String): Long? {
        if (raw.isEmpty()) return null
        val value = raw.toDoubleOrNull() ?: return null
        if (value <= 0.0) return null
        return Math.round(value * 100)
    }
}
