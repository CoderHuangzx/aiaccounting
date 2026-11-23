package com.aiaccounting.domain

/**
 * 通用 CSV 读写小工具，备份文件和支付宝账单都用它。
 *
 * 解析是「整篇扫描」而不是先按行切分：引号内的换行属于字段内容，
 * 先按行切会把一条记录拆坏。
 */
object CsvCodec {

    fun escape(cell: String): String {
        val needsQuote = cell.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        return if (needsQuote) "\"" + cell.replace("\"", "\"\"") + "\"" else cell
    }

    fun encodeRow(cells: List<String>): String = cells.joinToString(",") { escape(it) }

    /**
     * 解析整篇 CSV，返回记录列表（每条记录是字段列表）。
     * 支持字段内逗号、换行，以及 "" 形式的引号转义。
     */
    fun parseRecords(text: String): List<List<String>> {
        val records = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val cell = StringBuilder()
        var inQuotes = false
        var index = 0

        fun flushRow() {
            row.add(cell.toString())
            cell.setLength(0)
            records.add(row)
            row = mutableListOf()
        }

        while (index < text.length) {
            val ch = text[index]
            when {
                inQuotes && ch == '"' && index + 1 < text.length && text[index + 1] == '"' -> {
                    cell.append('"')
                    index++
                }

                ch == '"' -> inQuotes = !inQuotes

                ch == ',' && !inQuotes -> {
                    row.add(cell.toString())
                    cell.setLength(0)
                }

                (ch == '\n' || ch == '\r') && !inQuotes -> {
                    if (ch == '\r' && index + 1 < text.length && text[index + 1] == '\n') index++
                    flushRow()
                }

                else -> cell.append(ch)
            }
            index++
        }
        if (cell.isNotEmpty() || row.isNotEmpty()) flushRow()
        return records
    }

    /** 只解析出非空记录，省得调用方到处判空行。 */
    fun parseNonBlank(text: String): List<List<String>> =
        parseRecords(text).filter { cells -> cells.any { it.isNotBlank() } }
}
