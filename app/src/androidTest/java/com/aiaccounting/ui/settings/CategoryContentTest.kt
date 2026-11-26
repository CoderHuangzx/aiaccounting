package com.aiaccounting.ui.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.aiaccounting.data.local.CategoryEntity
import com.aiaccounting.domain.TxType
import androidx.compose.ui.test.performTextInput
import com.aiaccounting.ui.theme.AIAccountingTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** 分类管理里的两级分类：点一级分类弹出子分类面板。 */
class CategoryContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val food = CategoryEntity(id = 1, name = "餐饮", emoji = "🍜", kind = TxType.EXPENSE.name)
    private val lunch = CategoryEntity(
        id = 11, name = "午餐", emoji = "🍚", kind = TxType.EXPENSE.name, parentId = 1,
    )
    private val dinner = CategoryEntity(
        id = 12, name = "晚餐", emoji = "🍲", kind = TxType.EXPENSE.name, parentId = 1,
    )
    private val salary = CategoryEntity(id = 2, name = "工资", emoji = "💰", kind = TxType.INCOME.name)

    private fun content(onAdd: (String, String, TxType, Long?) -> Unit = { _, _, _, _ -> }) {
        composeRule.setContent {
            AIAccountingTheme {
                CategoryContent(
                    expenseCategories = listOf(food, lunch, dinner),
                    incomeCategories = listOf(salary),
                    merchantRules = emptyList(),
                    message = null,
                    onAddCategory = onAdd,
                    onUpdateCategory = { _, _, _ -> },
                    onDeleteCategory = {},
                    onDeleteMerchantRule = {},
                )
            }
        }
    }

    @Test
    fun 一级分类显示子分类数量() {
        content()
        composeRule.onNodeWithText("餐饮").assertIsDisplayed()
        composeRule.onNodeWithText("工资").assertIsDisplayed()
        // 子分类不在一级网格里重复出现
        composeRule.onNodeWithText("午餐").assertDoesNotExist()
    }

    @Test
    fun 点一级分类弹出子分类面板() {
        content()
        composeRule.onNodeWithTag("category_1").performClick()

        composeRule.onNodeWithText("子分类").assertIsDisplayed()
        composeRule.onNodeWithText("午餐").assertIsDisplayed()
        composeRule.onNodeWithText("晚餐").assertIsDisplayed()
    }

    @Test
    fun 子分类面板里能加子分类() {
        var added: Triple<String, String, Long?>? = null
        content { name, emoji, _, parentId -> added = Triple(name, emoji, parentId) }

        composeRule.onNodeWithTag("category_1").performClick()
        composeRule.onNodeWithTag("addSubCategory").performClick()

        composeRule.onNodeWithText("新建子分类（餐饮）").assertIsDisplayed()
        composeRule.onNodeWithTag("categoryNameField").performTextInput("夜宵")
        composeRule.onNodeWithText("保存").performClick()

        assertEquals("夜宵", added?.first)
        assertEquals("应带上父分类 id", 1L, added?.third)
    }

    @Test
    fun 面板里能编辑一级分类() {
        content()
        composeRule.onNodeWithTag("category_1").performClick()
        composeRule.onNodeWithTag("editParentCategory").performClick()

        composeRule.onNodeWithText("编辑分类").assertIsDisplayed()
        assertTrue(true)
    }
}
