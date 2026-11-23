package com.aiaccounting.domain

import com.aiaccounting.core.Money

/**
 * 首页搜索的匹配规则。
 *
 * 搜得到的东西要符合直觉：分类名、商户、备注、账户名，以及金额本身
 * （输入 52.5 能找到 52.50 那笔）。纯函数，方便单元测试。
 */
object HomeFilter {

    fun apply(txs: List<Tx>, query: String): List<Tx> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return txs
        return txs.filter { matches(it, trimmed) }
    }

    fun matches(tx: Tx, query: String): Boolean {
        val haystack = buildString {
            append(tx.categoryName)
            append(' ')
            append(tx.accountName)
            tx.merchant?.let { append(' ').append(it) }
            tx.note?.let { append(' ').append(it) }
            append(' ')
            append(Money.plain(tx.amountCents))
            append(' ')
            append(Money.format(tx.amountCents))
        }
        return haystack.contains(query, ignoreCase = true)
    }
}
