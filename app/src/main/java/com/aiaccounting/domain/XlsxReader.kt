package com.aiaccounting.domain

import org.w3c.dom.Element
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory

/**
 * 极简 xlsx 读取器：只做「把第一张工作表读成二维字符串」这件事。
 *
 * 不引 Apache POI（几十 MB），账单文件都是小表，DOM 解析完全够用。
 * xlsx 本质是个 zip：sharedStrings.xml 存字符串池，worksheets/sheetN.xml 存单元格。
 */
object XlsxReader {

    fun readFirstSheet(bytes: ByteArray): List<List<String>> {
        var sharedStrings: List<String> = emptyList()
        var sheet: ByteArray? = null
        var fallbackSheet: ByteArray? = null

        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (entry.isDirectory) continue
                val data = zip.readBytes()
                when {
                    entry.name == "xl/sharedStrings.xml" -> sharedStrings = parseSharedStrings(data)

                    entry.name == "xl/worksheets/sheet1.xml" -> sheet = data

                    entry.name.startsWith("xl/worksheets/sheet") && entry.name.endsWith(".xml") ->
                        if (fallbackSheet == null) fallbackSheet = data
                }
            }
        }

        val content = sheet ?: fallbackSheet
            ?: error("不是有效的 xlsx 文件：找不到工作表")
        return parseSheet(content, sharedStrings)
    }

    /** 给出单元格坐标（如 "B12"）对应的 0 基列号。 */
    fun columnIndex(reference: String): Int {
        var value = 0
        for (ch in reference) {
            if (!ch.isLetter()) break
            value = value * 26 + (ch.uppercaseChar() - 'A' + 1)
        }
        return (value - 1).coerceAtLeast(0)
    }

    private fun parseSharedStrings(bytes: ByteArray): List<String> {
        val document = parseXml(bytes)
        val nodes = document.getElementsByTagNameNS("*", "si")
        return (0 until nodes.length).map { index ->
            val si = nodes.item(index) as Element
            val texts = si.getElementsByTagNameNS("*", "t")
            buildString {
                for (i in 0 until texts.length) append(texts.item(i).textContent)
            }
        }
    }

    private fun parseSheet(bytes: ByteArray, sharedStrings: List<String>): List<List<String>> {
        val document = parseXml(bytes)
        val rowNodes = document.getElementsByTagNameNS("*", "row")
        val rows = mutableListOf<List<String>>()

        for (i in 0 until rowNodes.length) {
            val rowElement = rowNodes.item(i) as Element
            val cells = mutableListOf<String>()

            val cellNodes = rowElement.getElementsByTagNameNS("*", "c")
            for (j in 0 until cellNodes.length) {
                val cell = cellNodes.item(j) as Element
                val column = columnIndex(cell.getAttribute("r"))
                while (cells.size <= column) cells.add("")
                cells[column] = cellValue(cell, sharedStrings)
            }
            rows.add(cells)
        }
        return rows
    }

    private fun cellValue(cell: Element, sharedStrings: List<String>): String {
        return when (cell.getAttribute("t")) {
            "s" -> {
                val index = childText(cell, "v").toIntOrNull() ?: return ""
                sharedStrings.getOrElse(index) { "" }
            }

            "inlineStr" -> childText(cell, "t")

            else -> childText(cell, "v")
        }
    }

    private fun childText(element: Element, localName: String): String {
        val children = element.childNodes
        for (i in 0 until children.length) {
            val node = children.item(i)
            if (node is Element && node.localName == localName) return node.textContent
        }
        return ""
    }

    private fun parseXml(bytes: ByteArray) =
        DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
            isExpandEntityReferences = false
        }.newDocumentBuilder().parse(ByteArrayInputStream(bytes))
}
