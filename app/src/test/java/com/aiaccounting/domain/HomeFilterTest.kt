package com.aiaccounting.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeFilterTest {

    private fun tx(
        id: Long,
        category: String = "餐饮",
        merchant: String? = null,
        note: String? = null,
        account: String = "微信",
        cents: Long = 5250,
        type: TxType = TxType.EXPENSE,
    ) = Tx(
        id = id,
        type = type,
        amountCents = cents,
        occurredAt = 1_700_000_000_000,
        categoryId = 1,
        categoryName = category,
        categoryEmoji = "🍜",
        accountId = 1,
        accountName = account,
        merchant = merchant,
        note = note,
    )

    private val samples = listOf(
        tx(1, category = "餐饮", merchant = "瑞幸咖啡"),
        tx(2, category = "交通", merchant = "滴滴出行"),
        tx(3, category = "购物", merchant = "京东", note = "买键盘"),
        tx(4, category = "餐饮", merchant = "星巴克", cents = 3500),
    )

    @Test
    fun `空查询返回全部`() {
        assertEquals(4, HomeFilter.apply(samples, "").size)
        assertEquals(4, HomeFilter.apply(samples, "   ").size)
    }

    @Test
    fun `按分类名搜`() {
        val result = HomeFilter.apply(samples, "餐饮")
        assertEquals(2, result.size)
        assertTrue(result.all { it.categoryName == "餐饮" })
    }

    @Test
    fun `按商户搜且忽略大小写`() {
        val withAscii = samples + tx(9, merchant = "Starbucks")

        assertEquals(1, HomeFilter.apply(samples, "瑞幸").size)
        assertEquals(1, HomeFilter.apply(withAscii, "starbucks").size)
        assertEquals(1, HomeFilter.apply(withAscii, "STARBUCKS").size)
    }

    @Test
    fun `按备注搜`() {
        val result = HomeFilter.apply(samples, "键盘")
        assertEquals(1, result.size)
        assertEquals(3L, result.first().id)
    }

    @Test
    fun `按账户名搜`() {
        val others = samples + tx(5, merchant = "现金支付", account = "现金")
        assertEquals(1, HomeFilter.apply(others, "现金").size)
    }

    @Test
    fun `按金额搜`() {
        assertEquals(1, HomeFilter.apply(samples, "35.00").size)
        assertEquals(1, HomeFilter.apply(samples, "35").size)
    }

    @Test
    fun `搜不到时返回空`() {
        assertTrue(HomeFilter.apply(samples, "不存在的词").isEmpty())
    }

    @Test
    fun `查询前后的空格被忽略`() {
        assertEquals(1, HomeFilter.apply(samples, "  瑞幸  ").size)
    }
}
