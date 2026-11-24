package com.aiaccounting.ui

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import com.aiaccounting.data.HealthSnapshot
import com.aiaccounting.data.local.AccountEntity
import com.aiaccounting.data.local.CategoryEntity
import com.aiaccounting.domain.HomeGrouper
import com.aiaccounting.domain.PeriodUnit
import com.aiaccounting.domain.ReportCalculator
import com.aiaccounting.domain.ReportPeriods
import com.aiaccounting.domain.Tx
import com.aiaccounting.domain.TxSource
import com.aiaccounting.domain.TxType
import com.aiaccounting.ui.components.HomeBottomBar
import com.aiaccounting.ui.entry.EntryContent
import com.aiaccounting.ui.entry.EntryUiState
import com.aiaccounting.ui.settings.HealthContent
import com.aiaccounting.ui.entry.DatePickerPanel
import com.aiaccounting.ui.settings.SettingsScreen
import com.aiaccounting.ui.home.HomeScreen
import com.aiaccounting.ui.home.HomeUiState
import com.aiaccounting.ui.report.ReportScreen
import com.aiaccounting.ui.report.ReportUiState
import com.aiaccounting.ui.theme.AIAccountingTheme
import com.aiaccounting.ui.theme.PageBg
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/**
 * 用 layoutlib 在 JVM 上把界面渲染成 PNG，不需要模拟器。
 * 产物：app/build/reports/paparazzi/debug/images/
 */
