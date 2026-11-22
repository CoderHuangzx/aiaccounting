package com.aiaccounting.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aiaccounting.data.local.AccountEntity
import com.aiaccounting.data.local.CategoryEntity
import com.aiaccounting.domain.AmountInput
import com.aiaccounting.domain.TxType
import com.aiaccounting.ui.entry.EntryContent
import com.aiaccounting.ui.entry.EntryUiState
import com.aiaccounting.ui.theme.AIAccountingTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.ZoneId

/**
 * 记一笔面板的交互测试，跑在真机/模拟器上。
 *
 * 存在的意义：Paparazzi 只能验证「长什么样」，验证不了「点了有没有反应」。
 * 支出/收入切换漏接 clickable 就是这么漏出去的。
 */
@RunWith(AndroidJUnit4::class)
class EntryContentInteractionTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val expenseCategories = listOf(
        CategoryEntity(id = 1, name = "餐饮", emoji = "🍜", kind = "EXPENSE", sortOrder = 0),
        CategoryEntity(id = 2, name = "交通", emoji = "🚌", kind = "EXPENSE", sortOrder = 1),
    )

    private val incomeCategories = listOf(
        CategoryEntity(id = 7, name = "工资", emoji = "💰", kind = "INCOME", sortOrder = 0),
    )

    private val accounts = listOf(
        AccountEntity(id = 1, name = "微信", emoji = "💬", sortOrder = 0),
        AccountEntity(id = 2, name = "支付宝", emoji = "🔵", sortOrder = 1),
    )

    @Test
    fun 切换收入会触发回调并渲染收入分类() {
        var lastType: TxType? = null

        composeRule.setContent {
            var state by remember {
                mutableStateOf(
                    EntryUiState(
                        type = TxType.EXPENSE,
                        categories = expenseCategories,
                        categoryId = 1,
                        accounts = accounts,
                        accountId = 1,
                    ),
                )
            }
            AIAccountingTheme {
                EntryContent(
                    state = state,
                    onKey = {},
                    onType = { type ->
                        lastType = type
                        state = state.copy(
                            type = type,
                            categories = if (type == TxType.INCOME) incomeCategories else expenseCategories,
                            categoryId = if (type == TxType.INCOME) 7 else 1,
                        )
                    },
                    onCategory = {},
                    onAccount = {},
                    onNote = {},
                    onSave = {},
                )
            }
        }

        composeRule.onNodeWithText("工资").assertDoesNotExist()
        composeRule.onNodeWithText("收入").performClick()

        assertEquals(TxType.INCOME, lastType)
        composeRule.onNodeWithText("工资").assertIsDisplayed()
    }

    @Test
    fun 点击分类会回调对应的分类Id() {
        var selectedCategory: Long? = null

        composeRule.setContent {
            AIAccountingTheme {
                EntryContent(
                    state = EntryUiState(
                        type = TxType.EXPENSE,
                        categories = expenseCategories,
                        categoryId = 1,
                        accounts = accounts,
                        accountId = 1,
                    ),
                    onKey = {},
                    onType = {},
                    onCategory = { selectedCategory = it },
                    onAccount = {},
                    onNote = {},
                    onSave = {},
                )
            }
        }

        composeRule.onNodeWithText("交通").performClick()
        assertEquals(2L, selectedCategory)
    }

    @Test
    fun 键盘按键能累加金额且完成后可保存() {
        var saved = false

        composeRule.setContent {
            var state by remember {
                mutableStateOf(
                    EntryUiState(
                        type = TxType.EXPENSE,
                        categories = expenseCategories,
                        categoryId = 1,
                        accounts = accounts,
                        accountId = 1,
                    ),
                )
            }
            AIAccountingTheme {
                EntryContent(
                    state = state,
                    onKey = { key ->
                        val expression = AmountInput.press(state.expression, key)
                        state = state.copy(
                            expression = expression,
                            amountCents = AmountInput.evaluate(expression),
                        )
                    },
                    onType = {},
                    onCategory = {},
                    onAccount = {},
                    onNote = {},
                    onSave = { saved = true },
                )
            }
        }

        composeRule.onNodeWithText("12.00").assertDoesNotExist()
        composeRule.onNodeWithText("1").performClick()
        composeRule.onNodeWithText("2").performClick()
        composeRule.onNodeWithText("8").performClick()
        composeRule.onNodeWithText("128.00").assertIsDisplayed()

        composeRule.onNodeWithText("完成").performClick()
        assertTrue(saved)
    }

    @Test
    fun 金额为空时完成按钮不触发保存() {
        var saved = false

        composeRule.setContent {
            AIAccountingTheme {
                EntryContent(
                    state = EntryUiState(
                        type = TxType.EXPENSE,
                        categories = expenseCategories,
                        categoryId = 1,
                        accounts = accounts,
                        accountId = 1,
                    ),
                    onKey = {},
                    onType = {},
                    onCategory = {},
                    onAccount = {},
                    onNote = {},
                    onSave = { saved = true },
                )
            }
        }

        composeRule.onNodeWithText("完成").performClick()
        assertFalse(saved)
    }

    @Test
    fun 点击日期胶囊打开选择器并能确认时间() {
        val zone = ZoneId.of("Asia/Shanghai")
        val fixed = LocalDate.of(2026, 11, 20).atTime(9, 30)
            .atZone(zone).toInstant().toEpochMilli()
        var picked: Long? = null

        composeRule.setContent {
            AIAccountingTheme {
                EntryContent(
                    state = EntryUiState(
                        type = TxType.EXPENSE,
                        categories = expenseCategories,
                        categoryId = 1,
                        accounts = accounts,
                        accountId = 1,
                        occurredAt = fixed,
                    ),
                    onKey = {},
                    onType = {},
                    onCategory = {},
                    onAccount = {},
                    onNote = {},
                    onDateChange = { picked = it },
                    onSave = {},
                )
            }
        }

        composeRule.onNodeWithText("11月20日 09:30").assertIsDisplayed()
        composeRule.onNodeWithTag("entryDateChip").performClick()
        composeRule.onNodeWithText("编辑日期").assertIsDisplayed()

        // 不改动直接确认，应当回传同一个时间
        composeRule.onNodeWithText("确认").performClick()
        assertEquals(fixed, picked)
        composeRule.onNodeWithText("编辑日期").assertDoesNotExist()
    }

    @Test
    fun 日期选择器取消后不改动时间() {
        val zone = ZoneId.of("Asia/Shanghai")
        val fixed = LocalDate.of(2026, 11, 20).atTime(9, 30)
            .atZone(zone).toInstant().toEpochMilli()
        var picked: Long? = null

        composeRule.setContent {
            AIAccountingTheme {
                EntryContent(
                    state = EntryUiState(
                        type = TxType.EXPENSE,
                        categories = expenseCategories,
                        categoryId = 1,
                        accounts = accounts,
                        accountId = 1,
                        occurredAt = fixed,
                    ),
                    onKey = {},
                    onType = {},
                    onCategory = {},
                    onAccount = {},
                    onNote = {},
                    onDateChange = { picked = it },
                    onSave = {},
                )
            }
        }

        composeRule.onNodeWithTag("entryDateChip").performClick()
        composeRule.onNodeWithText("取消").performClick()

        composeRule.onNodeWithText("编辑日期").assertDoesNotExist()
        assertNull(picked)
    }
}
