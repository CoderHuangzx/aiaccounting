package com.aiaccounting.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class ReportBucket(val label: String, val cents: Long)

data class CategorySlice(
    val name: String,
    val emoji: String,
    val cents: Long,
    val ratio: Float,
)

data class ReportData(
    val expenseCents: Long = 0,
    val incomeCents: Long = 0,
    val netCents: Long = 0,
    val averagePerDayCents: Long = 0,
    val previousExpenseCents: Long = 0,
    val changeRatio: Float = 0f,
    val buckets: List<ReportBucket> = emptyList(),
    val categories: List<CategorySlice> = emptyList(),
) {
    val hasData: Boolean get() = expenseCents > 0 || incomeCents > 0
}

/**
 * 报表聚合。全部为纯函数，便于单元测试。
 * 数据量在自用级别（万级以内）时，内存聚合足够，也让统计逻辑可测。
 */
object ReportCalculator {

    fun build(
        txs: List<Tx>,
        range: PeriodRange,
        today: LocalDate,
        zone: ZoneId,
    ): ReportData {
        val current = txs.filter { range.contains(it.localDate(zone)) }
        val previous = txs.filter { range.containsPrevious(it.localDate(zone)) }

        val expense = current.expenseSum()
        val income = current.incomeSum()
        val previousExpense = previous.expenseSum()

        val elapsedDays = when {
            today.isBefore(range.start) -> 1
            today.isAfter(range.end) -> range.days
            else -> (today.toEpochDay() - range.start.toEpochDay() + 1).toInt().coerceAtLeast(1)
        }

        val buckets = range.buckets.map { bucket ->
            val sum = current.sumOf { tx ->
                val date = tx.localDate(zone)
                if (tx.type == TxType.EXPENSE && !date.isBefore(bucket.start) && !date.isAfter(bucket.end)) {
                    tx.amountCents
                } else {
                    0L
                }
            }
            ReportBucket(bucket.label, sum)
        }

        val categories = current
            .filter { it.type == TxType.EXPENSE }
            .groupBy { it.categoryId }
            .map { (_, items) ->
                CategorySlice(
                    name = items.first().categoryName,
                    emoji = items.first().categoryEmoji,
                    cents = items.sumOf { it.amountCents },
                    ratio = if (expense == 0L) 0f else items.sumOf { it.amountCents }.toFloat() / expense,
                )
            }
            .sortedByDescending { it.cents }
            .take(8)

        return ReportData(
            expenseCents = expense,
            incomeCents = income,
            netCents = income - expense,
            averagePerDayCents = expense / elapsedDays,
            previousExpenseCents = previousExpense,
            changeRatio = if (previousExpense == 0L) 0f else (expense - previousExpense).toFloat() / previousExpense,
            buckets = buckets,
            categories = categories,
        )
    }

    private fun List<Tx>.expenseSum(): Long = sumOf { if (it.type == TxType.EXPENSE) it.amountCents else 0L }

    private fun List<Tx>.incomeSum(): Long = sumOf { if (it.type == TxType.INCOME) it.amountCents else 0L }

    private fun Tx.localDate(zone: ZoneId): LocalDate =
        Instant.ofEpochMilli(occurredAt).atZone(zone).toLocalDate()
}
