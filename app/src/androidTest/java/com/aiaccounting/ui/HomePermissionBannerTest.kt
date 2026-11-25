package com.aiaccounting.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assertIsDisplayed
import com.aiaccounting.ui.home.HomeScreen
import com.aiaccounting.ui.home.HomeUiState
import com.aiaccounting.ui.theme.AIAccountingTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** 首页顶部的「通知读取权限已关闭」红条。 */
class HomePermissionBannerTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun content(missing: Boolean, onClick: () -> Unit = {}) {
        composeRule.setContent {
            AIAccountingTheme {
                HomeScreen(
                    state = HomeUiState(),
                    notificationPermissionMissing = missing,
                    onOpenNotificationPermission = onClick,
                )
            }
        }
    }

    @Test
    fun 权限被关时首页显示红条并能点击跳转() {
        var clicked = false
        content(missing = true) { clicked = true }

        composeRule.onNodeWithTag("permissionWarning").assertIsDisplayed()
        composeRule.onNodeWithText("通知读取权限已关闭").assertIsDisplayed()

        composeRule.onNodeWithTag("permissionWarning").performClick()
        assertTrue("应触发去开启权限的回调", clicked)
    }

    @Test
    fun 权限正常时不显示红条() {
        content(missing = false)

        composeRule.onNodeWithTag("permissionWarning").assertDoesNotExist()
    }
}
