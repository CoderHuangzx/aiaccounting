package com.aiaccounting.domain

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.floor

/**
 * 微信支付账单（xlsx）。
 *
 * 和支付宝不同，微信导出的时间是 **Excel 序列号**（如 46319.38），金额是纯数字，
 * 表头在第 18 行左右（前面全是说明）。所以要先按表头定位再逐行解析。
 *
 * 只收「支出 / 收入」，跳过「/」（充值、提现、理财通等中性交易）和退款/失败状态。
 */
object WeChatBillParser {

    private val DATE_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    /** Excel 序列号 1 = 1900-01-01，但 Excel 有个 1900 闰年 bug，所以基准取 1899-12-30。 */
    private val EXCEL_EPOCH: LocalDate = LocalDate.of(1899, 12, 30)

    private val SKIP_STATUS = listOf("退款", "失败", "关闭", "已撤销")

    fun parse(rows: List<List<String>>, zone: ZoneId = ZoneId.systemDefault()): List<BillRow> {
        val headerIndex = rows.indexOfFirst { row ->
            row.any { it.trim() == "交易时间" } && row.any { it.trim() == "收/支" }
        }
        require(headerIndex >= 0) { "这不是微信账单文件：没找到「交易时间 / 收/支」表头" }

        val header = rows[headerIndex].map { it.trim() }
        fun columnOf(vararg names: String): Int =
            names.firstNotNullOfOrNull { name -> header.indexOf(name).takeIf { it >= 0 } } ?: -1

        val timeCol = columnOf("交易时间")
        val directionCol = columnOf("收/支")
        val amountCol = columnOf("金额(元)", "金额（元）")
        val counterpartyCol = columnOf("交易对方")
        val productCol = columnOf("商品")
        val statusCol = columnOf("当前状态")
        val tradeNoCol = columnOf("交易单号")

        require(timeCol >= 0 && directionCol >= 0 && amountCol >= 0) {
            "账单表头缺少必需列（交易时间 / 收/支 / 金额）"
        }

        val requiredMax = maxOf(timeCol, directionCol, amountCol)

        return rows.drop(headerIndex + 1).mapNotNull { row ->
            if (row.size <= requiredMax) return@mapNotNull null

            val type = when (row[directionCol].trim()) {
                "支出" -> TxType.EXPENSE
                "收入" -> TxType.INCOME
                else -> return@mapNotNull null
            }

            val status = row.getOrNull(statusCol)?.trim().orEmpty()
            if (SKIP_STATUS.any { status.contains(it) }) return@mapNotNull null

            val amountCents = parseAmount(row[amountCol]) ?: return@mapNotNull null
            val occurredAt = parseTime(row[timeCol], zone) ?: return@mapNotNull null

            BillRow(
                occurredAt = occurredAt,
                amountCents = amountCents,
                type = type,
                counterparty = row.getOrNull(counterpartyCol)?.trim()?.takeIf { it.isNotEmpty() && it != "/" },
                description = row.getOrNull(productCol)?.trim()?.takeIf { it.isNotEmpty() && it != "/" },
                tradeNo = row.getOrNull(tradeNoCol)?.trim()?.takeIf { it.isNotEmpty() },
            )
        }
    }

    /**
     * Excel 序列号 → 毫秒。整数部分是天数，小数部分是当天的时间比例。
     * 微信账单声明所有时间均为 UTC+08:00，这里按传入时区解释。
     */
    fun excelSerialToMillis(serial: Double, zone: ZoneId = ZoneId.systemDefault()): Long {
        val days = floor(serial).toLong()
        val secondsOfDay = Math.round((serial - days) * 86_400.0)
        return EXCEL_EPOCH.plusDays(days)
            .atStartOfDay()
            .plusSeconds(secondsOfDay)
            .atZone(zone)
            .toInstant()
            .toEpochMilli()
    }

    private fun parseTime(raw: String, zone: ZoneId): Long? {
        val text = raw.trim()
        if (text.isEmpty()) return null
        // 正常是 Excel 序列号；万一是文本日期也兼容一下
        text.toDoubleOrNull()?.let { return excelSerialToMillis(it, zone) }
        return runCatching {
            LocalDateTime.parse(text, DATE_TIME).atZone(zone).toInstant().toEpochMilli()
        }.getOrNull()
    }

    private fun parseAmount(raw: String): Long? {
        val cleaned = raw.trim()
            .replace("¥", "")
            .replace("￥", "")
            .replace("元", "")
            .replace(",", "")
            .trim()
        if (cleaned.isEmpty()) return null
        val value = cleaned.toDoubleOrNull() ?: return null
        if (value <= 0.0) return null
        return Math.round(value * 100)
    }
}
