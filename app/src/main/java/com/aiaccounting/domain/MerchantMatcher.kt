package com.aiaccounting.domain

/**
 * 商户名 → 分类的匹配规则。
 *
 * 用户改过一次分类，就把「商户 → 分类」记下来；下次同商户进来直接命中。
 * 匹配顺序：先完全相等，再「商户名包含规则名」的最长匹配——
 * 这样「瑞幸咖啡（首蓿园店）」也能命中「瑞幸咖啡」这条规则。
 */
object MerchantMatcher {

    data class Rule(val pattern: String, val categoryId: Long)

    /** 归一化：去首尾空格、统一大小写、全角括号转半角、去掉空格。 */
    fun normalize(raw: String): String = raw.trim()
        .lowercase()
        .replace("（", "(")
        .replace("）", ")")
        .replace(" ", "")

    fun bestMatch(rules: List<Rule>, merchant: String?): Long? {
        val target = normalize(merchant.orEmpty())
        if (target.isEmpty() || rules.isEmpty()) return null

        val normalized = rules.map { it.copy(pattern = normalize(it.pattern)) }
            .filter { it.pattern.isNotEmpty() }

        normalized.firstOrNull { it.pattern == target }?.let { return it.categoryId }

        return normalized
            .filter { target.contains(it.pattern) }
            .maxByOrNull { it.pattern.length }
            ?.categoryId
    }
}
