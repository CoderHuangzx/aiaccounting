package com.aiaccounting.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aiaccounting.data.AutoSource
import com.aiaccounting.data.HealthSnapshot
import com.aiaccounting.ui.theme.AlertRed
import com.aiaccounting.ui.theme.BrandBlue
import com.aiaccounting.ui.theme.BrandBlueDeep
import com.aiaccounting.ui.theme.BrandBlueSofter
import com.aiaccounting.ui.theme.HairLine
import com.aiaccounting.ui.theme.IncomeGreen
import com.aiaccounting.ui.theme.PageBg
import com.aiaccounting.ui.theme.TextFaint
import com.aiaccounting.ui.theme.TextMain
import com.aiaccounting.ui.theme.TextSub

/** 设置里的页面。分两级：主页只放三个入口，细节都收进子页。 */
private enum class SettingsPage {
    MAIN,
    AUTO_CAPTURE,
    HEALTH,
    IMPORT,
    EXPORT,
    TUTORIAL_WECHAT,
    TUTORIAL_ALIPAY,
    TUTORIAL_OTHER,
}

/**
 * 设置。主页刻意做得很薄：自动记账 / 导入数据 / 导出数据，
 * 具体操作与教程都在子页里，避免一屏堆十几个入口。
 */
@Composable
fun SettingsScreen(
    recordCount: Int,
    pendingCount: Int,
    webDavConfigured: Boolean,
    versionName: String,
    message: String?,
    onBack: () -> Unit,
    onExport: () -> Unit,
    onImportBackup: () -> Unit,
    onImportAlipay: () -> Unit,
    onImportWeChat: () -> Unit,
    onOpenWebDav: () -> Unit,
    onUploadCloud: () -> Unit,
    onDownloadCloud: () -> Unit,
    onInbox: () -> Unit,
    onNotificationPermission: () -> Unit,
    onCaptureLog: () -> Unit,
    onOpenAccessibility: () -> Unit,
    health: HealthSnapshot,
    onOpenOverlayPermission: () -> Unit,
    onRequestBatteryWhitelist: () -> Unit,
    onRefreshHealth: () -> Unit,
    sourceStates: Map<AutoSource, Boolean>,
    onToggleSource: (AutoSource, Boolean) -> Unit,
    onToggleAllSources: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var page by rememberSaveable { mutableStateOf(SettingsPage.MAIN) }

    fun goBack() {
        if (page == SettingsPage.MAIN) onBack() else page = SettingsPage.MAIN
    }

    BackHandler { goBack() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PageBg)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        when (page) {
            SettingsPage.MAIN -> MainPage(
                pendingCount = pendingCount,
                versionName = versionName,
                message = message,
                onBack = onBack,
                onOpenAutoCapture = { page = SettingsPage.AUTO_CAPTURE },
                onOpenImport = { page = SettingsPage.IMPORT },
                onOpenExport = { page = SettingsPage.EXPORT },
            )

            SettingsPage.AUTO_CAPTURE -> AutoCapturePage(
                pendingCount = pendingCount,
                message = message,
                issueCount = health.issueCount,
                sourceStates = sourceStates,
                onBack = ::goBack,
                onInbox = onInbox,
                onNotificationPermission = onNotificationPermission,
                onCaptureLog = onCaptureLog,
                onOpenAccessibility = onOpenAccessibility,
                onOpenHealth = { page = SettingsPage.HEALTH },
                onToggleSource = onToggleSource,
                onToggleAllSources = onToggleAllSources,
            )

            SettingsPage.HEALTH -> PageScaffold(title = "运行状态", onBack = ::goBack) {
                HealthContent(
                    snapshot = health,
                    onOpenNotificationAccess = onNotificationPermission,
                    onOpenAccessibility = onOpenAccessibility,
                    onOpenOverlay = onOpenOverlayPermission,
                    onRequestBatteryWhitelist = onRequestBatteryWhitelist,
                    onRefresh = onRefreshHealth,
                )
            }

            SettingsPage.IMPORT -> ImportPage(
                webDavConfigured = webDavConfigured,
                message = message,
                onBack = ::goBack,
                onImportAlipay = onImportAlipay,
                onImportWeChat = onImportWeChat,
                onImportBackup = onImportBackup,
                onDownloadCloud = onDownloadCloud,
                onOpenTutorial = { page = it },
            )

            SettingsPage.EXPORT -> ExportPage(
                recordCount = recordCount,
                webDavConfigured = webDavConfigured,
                message = message,
                onBack = ::goBack,
                onExport = onExport,
                onUploadCloud = onUploadCloud,
                onOpenWebDav = onOpenWebDav,
            )

            SettingsPage.TUTORIAL_WECHAT -> TutorialPage(
                title = "微信账单导入教程",
                steps = WECHAT_STEPS,
                tip = "微信单次最多导出 3 个月；解压密码在「微信支付」服务号的通知里。",
                onBack = ::goBack,
                onPickFile = onImportWeChat,
            )

            SettingsPage.TUTORIAL_ALIPAY -> TutorialPage(
                title = "支付宝账单导入教程",
                steps = ALIPAY_STEPS,
                tip = "支付宝单次最多导出一年；解压密码在「我的账单」服务消息里。",
                onBack = ::goBack,
                onPickFile = onImportAlipay,
            )

            SettingsPage.TUTORIAL_OTHER -> TutorialPage(
                title = "其他平台 / 备份恢复",
                steps = OTHER_STEPS,
                tip = "恢复备份会覆盖当前全部记录，建议先导出一次做保险。",
                onBack = ::goBack,
                onPickFile = onImportBackup,
            )
        }
    }
}

