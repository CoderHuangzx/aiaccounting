package com.aiaccounting.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aiaccounting.ui.settings.SettingsScreen
import com.aiaccounting.ui.theme.AIAccountingTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun 设置页展示数据入口并回调() {
        var exported = false
        var imported = false
        var backClicked = false

        composeRule.setContent {
            AIAccountingTheme {
                SettingsScreen(
                    recordCount = 12,
                    pendingCount = 3,
                    webDavConfigured = false,
                    versionName = "0.2.0",
                    message = null,
                    onBack = { backClicked = true },
                    onExport = { exported = true },
                    onImport = { imported = true },
                    onImportBill = {},
                    onImportWeChat = {},
                    onWebDav = {},
                    onInbox = {},
                    onNotificationPermission = {},
                    onCaptureLog = {},
                )
            }
        }

        composeRule.onNodeWithText("导出数据").assertIsDisplayed()
        composeRule.onNodeWithText("导入数据").assertIsDisplayed()
        composeRule.onNodeWithText("云备份（WebDAV）").assertIsDisplayed()
        composeRule.onNodeWithText("待确认收件箱").assertIsDisplayed()
        composeRule.onNodeWithText("AI 记账 v0.2.0").assertIsDisplayed()

        composeRule.onNodeWithText("导出数据").performClick()
        assertTrue(exported)

        composeRule.onNodeWithText("导入数据").performClick()
        assertTrue(imported)

        composeRule.onNodeWithTag("settingsBack").performClick()
        assertTrue(backClicked)
    }
}