class ScreenshotTest {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        theme = "Theme.AIAccounting",
        showSystemUi = false,
    )

    private val zone: ZoneId = ZoneId.of("Asia/Shanghai")
    private val today = LocalDate.of(2026, 11, 22)

    private fun tx(
        id: Long,
        type: TxType,
        cents: Long,
        date: LocalDate,
        hour: Int,
        minute: Int,
        categoryId: Long,
        categoryName: String,
        emoji: String,
        merchant: String? = null,
        note: String? = null,
        source: TxSource = TxSource.MANUAL,
        account: String = "微信",
    ): Tx = Tx(
        id = id,
        type = type,
        amountCents = cents,
        occurredAt = date.atTime(hour, minute).atZone(zone).toInstant().toEpochMilli(),
        categoryId = categoryId,
        categoryName = categoryName,
        categoryEmoji = emoji,
        accountId = 1L,
        accountName = account,
        merchant = merchant,
        note = note,
        source = source,
    )

    private val samples: List<Tx> = listOf(
        tx(1, TxType.EXPENSE, 2_750, today, 17, 42, 1, "餐饮", "🍜", "小厨娘淮扬菜（首蓿园光华路店）"),
        tx(2, TxType.EXPENSE, 2_469, today, 11, 11, 1, "餐饮", "🍜", "张正发茶餐厅（南京孝陵卫店）"),
        tx(3, TxType.EXPENSE, 2_290, today.minusDays(1), 20, 45, 3, "购物", "🛍️", "美团外卖", source = TxSource.NOTIFICATION),
        tx(4, TxType.EXPENSE, 2_220, today.minusDays(1), 11, 8, 3, "购物", "🛍️", "美团外卖", source = TxSource.NOTIFICATION),
        tx(5, TxType.EXPENSE, 8_840, today.minusDays(2), 19, 2, 4, "日用", "🧻", "盒马鲜生"),
        tx(6, TxType.EXPENSE, 1_860, today.minusDays(4), 8, 30, 2, "交通", "🚌", "滴滴出行"),
        tx(7, TxType.EXPENSE, 3_500, today.minusDays(5), 21, 10, 5, "娱乐", "🎮", "网易云音乐", note = "年费会员"),
        tx(8, TxType.INCOME, 1_200_000, today.minusDays(7), 9, 0, 7, "工资", "💰", note = "11 月"),
        tx(9, TxType.EXPENSE, 12_000, today.minusDays(10), 12, 30, 6, "住房", "🏠", "物业费"),
        tx(10, TxType.EXPENSE, 6_800, today.minusDays(12), 18, 20, 3, "购物", "🛍️", "南京新百"),
        // 上月数据：用来让报表的「比上期」有对比值
        tx(11, TxType.EXPENSE, 15_000, LocalDate.of(2026, 10, 12), 20, 0, 1, "餐饮", "🍜", "家庭聚餐"),
    )

    @Composable
    private fun AppFrame(selectedTab: Int, content: @Composable (Modifier) -> Unit) {
        Scaffold(
            containerColor = PageBg,
            bottomBar = {
                HomeBottomBar(selectedTab = selectedTab, onSelect = {}, onAdd = {})
            },
        ) { padding ->
            content(Modifier.padding(padding))
        }
    }

    @Test
    fun home() {
        val state = HomeUiState(
            sections = HomeGrouper.group(samples, today, zone),
            summary = HomeGrouper.summary(samples, today, zone),
            loaded = true,
        )

        paparazzi.snapshot(name = "home") {
            AIAccountingTheme {
                AppFrame(selectedTab = 0) { modifier ->
                    HomeScreen(state = state, modifier = modifier)
                }
            }
        }
    }

    @Test
    fun homeEmpty() {
        paparazzi.snapshot(name = "home_empty") {
            AIAccountingTheme {
                AppFrame(selectedTab = 0) { modifier ->
                    HomeScreen(state = HomeUiState(loaded = true), modifier = modifier)
                }
            }
        }
    }

    @Test
    fun reportMonth() {
        val range = ReportPeriods.of(PeriodUnit.MONTH, today)
        val state = ReportUiState(
            unit = PeriodUnit.MONTH,
            range = range,
            data = ReportCalculator.build(samples, range, today, zone),
        )

        paparazzi.snapshot(name = "report_month") {
            AIAccountingTheme {
                AppFrame(selectedTab = 1) { modifier ->
                    ReportScreen(
                        state = state,
                        modifier = modifier,
                        onSelectUnit = {},
                        onShift = {},
                    )
                }
            }
        }
    }

    @Test
    fun entrySheet() {        val categories = listOf(
            CategoryEntity(id = 1, name = "餐饮", emoji = "🍜", kind = "EXPENSE", sortOrder = 0),
            CategoryEntity(id = 2, name = "交通", emoji = "🚌", kind = "EXPENSE", sortOrder = 1),
            CategoryEntity(id = 3, name = "购物", emoji = "🛍️", kind = "EXPENSE", sortOrder = 2),
            CategoryEntity(id = 4, name = "日用", emoji = "🧻", kind = "EXPENSE", sortOrder = 3),
            CategoryEntity(id = 5, name = "娱乐", emoji = "🎮", kind = "EXPENSE", sortOrder = 4),
            CategoryEntity(id = 6, name = "医疗", emoji = "💊", kind = "EXPENSE", sortOrder = 5),
            CategoryEntity(id = 7, name = "住房", emoji = "🏠", kind = "EXPENSE", sortOrder = 6),
            CategoryEntity(id = 8, name = "通讯", emoji = "📱", kind = "EXPENSE", sortOrder = 7),
            CategoryEntity(id = 9, name = "人情", emoji = "🎁", kind = "EXPENSE", sortOrder = 8),
            CategoryEntity(id = 10, name = "其他", emoji = "📦", kind = "EXPENSE", sortOrder = 9),
        )
        val accounts = listOf(
            AccountEntity(id = 1, name = "微信", emoji = "💬", sortOrder = 0),
            AccountEntity(id = 2, name = "支付宝", emoji = "🔵", sortOrder = 1),
            AccountEntity(id = 3, name = "现金", emoji = "💵", sortOrder = 2),
            AccountEntity(id = 4, name = "银行卡", emoji = "💳", sortOrder = 3),
        )
        val state = EntryUiState(
            type = TxType.EXPENSE,
            expression = "12+8",
            amountCents = 2_000,
            categoryId = 1,
            accountId = 1,
            // 固定时间，否则日期胶囊文案会随运行时刻变化，快照永远对不上
            occurredAt = LocalDate.of(2026, 11, 20).atTime(9, 30)
                .atZone(zone).toInstant().toEpochMilli(),
            categories = categories,
            accounts = accounts,
        )

        paparazzi.snapshot(name = "entry_sheet") {
            AIAccountingTheme {
                Box(Modifier.fillMaxSize().background(Color.White)) {
                    EntryContent(
                        state = state,
                        onKey = {},
                        onType = {},
                        onCategory = {},
                        onAccount = {},
                        onNote = {},
                        onSave = {},
                    )
                }
            }
        }
    }

    @Test
    fun datePicker() {
        val picked = LocalDate.of(2026, 11, 20).atTime(9, 30)
            .atZone(zone).toInstant().toEpochMilli()

        paparazzi.snapshot(name = "date_picker") {
            AIAccountingTheme {
                Box(Modifier.fillMaxSize().background(PageBg)) {
                    DatePickerPanel(
                        initialMillis = picked,
                        onConfirm = {},
                        onDismiss = {},
                        modifier = Modifier.align(Alignment.BottomCenter),
                        today = today,
                        zone = zone,
                    )
                }
            }
        }
    }

    @Test
    fun settings() {
        paparazzi.snapshot(name = "settings") {
            AIAccountingTheme {
                SettingsScreen(
                    recordCount = 42,
                    pendingCount = 3,
                    webDavConfigured = true,
                    versionName = "0.1.1",
                    message = null,
                    onBack = {},
                    onExport = {},
                    onImportBackup = {},
                    onImportAlipay = {},
                    onImportWeChat = {},
                    onOpenWebDav = {},
                    onUploadCloud = {},
                    onDownloadCloud = {},
                    onInbox = {},
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
                    onRefreshHealth = {},
                )
            }
        }
    }
    @Test
    fun health() {
        paparazzi.snapshot(name = "health") {
            AIAccountingTheme {
                Box(Modifier.fillMaxSize().background(PageBg)) {
                    Column(Modifier.padding(16.dp)) {
                        HealthContent(
                            snapshot = HealthSnapshot(
                                notificationListenerEnabled = true,
                                accessibilityEnabled = false,
                                overlayGranted = true,
                                batteryUnrestricted = false,
                                lastCaptureAt = 1_795_510_000_000,
                                lastBackupAt = null,
                            ),
                            onOpenNotificationAccess = {},
                            onOpenAccessibility = {},
                            onOpenOverlay = {},
                            onRequestBatteryWhitelist = {},
                            onRefresh = {},
                        )
                    }
                }
            }
        }
    }
}