// ── 主页 ────────────────────────────────────────────────────────────────

@Composable
private fun MainPage(
    pendingCount: Int,
    versionName: String,
    message: String?,
    onBack: () -> Unit,
    onOpenAutoCapture: () -> Unit,
    onOpenImport: () -> Unit,
    onOpenExport: () -> Unit,
) {
    PageScaffold(title = "设置", onBack = onBack) {
        SettingsCard {
            SettingsRow(
                title = "自动记账",
                subtitle = if (pendingCount > 0) "通知捕捉 · 有 $pendingCount 笔待确认" else "通知捕捉 · 收件箱",
                onClick = onOpenAutoCapture,
            )
        }
        Spacer(Modifier.height(12.dp))
        SettingsCard {
            SettingsRow(
                title = "导入数据",
                subtitle = "支付宝 / 微信账单 · 备份恢复",
                onClick = onOpenImport,
            )
            RowDivider()
            SettingsRow(
                title = "导出数据",
                subtitle = "CSV 备份 · 上传云端",
                onClick = onOpenExport,
            )
        }

        if (message != null) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = message,
                fontSize = 12.sp,
                color = if (message.contains("失败")) AlertRed else IncomeGreen,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        }

        Spacer(Modifier.height(28.dp))
        Text(
            text = "AI 记账 v$versionName",
            fontSize = 11.sp,
            color = TextFaint,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
    }
}

// ── 自动记账 ────────────────────────────────────────────────────────────

