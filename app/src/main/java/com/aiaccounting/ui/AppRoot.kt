package com.aiaccounting.ui

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aiaccounting.AccountingApp
import com.aiaccounting.bubble.BubbleSettingsStore
import com.aiaccounting.bubble.FloatingBubbleService
import com.aiaccounting.data.AccountingRepository
import com.aiaccounting.data.webdav.WebDavClient
import com.aiaccounting.data.webdav.WebDavConfig
import com.aiaccounting.data.webdav.WebDavSettingsStore
import com.aiaccounting.domain.AlipayBillParser
import com.aiaccounting.domain.WeChatBillParser
import com.aiaccounting.domain.XlsxReader
import com.aiaccounting.ui.backup.WebDavSheet
import com.aiaccounting.ui.settings.CaptureLogSheet
import com.aiaccounting.ui.settings.SettingsScreen
import com.aiaccounting.ui.components.HomeBottomBar
import com.aiaccounting.ui.entry.EntrySheet
import com.aiaccounting.ui.entry.EntryViewModel
import com.aiaccounting.ui.home.HomeScreen
import com.aiaccounting.ui.home.HomeViewModel
import com.aiaccounting.ui.inbox.InboxSheet
import com.aiaccounting.ui.report.ReportScreen
import com.aiaccounting.ui.report.ReportViewModel
import com.aiaccounting.ui.theme.AlertRed
import com.aiaccounting.ui.theme.PageBg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

