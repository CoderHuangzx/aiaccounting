package com.aiaccounting.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class XlsxReaderTest {

    /** 单元格文本以 # 开头表示数字，否则进共享字符串池。 */
    private fun buildXlsx(rows: List<List<String>>): ByteArray {
        val shared = mutableListOf<String>()
        fun sharedIndex(value: String): Int {
            val existing = shared.indexOf(value)
            if (existing >= 0) return existing
            shared.add(value)
            return shared.size - 1
        }

        fun columnName(index: Int): String {
            var value = index + 1
            val name = StringBuilder()
            while (value > 0) {
                val remainder = (value - 1) % 26
                name.insert(0, ('A' + remainder))
                value = (value - 1) / 26
            }
            return name.toString()
        }

        // 先生成 sheet 内容，过程中顺手填充共享字符串池
        val sheetXml = buildString {
            append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
            append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>""")
            rows.forEachIndexed { rowIndex, cells ->
                append("""<row r="${rowIndex + 1}">""")
                cells.forEachIndexed { columnIndex, value ->
                    val ref = columnName(columnIndex) + (rowIndex + 1)
                    if (value.startsWith("#")) {
                        append("""<c r="$ref"><v>${value.substring(1)}</v></c>""")
                    } else {
                        append("""<c r="$ref" t="s"><v>${sharedIndex(value)}</v></c>""")
                    }
                }
                append("</row>")
            }
            append("</sheetData></worksheet>")
        }

        val sharedStringsXml = buildString {
            append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
            append("""<sst xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")
            shared.forEach { append("<si><t>").append(it).append("</t></si>") }
            append("</sst>")
        }

        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            fun put(name: String, content: String) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(content.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
            put("xl/sharedStrings.xml", sharedStringsXml)
            put("xl/worksheets/sheet1.xml", sheetXml)
        }
        return out.toByteArray()
    }

    @Test
    fun `能读出字符串与数字单元格`() {
        val bytes = buildXlsx(
            listOf(
                listOf("交易时间", "交易对方", "金额"),
                listOf("#46319.38", "瑞幸咖啡", "#2.72"),
            ),
        )

        val rows = XlsxReader.readFirstSheet(bytes)
        assertEquals(2, rows.size)
        assertEquals("交易时间", rows[0][0])
        assertEquals("瑞幸咖啡", rows[1][1])
        assertEquals("46319.38", rows[1][0])
        assertEquals("2.72", rows[1][2])
    }

    @Test
    fun `空单元格会把列补空`() {
        val bytes = buildXlsx(
            listOf(
                listOf("A", "", "C"),
            ),
        )
        val rows = XlsxReader.readFirstSheet(bytes)
        assertEquals(listOf("A", "", "C"), rows.first())
    }

    @Test
    fun `列号换算`() {
        assertEquals(0, XlsxReader.columnIndex("A1"))
        assertEquals(10, XlsxReader.columnIndex("K18"))
        assertEquals(26, XlsxReader.columnIndex("AA1"))
    }

    @Test
    fun `不是 xlsx 时报错`() {
        assertThrows(IllegalStateException::class.java) {
            XlsxReader.readFirstSheet("not a zip".toByteArray())
        }
    }

    @Test
    fun `没有 worksheet 时报错`() {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            zip.putNextEntry(ZipEntry("xl/sharedStrings.xml"))
            zip.write("<sst/>".toByteArray())
            zip.closeEntry()
        }
        val error = assertThrows(IllegalStateException::class.java) {
            XlsxReader.readFirstSheet(out.toByteArray())
        }
        assertTrue(error.message!!.contains("工作表"))
    }
}