@Composable
private fun AutoCapturePage(
    pendingCount: Int,
    message: String?,
    issueCount: Int,
    sourceStates: Map<AutoSource, Boolean>,
    onBack: () -> Unit,
    onInbox: () -> Unit,
    onNotificationPermission: () -> Unit,
    onCaptureLog: () -> Unit,
    onOpenAccessibility: () -> Unit,
    onOpenHealth: () -> Unit,
    onToggleSource: (AutoSource, Boolean) -> Unit,
    onToggleAllSources: (Boolean) -> Unit,
) {
    val allEnabled = sourceStates.values.all { it }
    PageScaffold(title = "自动记账", onBack = onBack) {
        Text(
            text = "监听微信 / 支付宝 / 淘宝 / 京东 / 美团 / 抖音的支付通知，解析成一笔待确认记录。",
            fontSize = 12.sp,
            color = TextSub,
            lineHeight = 18.sp,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
        )
        Spacer(Modifier.height(10.dp))
        SettingsCard {
            SettingsRow(
                title = "运行状态",
                subtitle = if (issueCount == 0) "全部就绪" else "$issueCount 项未就绪，点开看看",
                value = if (issueCount > 0) "⚠" else "✓",
                onClick = onOpenHealth,
            )
            RowDivider()
            SettingsRow(
                title = "待确认收件箱",
                subtitle = if (pendingCount > 0) "$pendingCount 笔等待确认" else "暂无待确认记录",
                value = if (pendingCount > 0) "$pendingCount" else null,
                onClick = onInbox,
            )
            RowDivider()
            SettingsRow(
                title = "通知读取权限",
                subtitle = "没开这个，任何通知都收不到",
                onClick = onNotificationPermission,
            )
            RowDivider()
            SettingsRow(
                title = "捕捉日志",
                subtitle = "看看哪些通知被记了、哪些没匹配上",
                onClick = onCaptureLog,
            )
        }

        Spacer(Modifier.height(18.dp))
        Text(
            text = "来源",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextMain,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
        )
        Spacer(Modifier.height(6.dp))

        SettingsCard {
            AutoSource.entries.forEachIndexed { index, source ->
                if (index > 0) RowDivider()
                AutoSourceRow(
                    source = source,
                    enabled = sourceStates[source] ?: true,
                    onToggle = { onToggleSource(source, it) },
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        Text(
            text = "关掉的来源不再解析、不留日志、也不会生成待确认记录。",
            fontSize = 11.sp,
            color = TextFaint,
            lineHeight = 16.sp,
            modifier = Modifier.padding(horizontal = 20.dp),
        )

        Spacer(Modifier.height(18.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(if (allEnabled) BrandBlueSofter else BrandBlue)
                .clickable { onToggleAllSources(!allEnabled) }
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (allEnabled) "全部关闭" else "全部开启",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (allEnabled) BrandBlue else Color.White,
            )
        }

        if (message != null) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = message,
                fontSize = 12.sp,
                color = if (message.contains("失败")) AlertRed else IncomeGreen,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        }
    }
}

/** 一个来源一行：标题 + 说明 + 右侧开关。读屏那行点标题可以跳到无障碍设置。 */
@Composable
private fun AutoSourceRow(
    source: AutoSource,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = source.label,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = if (enabled) TextMain else TextFaint,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = source.detail,
                fontSize = 11.sp,
                color = if (enabled) TextSub else TextFaint,
                lineHeight = 15.sp,
            )
        }
        Switch(
            checked = enabled,
            onCheckedChange = onToggle,
            modifier = Modifier.testTag("source_${source.key}"),
            colors = SwitchDefaults.colors(checkedTrackColor = BrandBlue),
        )
    }
}

// ── 导入 ────────────────────────────────────────────────────────────────

