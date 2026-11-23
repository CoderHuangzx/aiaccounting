package com.aiaccounting.domain

import com.aiaccounting.core.Money
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 备份文件（CSV）的编解码。
 *
 * 刻意用「分类名 / 账户名」而不是内部 id：导出的文件能直接用 Excel 打开看，
 * 换设备恢复时也不依赖某台机器上的自增 id。
 *
 * 纯 Kotlin，不碰 Android API，方便单元测试。
 */
object BackupCsv {

    const val HEADER = "日期,类型,金额,分类,账户,商户,备注,来源,指纹"

    private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    /** 一行备份数据。 */
    data class Row(
        val occurredAt: Long,
        val type: TxType,
        val amountCents: Long,
        val category: String,
        val account: String,
        val merchant: String?,
        val note: String?,
        val source: TxSource,
        val dedupKey: String?,
    )

    fun encode(rows: List<Row>, zone: ZoneId = ZoneId.systemDefault()): String = buildString {
        appendLine(HEADER)
        rows.forEach { row ->
            val date = Instant.ofEpochMilli(row.occurredAt).atZone(zone).format(DATE_FORMAT)
            val cells = listOf(
                date,
                when (row.type) {
                    TxType.EXPENSE -> "支出"
                    TxType.INCOME -> "收入"
                },
                Money.plain(row.amountCents),
                row.category,
                row.account,
                row.merchant.orEmpty(),
                row.note.orEmpty(),
                when (row.source) {
                    TxSource.MANUAL -> "手动"
                    TxSource.NOTIFICATION -> "通知"
                    TxSource.IMPORT -> "导入"
                },
                row.dedupKey.orEmpty(),
            )
            appendLine(CsvCodec.encodeRow(cells))
        }
    }

    /**
     * 解析备份文件。跳过空行；格式不对的行直接报错，避免「静默丢数据」。
     * @throws IllegalArgumentException 带行号说明
     */
    fun decode(text: String, zone: ZoneId = ZoneId.systemDefault()): List<Row> {
        val records = CsvCodec.parseNonBlank(text)
            .mapIndexed { index, cells -> index + 1 to cells }
        if (records.isEmpty()) return emptyList()

        val headerCells = records.first().second
        if (headerCells.firstOrNull()?.trim()?.removePrefix("\uFEFF") != "日期") {
            throw IllegalArgumentException("不是有效的备份文件：缺少表头")
        }

        return records.drop(1).map { (lineNo, cells) ->
            if (cells.size < 5) throw IllegalArgumentException("第 $lineNo 行列数不足")
            runCatching {
                Row(
                    occurredAt = LocalDateTime.parse(cells[0].trim(), DATE_FORMAT)
                        .atZone(zone).toInstant().toEpochMilli(),
                    type = when (cells[1].trim()) {
                        "支出", "EXPENSE" -> TxType.EXPENSE
                        "收入", "INCOME" -> TxType.INCOME
                        else -> throw IllegalArgumentException("未知类型 ${cells[1]}")
                    },
                    amountCents = parseAmount(cells[2].trim()),
                    category = cells[3].trim().ifBlank { "其他" },
                    account = cells[4].trim().ifBlank { "现金" },
                    merchant = cells.getOrNull(5)?.trim()?.takeIf { it.isNotEmpty() },
                    note = cells.getOrNull(6)?.trim()?.takeIf { it.isNotEmpty() },
                    source = when (cells.getOrNull(7)?.trim()) {
                        "通知", "NOTIFICATION" -> TxSource.NOTIFICATION
                        "导入", "IMPORT" -> TxSource.IMPORT
                        else -> TxSource.MANUAL
                    },
                    dedupKey = cells.getOrNull(8)?.trim()?.takeIf { it.isNotEmpty() },
                )
            }.getOrElse { error ->
                throw IllegalArgumentException("第 $lineNo 行解析失败：${error.message}")
            }
        }
    }

    private fun parseAmount(raw: String): Long {
        val value = raw.toDoubleOrNull() ?: throw IllegalArgumentException("金额格式不对：$raw")
        return Math.round(value * 100)
    }
}
