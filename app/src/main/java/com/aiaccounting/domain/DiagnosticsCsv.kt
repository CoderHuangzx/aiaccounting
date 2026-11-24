package com.aiaccounting.domain

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 诊断数据的 CSV 编解码：商户规则 + 捕捉日志。
 *
 * 为什么跟账本备份分开：
 * - 账本 CSV 是「钱」的记录，要能直接给 Excel 看、能恢复；
 * - 商户规则是配置，值得云备份（丢了得重新学习），但不该混进账本；
 * - 捕捉日志是调试用的原文，含完整通知内容（金额、商户，偶尔带人名），
 *   只在你主动导出排查时才写出来，不进自动备份。
 */
object DiagnosticsCsv {

    private val TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    data class RuleRow(val merchant: String, val categoryName: String, val hitCount: Int)

    data class CaptureRow(
        val capturedAt: Long,
        val appLabel: String,
        val packageName: String,
        val title: String?,
        val text: String?,
        val parsed: Boolean,
        val reason: String?,
    )

    private val RULE_HEADER = listOf("商户", "分类", "命中次数")
    private val CAPTURE_HEADER = listOf("时间", "应用", "包名", "标题", "正文", "已解析", "原因")

    fun encodeRules(rows: List<RuleRow>): String = buildString {
        appendLine(CsvCodec.encodeRow(RULE_HEADER))
        rows.forEach { row ->
            appendLine(CsvCodec.encodeRow(listOf(row.merchant, row.categoryName, row.hitCount.toString())))
        }
    }

    fun decodeRules(text: String): List<RuleRow> {
        val records = CsvCodec.parseNonBlank(text)
        if (records.isEmpty()) return emptyList()
        val header = records.first().map { it.trim() }
        require(header.firstOrNull() == RULE_HEADER.first()) {
            "这不是商户规则文件：表头应为「商户,分类,命中次数」"
        }
        return records.drop(1).mapNotNull { cells ->
            val merchant = cells.getOrNull(0)?.trim().orEmpty()
            val category = cells.getOrNull(1)?.trim().orEmpty()
            if (merchant.isEmpty() || category.isEmpty()) return@mapNotNull null
            RuleRow(
                merchant = merchant,
                categoryName = category,
                hitCount = cells.getOrNull(2)?.trim()?.toIntOrNull() ?: 1,
            )
        }
    }

    fun encodeCaptures(rows: List<CaptureRow>, zone: ZoneId = ZoneId.systemDefault()): String = buildString {
        appendLine(CsvCodec.encodeRow(CAPTURE_HEADER))
        rows.forEach { row ->
            appendLine(
                CsvCodec.encodeRow(
                    listOf(
                        TIME.format(Instant.ofEpochMilli(row.capturedAt).atZone(zone)),
                        row.appLabel,
                        row.packageName,
                        row.title.orEmpty(),
                        row.text.orEmpty(),
                        if (row.parsed) "是" else "否",
                        row.reason.orEmpty(),
                    ),
                ),
            )
        }
    }
}