@Composable
private fun ImportPage(
    webDavConfigured: Boolean,
    message: String?,
    onBack: () -> Unit,
    onImportAlipay: () -> Unit,
    onImportWeChat: () -> Unit,
    onImportBackup: () -> Unit,
    onDownloadCloud: () -> Unit,
    onOpenTutorial: (SettingsPage) -> Unit,
) {
    PageScaffold(title = "导入数据", onBack = onBack) {
        SettingsCard {
            SettingsRow(
                title = "支付宝账单",
                subtitle = "选支付宝导出的 CSV 文件",
                trailingText = "教程",
                trailingTag = "tutorialAlipay",
                onTrailingClick = { onOpenTutorial(SettingsPage.TUTORIAL_ALIPAY) },
                onClick = onImportAlipay,
            )
            RowDivider()
            SettingsRow(
                title = "微信账单",
                subtitle = "选微信导出的 xlsx 文件",
                trailingText = "教程",
                trailingTag = "tutorialWeChat",
                onTrailingClick = { onOpenTutorial(SettingsPage.TUTORIAL_WECHAT) },
                onClick = onImportWeChat,
            )
        }

        Spacer(Modifier.height(12.dp))

        SettingsCard {
            SettingsRow(
                title = "恢复备份文件",
                subtitle = "选本 App 导出的 CSV，覆盖恢复全部记录",
                trailingText = "说明",
                onTrailingClick = { onOpenTutorial(SettingsPage.TUTORIAL_OTHER) },
                onClick = onImportBackup,
            )
            RowDivider()
            SettingsRow(
                title = "从云端恢复",
                subtitle = if (webDavConfigured) "从 WebDAV 拉取最近一次备份" else "还没配置云备份（去「导出数据」里配）",
                onClick = {
                    if (webDavConfigured) onDownloadCloud()
                },
            )
        }

        Spacer(Modifier.height(12.dp))
        Text(
            text = "账单导入会先给预览：解析多少笔、几笔重复；确认后才写入，重复的自动跳过。",
            fontSize = 11.sp,
            color = TextFaint,
            lineHeight = 16.sp,
            modifier = Modifier.padding(horizontal = 20.dp),
        )

        if (message != null) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = message,
                fontSize = 12.sp,
                color = if (message.contains("失败")) AlertRed else IncomeGreen,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        }
    }
}

// ── 导出 ────────────────────────────────────────────────────────────────

@Composable
private fun ExportPage(
    recordCount: Int,
    webDavConfigured: Boolean,
    message: String?,
    onBack: () -> Unit,
    onExport: () -> Unit,
    onUploadCloud: () -> Unit,
    onOpenWebDav: () -> Unit,
) {
    PageScaffold(title = "导出数据", onBack = onBack) {
        SettingsCard {
            SettingsRow(
                title = "导出 CSV 文件",
                subtitle = "当前共 $recordCount 条记录，Excel 能直接打开",
                onClick = onExport,
            )
            RowDivider()
            SettingsRow(
                title = "上传到云端",
                subtitle = if (webDavConfigured) "把最新数据推到 WebDAV" else "先配置云备份",
                onClick = { if (webDavConfigured) onUploadCloud() },
            )
        }

        Spacer(Modifier.height(12.dp))

        SettingsCard {
            SettingsRow(
                title = "云备份设置（WebDAV）",
                subtitle = if (webDavConfigured) "已配置，可开自动备份" else "坚果云：填地址 + 邮箱 + 应用密码",
                onClick = onOpenWebDav,
            )
        }

        Spacer(Modifier.height(12.dp))
        Text(
            text = "导出的是纯文本 CSV，中文表头，用 Excel / WPS 都能打开；也支持导入回来。",
            fontSize = 11.sp,
            color = TextFaint,
            lineHeight = 16.sp,
            modifier = Modifier.padding(horizontal = 20.dp),
        )

        if (message != null) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = message,
                fontSize = 12.sp,
                color = if (message.contains("失败")) AlertRed else IncomeGreen,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        }
    }
}

// ── 教程 ────────────────────────────────────────────────────────────────

private data class TutorialStep(val title: String, val detail: String)

private val WECHAT_STEPS = listOf(
    TutorialStep("打开微信 →「我」→「服务」→「钱包」", "钱包页面右上角有「账单」入口"),
    TutorialStep("账单页右上角「常见问题」→「下载账单」", "在常见问题列表里找「下载账单」"),
    TutorialStep("用途选「用于个人对账」", "不要选「用于材料证明」，格式不一样"),
    TutorialStep("选时间范围并填自己的邮箱", "微信单次最长 3 个月，可多次导出"),
    TutorialStep("邮箱收到压缩包，解压密码在「微信支付」服务号通知里", "通知里会有 6 位数字密码"),
    TutorialStep("解压得到 .xlsx 文件，回到上一页选它导入", "本机会自动跳过与已有记录重复的账"),
)

