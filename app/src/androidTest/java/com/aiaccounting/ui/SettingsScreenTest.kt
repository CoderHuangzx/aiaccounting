package com.aiaccounting.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aiaccounting.ui.settings.SettingsScreen
import com.aiaccounting.ui.theme.AIAccountingTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var exported = false
    private var importedAlipay = false
    private var importedWeChat = false
    private var importedBackup = false
    private var openedWebDav = false
    private var openedInbox = false
    private var backClicked = false
    private var healthRefreshed = false
    private var toggledSource: Pair<String, Boolean>? = null
    private var allSourcesToggled: Boolean? = null
    private var addedCategory: Triple<String, String, com.aiaccounting.domain.TxType>? = null

    private fun content() {
        composeRule.setContent {
            AIAccountingTheme {
                SettingsScreen(
                    recordCount = 12,
                    pendingCount = 3,
                    webDavConfigured = false,
                    versionName = "0.1.1",
                    message = null,
                    onBack = { backClicked = true },
                    onExport = { exported = true },
                    onImportBackup = { importedBackup = true },
                    onImportAlipay = { importedAlipay = true },
                    onImportWeChat = { importedWeChat = true },
                    onOpenWebDav = { openedWebDav = true },
                    onUploadCloud = {},
                    onDownloadCloud = {},
                    onInbox = { openedInbox = true },
                    onNotificationPermission = {},
                    onCaptureLog = {},
                    onOpenAccessibility = {},
                    health = com.aiaccounting.data.HealthSnapshot(
                        notificationListenerEnabled = true,
                        accessibilityEnabled = true,
                        overlayGranted = true,
                        batteryUnrestricted = false,
                        lastCaptureAt = null,
                        lastBackupAt = null,
                    ),
                    onOpenOverlayPermission = {},
                    onRequestBatteryWhitelist = {},
                    onRefreshHealth = { healthRefreshed = true },
                    sourceStates = com.aiaccounting.data.AutoSource.entries.associateWith { true },
                    onToggleSource = { source, enabled -> toggledSource = source.key to enabled },
                    onToggleAllSources = { allSourcesToggled = it },
                    categoryState = com.aiaccounting.ui.settings.CategoryPageState(),
                    onAddCategory = { name, emoji, kind -> addedCategory = Triple(name, emoji, kind) },
                    onUpdateCategory = { _, _, _ -> },
                    onDeleteCategory = {},
                    onDeleteMerchantRule = {},
                    crashLog = com.aiaccounting.ui.settings.CrashLogState(),
                    onOpenCrash = {},
                    onCloseCrashDetail = {},
                    onCopyCrash = {},
                    onClearCrashes = {},                )
            }
        }
    }

    @Test
    fun 主页只保留三个入口() {
        content()

        composeRule.onNodeWithText("自动记账").assertIsDisplayed()
        composeRule.onNodeWithText("导入数据").assertIsDisplayed()
        composeRule.onNodeWithText("导出数据").assertIsDisplayed()
        composeRule.onNodeWithText("AI 记账 v0.1.1").assertIsDisplayed()
    }

    @Test
    fun 导入页能触发支付宝与微信导入() {
        content()

        composeRule.onNodeWithText("导入数据").performClick()
        composeRule.onNodeWithText("支付宝账单").assertIsDisplayed()
        composeRule.onNodeWithText("微信账单").assertIsDisplayed()
        composeRule.onNodeWithText("恢复备份文件").assertIsDisplayed()

        composeRule.onNodeWithText("支付宝账单").performClick()
        assertTrue(importedAlipay)

        composeRule.onNodeWithText("微信账单").performClick()
        assertTrue(importedWeChat)
    }

    @Test
    fun 设置里能进分类管理并新建() {
        content()

        composeRule.onNodeWithText("记账分类管理").performClick()
        composeRule.onNodeWithText("支出管理").assertIsDisplayed()
        composeRule.onNodeWithText("收入管理").assertIsDisplayed()

        composeRule.onNodeWithTag("addExpenseCategory").performClick()
        composeRule.onNodeWithText("新建分类").assertIsDisplayed()
        composeRule.onNodeWithText("图标").assertIsDisplayed()
    }

    @Test
    fun 导入页能打开教程并返回() {
        content()

        composeRule.onNodeWithText("导入数据").performClick()
        composeRule.onNodeWithTag("tutorialAlipay").performClick()
        composeRule.onNodeWithText("支付宝账单导入教程").assertIsDisplayed()
        composeRule.onNodeWithText("选文件导入").assertIsDisplayed()

        composeRule.onNodeWithTag("settingsBack").performClick()
        composeRule.onNodeWithText("导入数据").assertIsDisplayed()   // 回到导入页标题所在列表
    }

    @Test
    fun 导出页能导出与进云备份设置() {
        content()

        composeRule.onNodeWithText("导出数据").performClick()
        composeRule.onNodeWithText("导出 CSV 文件").assertIsDisplayed()
        composeRule.onNodeWithText("导出 CSV 文件").performClick()
        assertTrue(exported)

        composeRule.onNodeWithText("云备份设置（WebDAV）").performClick()
        assertTrue(openedWebDav)
    }

    @Test
    fun 自动记账页能进收件箱() {
        content()

        composeRule.onNodeWithText("自动记账").performClick()
        composeRule.onNodeWithText("待确认收件箱").assertIsDisplayed()
        composeRule.onNodeWithText("通知读取权限").assertIsDisplayed()
        composeRule.onNodeWithText("捕捉日志").assertIsDisplayed()

        composeRule.onNodeWithText("待确认收件箱").performClick()
        assertTrue(openedInbox)
    }

    @Test
    fun 自动记账页能进运行状态并显示红绿灯() {
        content()

        composeRule.onNodeWithText("自动记账").performClick()
        composeRule.onNodeWithText("运行状态").assertIsDisplayed()
        composeRule.onNodeWithText("运行状态").performClick()

        // 电池白名单是唯一未就绪项 → 应提示 1 项未就绪
        composeRule.onNodeWithText("有 1 项未就绪，自动记账可能悄悄失效").assertIsDisplayed()
        composeRule.onNodeWithTag("healthNotification").assertIsDisplayed()
        composeRule.onNodeWithTag("healthAccessibility").assertIsDisplayed()
        composeRule.onNodeWithTag("healthBattery").assertIsDisplayed()
        composeRule.onNodeWithText("重新检测").assertIsDisplayed()

        composeRule.onNodeWithText("重新检测").performClick()
        assertTrue(healthRefreshed)
    }


    @Test
    fun 主页返回会回调() {
        content()
        composeRule.onNodeWithTag("settingsBack").performClick()
        assertTrue(backClicked)
    }
}
