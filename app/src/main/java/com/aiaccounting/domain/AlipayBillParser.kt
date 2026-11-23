package com.aiaccounting.domain

import java.nio.charset.Charset
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 支付宝导出的账单（CSV，GBK 编码，邮件附件解压后得到）。
 *
 * 文件结构：几行说明 → 一行表头（第一格是「交易号」）→ 数据行 → 末尾分隔线。
 * 这里只收「交易成功」的支出/收入；关闭、退款、不计收支（如余额宝转入）都跳过，
 * 免得把调账、理财划转记成消费。
 */
object AlipayBillParser {

    data class BillRow(
        val occurredAt: Long,
        val amountCents: Long,
        val type: TxType,
        /** 交易对方，通常是商户名。 */
        val counterparty: String?,
        /** 商品名称。 */
        val description: String?,
        /** 交易号，用来做去重指纹。 */
        val tradeNo: String?,
    )

    private val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    /** 支付宝导出的 csv 是 GBK；一旦解出乱码就退回 UTF-8。 */
    fun decode(bytes: ByteArray): String {
        val gbkText = String(bytes, Charset.forName("GBK"))
        return if (gbkText.contains('\uFFFD')) String(bytes, Charsets.UTF_8) else gbkText
    }

    fun parse(text: String, zone: ZoneId = ZoneId.systemDefault()): List<BillRow> {
        if (text.isBlank()) return emptyList()
        val records = CsvCodec.parseNonBlank(text)
        val headerIndex = records.indexOfFirst { cells ->
            cells.firstOrNull()?.trim()?.removePrefix("\uFEFF") == "交易号"
        }
        require(headerIndex >= 0) { "这不是支付宝账单文件：没找到「交易号」表头" }

        val header = records[headerIndex].map { it.trim() }
        fun columnOf(vararg names: String): Int =
            names.firstNotNullOfOrNull { name -> header.indexOf(name).takeIf { it >= 0 } } ?: -1

        val timeCol = columnOf("付款时间", "交易创建时间")
        val amountCol = columnOf("金额（元）", "金额(元)")
        val directionCol = columnOf("收/支")
        val statusCol = columnOf("交易状态")
        val counterpartyCol = columnOf("交易对方")
        val descriptionCol = columnOf("商品名称")
        val tradeNoCol = columnOf("交易号")

        require(timeCol >= 0 && amountCol >= 0 && directionCol >= 0) {
            "账单表头缺少必需列（时间 / 金额 / 收支）"
        }

        val requiredMax = maxOf(timeCol, amountCol, directionCol)

        return records.drop(headerIndex + 1).mapNotNull { cells ->
            val cell = cells.map { it.trim() }
            if (cell.size <= requiredMax) return@mapNotNull null

            val type = when (cell[directionCol]) {
                "支出" -> TxType.EXPENSE
                "收入" -> TxType.INCOME
                else -> return@mapNotNull null
            }

            val status = cell.getOrNull(statusCol).orEmpty()
            if (status.contains("关闭") || status.contains("退款")) return@mapNotNull null
            if (!status.contains("成功")) return@mapNotNull null

            val amount = cell[amountCol].toDoubleOrNull() ?: return@mapNotNull null
            if (amount <= 0.0) return@mapNotNull null

            val occurredAt = runCatching {
                LocalDateTime.parse(cell[timeCol], TIME_FORMAT).atZone(zone).toInstant().toEpochMilli()
            }.getOrNull() ?: return@mapNotNull null

            BillRow(
                occurredAt = occurredAt,
                amountCents = Math.round(amount * 100),
                type = type,
                counterparty = cell.getOrNull(counterpartyCol)
                    ?.takeIf { it.isNotEmpty() && it != "/" && it != "-" },
                description = cell.getOrNull(descriptionCol)?.takeIf { it.isNotEmpty() },
                tradeNo = cell.getOrNull(tradeNoCol)?.takeIf { it.isNotEmpty() },
            )
        }
    }
}