private val ALIPAY_STEPS = listOf(
    TutorialStep("打开支付宝 →「我的」→「账单」", "账单页右上角有三个点「…」"),
    TutorialStep("点「…」→「开具交易流水证明」", "用途选「用于个人对账」后申请"),
    TutorialStep("选时间范围（最长一年）并填邮箱", "确认发送，账单会以压缩包发到邮箱"),
    TutorialStep("解压密码在「我的账单」服务消息里", "支付宝 → 消息 → 服务消息 → 交易流水文件发送成功通知"),
    TutorialStep("解压得到 .csv 文件，回到上一页选它导入", "CSV 可能是 GBK 编码，本机会自动识别"),
)

private val OTHER_STEPS = listOf(
    TutorialStep("想从别的 App 迁过来？先看它能不能导出 CSV", "银行 App、其他记账 App 一般都能导出"),
    TutorialStep("也可以直接用本 App 的备份文件", "「导出数据」里导出的 CSV 就是标准备份"),
    TutorialStep("选文件后确认恢复", "恢复会覆盖当前全部记录，先导一份做保险"),
)

@Composable
private fun TutorialPage(
    title: String,
    steps: List<TutorialStep>,
    tip: String,
    onBack: () -> Unit,
    onPickFile: () -> Unit,
) {
    PageScaffold(title = title, onBack = onBack) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color.White)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            steps.forEachIndexed { index, step ->
                Row(verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(BrandBlue),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "${index + 1}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = step.title,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextMain,
                            lineHeight = 19.sp,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = step.detail,
                            fontSize = 11.sp,
                            color = TextSub,
                            lineHeight = 16.sp,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(BrandBlueSofter)
                .padding(12.dp),
        ) {
            Text(
                text = tip,
                fontSize = 11.sp,
                color = BrandBlueDeep,
                lineHeight = 16.sp,
            )
        }

        Spacer(Modifier.height(18.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(BrandBlue)
                .clickable { onPickFile() }
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "选文件导入",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
            )
        }
    }
}

// ── 通用组件 ────────────────────────────────────────────────────────────

@Composable
private fun PageScaffold(
    title: String,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .testTag("settingsBack")
                    .clip(RoundedCornerShape(50))
                    .clickable { onBack() }
                    .padding(horizontal = 8.dp, vertical = 8.dp),
            ) {
                BackArrow()
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = title,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextMain,
            )
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.width(32.dp))
        }

        Spacer(Modifier.height(6.dp))
        content()
    }
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White),
    ) {
        content()
    }
}

@Composable
private fun SettingsRow(
    title: String,
    subtitle: String,
    value: String? = null,
    trailingText: String? = null,
    trailingTag: String? = null,
    onTrailingClick: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = TextMain,
            )
            Spacer(Modifier.height(3.dp))
            Text(text = subtitle, fontSize = 11.sp, color = TextSub)
        }
        if (value != null) {
            Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = BrandBlue)
            Spacer(Modifier.width(6.dp))
        }
        if (trailingText != null && onTrailingClick != null) {
            Text(
                text = trailingText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = BrandBlue,
                modifier = Modifier
                    .then(if (trailingTag != null) Modifier.testTag(trailingTag) else Modifier)
                    .clip(RoundedCornerShape(50))
                    .clickable { onTrailingClick() }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
            Spacer(Modifier.width(4.dp))
        }
        Text(text = "›", fontSize = 18.sp, color = TextFaint)
    }
}

@Composable
private fun RowDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp)
            .height(1.dp)
            .background(HairLine),
    )
}

/** 返回箭头用 Canvas 画，避免为一个小图标引依赖。 */
@Composable
private fun BackArrow() {
    androidx.compose.foundation.Canvas(modifier = Modifier.width(20.dp).height(20.dp)) {
        val stroke = 2.dp.toPx()
        val color = BrandBlueDeep
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(size.width * 0.72f, size.height * 0.2f),
            end = androidx.compose.ui.geometry.Offset(size.width * 0.3f, size.height * 0.5f),
            strokeWidth = stroke,
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(size.width * 0.3f, size.height * 0.5f),
            end = androidx.compose.ui.geometry.Offset(size.width * 0.72f, size.height * 0.8f),
            strokeWidth = stroke,
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
        )
    }
}
