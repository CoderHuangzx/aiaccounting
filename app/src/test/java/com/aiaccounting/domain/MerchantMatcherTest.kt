package com.aiaccounting.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MerchantMatcherTest {

    private val rules = listOf(
        MerchantMatcher.Rule("瑞幸咖啡", 1),
        MerchantMatcher.Rule("首蓿园光华路店", 2),
        MerchantMatcher.Rule("Starbucks", 3),
    )

    @Test
    fun `完全相等优先`() {
        assertEquals(1L, MerchantMatcher.bestMatch(rules, "瑞幸咖啡"))
    }

    @Test
    fun `包含匹配取最长的那条`() {
        // 「瑞幸咖啡」和「首蓿园光华路店」都在里面，后者更长应胜出
        assertEquals(2L, MerchantMatcher.bestMatch(rules, "瑞幸咖啡（首蓿园光华路店）"))
    }

    @Test
    fun `大小写与空格不影响匹配`() {
        assertEquals(3L, MerchantMatcher.bestMatch(rules, " STARBUCKS "))
        assertEquals(3L, MerchantMatcher.bestMatch(rules, "StarBucks 星巴克"))
    }

    @Test
    fun `没有命中返回 null`() {
        assertNull(MerchantMatcher.bestMatch(rules, "盒马鲜生"))
    }

    @Test
    fun `商户为空或规则为空时返回 null`() {
        assertNull(MerchantMatcher.bestMatch(rules, null))
        assertNull(MerchantMatcher.bestMatch(rules, "   "))
        assertNull(MerchantMatcher.bestMatch(emptyList(), "瑞幸咖啡"))
    }

    @Test
    fun `归一化会统一括号与空格`() {
        assertEquals("瑞幸咖啡(首蓿园光华路店)", MerchantMatcher.normalize(" 瑞幸咖啡 （首蓿园光华路店） "))
        assertEquals("abc", MerchantMatcher.normalize("A B C"))
    }
}
