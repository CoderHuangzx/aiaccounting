package com.aiaccounting.domain

import com.aiaccounting.core.DateLabels
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** 首页按天分组后的一段。 */
data class DaySection(
    val date: LocalDate,
    val label: String,
    val expenseCents: Long,
    val incomeCents: Long,
    val items: List<Tx>,
)

/** 首页头部三个数字：今日支出、本月支出、本月收入。 */
data class HomeSummary(
    val todayExpenseCents: Long = 0,
    val monthExpenseCents: Long = 0,
    val monthIncomeCents: Long = 0,
    val monthLabel: String = "",
)

object HomeGrouper {

    fun group(txs: List<Tx>, today: LocalDate, zone: ZoneId): List<DaySection> =
        txs.groupBy { localDate(it.occurredAt, zone) }
            .toSortedMap(compareByDescending { it })
            .map { (date, items) ->
                DaySection(
                    date = date,
                    label = DateLabels.dayHeader(date, today),
                    expenseCents = items.sumOf { if (it.type == TxType.EXPENSE) it.amountCents else 0L },
                    incomeCents = items.sumOf { if (it.type == TxType.INCOME) it.amountCents else 0L },
                    items = items.sortedByDescending { it.occurredAt },
                )
            }

    fun summary(txs: List<Tx>, today: LocalDate, zone: ZoneId): HomeSummary {
        val monthStart = today.withDayOfMonth(1)
        var todayExpense = 0L
        var monthExpense = 0L
        var monthIncome = 0L
        for (tx in txs) {
            val date = localDate(tx.occurredAt, zone)
            if (date.isBefore(monthStart)) continue
            when (tx.type) {
                TxType.EXPENSE -> {
                    monthExpense += tx.amountCents
                    if (date == today) todayExpense += tx.amountCents
                }

                TxType.INCOME -> monthIncome += tx.amountCents
            }
        }
        return HomeSummary(
            todayExpenseCents = todayExpense,
            monthExpenseCents = monthExpense,
            monthIncomeCents = monthIncome,
            monthLabel = DateLabels.month(today),
        )
    }

    private fun localDate(millis: Long, zone: ZoneId): LocalDate =
        Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
}
