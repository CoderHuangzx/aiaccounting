package com.aiaccounting.core

import kotlin.math.abs

/**
 * 金额统一用「分」(Long) 表示，避免浮点误差。
 * 所有格式化都不依赖 Locale，保证测试与真机输出一致。
 */
object Money {

    /** 123456 -> "1,234.56"；-1250 -> "-12.50" */
    fun format(cents: Long): String {
        val negative = cents < 0
        val absolute = abs(cents)
        val yuan = absolute / 100
        val fen = absolute % 100
        val grouped = yuan.toString()
            .reversed()
            .chunked(3)
            .joinToString(",")
            .reversed()
        return buildString {
            if (negative) append('-')
            append(grouped)
            append('.')
            append(fen.toString().padStart(2, '0'))
        }
    }

    /** 123456 -> "¥1,234.56" */
    fun formatWithSymbol(cents: Long): String = "¥" + format(cents)

    /** 不带千分位，用于输入框回显：123456 -> "1234.56" */
    fun plain(cents: Long): String {
        val negative = cents < 0
        val absolute = abs(cents)
        return buildString {
            if (negative) append('-')
            append(absolute / 100)
            append('.')
            append((absolute % 100).toString().padStart(2, '0'))
        }
    }
}