@Composable
fun AppRoot(
    openEntryOnLaunch: Boolean = false,
    onEntryRequestConsumed: () -> Unit = {},
) {
    val context = LocalContext.current
    val repository = (context.applicationContext as AccountingApp).repository

    val homeViewModel: HomeViewModel = viewModel(factory = HomeViewModel.factory(repository))
    val reportViewModel: ReportViewModel = viewModel(factory = ReportViewModel.factory(repository))
    val entryViewModel: EntryViewModel = viewModel(factory = EntryViewModel.factory(repository))

    val homeState by homeViewModel.uiState.collectAsStateWithLifecycle()
    val pendingTxs by homeViewModel.pending.collectAsStateWithLifecycle()
    val reportState by reportViewModel.uiState.collectAsStateWithLifecycle()
    val entryState by entryViewModel.state.collectAsStateWithLifecycle()

    var tab by rememberSaveable { mutableIntStateOf(0) }
    var showEntry by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showInbox by remember { mutableStateOf(false) }
    var showCaptureLog by remember { mutableStateOf(false) }
    var captures by remember { mutableStateOf<List<com.aiaccounting.data.local.RawCaptureEntity>>(emptyList()) }
    var backupMessage by remember { mutableStateOf<String?>(null) }
    var billPreview by remember { mutableStateOf<AccountingRepository.BillImportPreview?>(null) }
    var billSourceName by remember { mutableStateOf("支付宝") }
    val scope = rememberCoroutineScope()

    val versionName = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "0.2.0"
    }

    val openNotificationSettings: () -> Unit = {
        val opened = runCatching {
            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }.isSuccess
        if (!opened) {
            Toast.makeText(context, "打不开系统设置，请手动去「通知使用权」里开启", Toast.LENGTH_LONG).show()
        }
    }

    val settingsStore = remember { WebDavSettingsStore(context) }
    var webDavConfig by remember { mutableStateOf(settingsStore.load()) }
    var showWebDav by remember { mutableStateOf(false) }
    var webDavBusy by remember { mutableStateOf(false) }

    // 悬浮球快捷记账
    val bubbleStore = remember { BubbleSettingsStore(context) }
    var bubbleEnabled by remember { mutableStateOf(bubbleStore.isEnabled()) }

    val toggleBubble: () -> Unit = {
        when {
            !Settings.canDrawOverlays(context) -> {
                backupMessage = "先开「显示在其他应用上层」，回来再点一次"
                runCatching {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            android.net.Uri.parse("package:${context.packageName}"),
                        ),
                    )
                }
            }

            bubbleEnabled -> {
                context.stopService(Intent(context, FloatingBubbleService::class.java))
                bubbleStore.setEnabled(false)
                bubbleEnabled = false
                backupMessage = "悬浮球已关闭"
            }

            else -> {
                runCatching {
                    context.startForegroundService(Intent(context, FloatingBubbleService::class.java))
                }
                bubbleStore.setEnabled(true)
                bubbleEnabled = true
                backupMessage = "悬浮球已开启，点小球就能记账"
            }
        }
    }

    // App 启动时如果之前是开着的，重新拉起服务
    LaunchedEffect(Unit) {
        if (bubbleStore.isEnabled() && Settings.canDrawOverlays(context)) {
            runCatching {
                context.startForegroundService(Intent(context, FloatingBubbleService::class.java))
            }
        }
    }

    // 悬浮球点进来 → 直接弹记账面板
    LaunchedEffect(openEntryOnLaunch) {
        if (openEntryOnLaunch) {
            entryViewModel.reset()
            showEntry = true
            onEntryRequestConsumed()
        }
    }

    /** 把当前数据推到云端。automatic = true 时不覆盖界面上的提示，避免打扰。 */
    val uploadToCloud: (Boolean) -> Unit = { automatic ->
        val config = webDavConfig
        if (!config.isConfigured) {
            if (!automatic) backupMessage = "先填好 WebDAV 地址、账号和应用密码"
        } else {
            scope.launch {
                if (!automatic) webDavBusy = true
                runCatching {
                    withContext(Dispatchers.IO) {
                        val csv = repository.exportBackupCsv()
                        WebDavClient(config).upload(csv)
                        csv.lineSequence().count { it.isNotBlank() } - 1
                    }
                }.onSuccess { count ->
                    if (!automatic) backupMessage = "已上传 $count 条记录到云端"
                }.onFailure { error ->
                    if (!automatic) backupMessage = "上传失败：${error.message}"
                }
                if (!automatic) webDavBusy = false
            }
        }
    }

    val downloadFromCloud: () -> Unit = {
        val config = webDavConfig
        if (!config.isConfigured) {
            backupMessage = "先填好 WebDAV 地址、账号和应用密码"
        } else {
            scope.launch {
                webDavBusy = true
                runCatching {
                    withContext(Dispatchers.IO) {
                        val text = WebDavClient(config).download() ?: error("云端还没有备份文件")
                        repository.importBackupCsv(text)
                    }
                }.onSuccess { count -> backupMessage = "已从云端恢复 $count 条记录" }
                    .onFailure { error -> backupMessage = "恢复失败：${error.message}" }
                webDavBusy = false
            }
        }
    }

    val autoBackupIfNeeded: () -> Unit = {
        if (webDavConfig.autoBackup && webDavConfig.isConfigured) {
            uploadToCloud(true)
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    val csv = repository.exportBackupCsv()
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            out.write(csv.toByteArray(Charsets.UTF_8))
                        } ?: error("无法写入目标文件")
                    }
                    val count = csv.lineSequence().count { it.isNotBlank() } - 1
                    "已导出 $count 条记录"
                }.onSuccess { backupMessage = it }
                    .onFailure { backupMessage = "导出失败：${it.message}" }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    val text = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText()
                            ?: error("无法读取文件")
                    }
                    val count = repository.importBackupCsv(text)
                    "已恢复 $count 条记录"
                }.onSuccess {
                    backupMessage = it
                    autoBackupIfNeeded()
                }
                    .onFailure { backupMessage = "导入失败：${it.message}" }
            }
        }
    }

    /** 支付宝账单：选文件 → 解析 → 先给预览（能导几笔、重复几笔）→ 再落库。 */
    val billLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    val bytes = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.readBytes()
                            ?: error("无法读取文件")
                    }
                    val text = AlipayBillParser.decode(bytes)
                    val rows = AlipayBillParser.parse(text)
                    require(rows.isNotEmpty()) { "文件里没有解析出任何交易" }
                    repository.previewBillImport(rows)
                }.onSuccess {
                    billSourceName = "支付宝"
                    billPreview = it
                }
                    .onFailure { backupMessage = "解析失败：${it.message}" }
            }
        }
    }

    /** 微信账单：xlsx → 自己解 zip + XML → 同一套预览/落库流程。 */
    val weChatLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    val bytes = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.readBytes()
                            ?: error("无法读取文件")
                    }
                    val rows = WeChatBillParser.parse(XlsxReader.readFirstSheet(bytes))
                    require(rows.isNotEmpty()) { "文件里没有解析出任何交易" }
                    repository.previewBillImport(rows)
                }.onSuccess {
                    billSourceName = "微信"
                    billPreview = it
                }
                    .onFailure { backupMessage = "解析失败：${it.message}" }
            }
        }
    }

    LaunchedEffect(entryState.saved) {
        if (entryState.saved) {
            val wasEditing = entryState.isEditing
            showEntry = false
            entryViewModel.reset()
            Toast.makeText(context, if (wasEditing) "已保存" else "已记一笔", Toast.LENGTH_SHORT).show()
            autoBackupIfNeeded()
        }
    }

    LaunchedEffect(entryState.deleted) {
        if (entryState.deleted) {
            showEntry = false
            entryViewModel.reset()
            Toast.makeText(context, "已删除", Toast.LENGTH_SHORT).show()
            autoBackupIfNeeded()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        containerColor = PageBg,
        bottomBar = {
            HomeBottomBar(
                selectedTab = tab,
                onSelect = { tab = it },
                onAdd = {
                    entryViewModel.reset()
                    showEntry = true
                },
            )
        },
    ) { innerPadding ->
        when (tab) {
            0 -> HomeScreen(
                state = homeState,
                modifier = Modifier.padding(innerPadding),
                onTxClick = { tx ->
                    entryViewModel.startEdit(tx)
                    showEntry = true
                },
                onSettingsClick = {
                    backupMessage = null
                    showSettings = true
                },
                onQueryChange = homeViewModel::onQueryChange,
                pendingCount = pendingTxs.size,
                onPendingClick = { showInbox = true },
            )

            else -> ReportScreen(
                state = reportState,
                modifier = Modifier.padding(innerPadding),
                onSelectUnit = reportViewModel::selectUnit,
                onShift = reportViewModel::shift,
            )
        }
    }

    if (showEntry) {
        EntrySheet(
            state = entryState,
            onKey = entryViewModel::onKey,
            onType = entryViewModel::onType,
            onCategory = entryViewModel::onCategory,
            onAccount = entryViewModel::onAccount,
            onNote = entryViewModel::onNote,
            onDateChange = entryViewModel::onDateTimeChange,
            onDelete = { confirmDelete = true },
            onSave = entryViewModel::save,
            onDismiss = {
                showEntry = false
                entryViewModel.reset()
            },
        )
    }

    if (showSettings) {
        SettingsScreen(
            recordCount = homeState.sections.sumOf { it.items.size },
            pendingCount = pendingTxs.size,
            webDavConfigured = webDavConfig.isConfigured,
            versionName = versionName,
            message = backupMessage,
            onBack = { showSettings = false },
            onExport = {
                backupMessage = null
                exportLauncher.launch("AI记账备份-${LocalDate.now()}.csv")
            },
            onImportBackup = {
                backupMessage = null
                importLauncher.launch(arrayOf("text/*", "application/octet-stream"))
            },
            onImportAlipay = {
                backupMessage = null
                billLauncher.launch(arrayOf("text/*", "text/csv", "application/octet-stream"))
            },
            onImportWeChat = {
                backupMessage = null
                weChatLauncher.launch(
                    arrayOf(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        "application/octet-stream",
                        "*/*",
                    ),
                )
            },
            onOpenWebDav = {
                backupMessage = null
                showSettings = false
                showWebDav = true
            },
            onUploadCloud = { uploadToCloud(false) },
            onDownloadCloud = downloadFromCloud,
            onInbox = {
                showSettings = false
                showInbox = true
            },
            onNotificationPermission = openNotificationSettings,
            onCaptureLog = { showCaptureLog = true },
            bubbleEnabled = bubbleEnabled,
            onToggleBubble = toggleBubble,
            modifier = Modifier.fillMaxSize(),
        )
    }
    }

    if (showWebDav) {
        WebDavSheet(
            initial = webDavConfig,
            busy = webDavBusy,
            message = backupMessage,
            onSave = { config ->
                settingsStore.save(config)
                webDavConfig = config
            },
            onUpload = { uploadToCloud(false) },
            onDownload = downloadFromCloud,
            onDismiss = { showWebDav = false },
        )
    }

    if (showInbox) {
        InboxSheet(
            pending = pendingTxs,
            onConfirm = homeViewModel::confirmPending,
            onIgnore = homeViewModel::ignorePending,
            onOpenPermission = openNotificationSettings,
            onDismiss = { showInbox = false },
        )
    }

    billPreview?.let { preview ->
        val source = billSourceName
        AlertDialog(
            onDismissRequest = { billPreview = null },
            title = { Text("导入${source}账单") },
            text = {
                Text(
                    "共解析 ${preview.total} 笔：\n" +
                        "可导入 ${preview.importable.size} 笔\n" +
                        "${preview.duplicates} 笔已存在，会自动跳过",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val rows = preview.importable
                        billPreview = null
                        scope.launch {
                            runCatching {
                                withContext(Dispatchers.IO) {
                                    repository.applyBillImport(rows, accountName = source)
                                }
                            }.onSuccess { count ->
                                backupMessage = "已导入 $count 笔账单"
                                autoBackupIfNeeded()
                            }.onFailure { error ->
                                backupMessage = "导入失败：${error.message}"
                            }
                        }
                    },
                ) {
                    Text("确认导入")
                }
            },
            dismissButton = {
                TextButton(onClick = { billPreview = null }) { Text("取消") }
            },
        )
    }

    if (showCaptureLog) {
        LaunchedEffect(Unit) {
            captures = repository.latestRawCaptures(50)
        }
        CaptureLogSheet(captures = captures, onDismiss = { showCaptureLog = false })
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("删除这笔记录？") },
            text = { Text("删除后无法恢复。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        entryViewModel.delete()
                    },
                ) {
                    Text("删除", color = AlertRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text("取消")
                }
            },
        )
    }
}
