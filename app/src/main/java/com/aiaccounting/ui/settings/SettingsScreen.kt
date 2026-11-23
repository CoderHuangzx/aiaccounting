package com.aiaccounting.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aiaccounting.ui.theme.AlertRed
import com.aiaccounting.ui.theme.BrandBlue
import com.aiaccounting.ui.theme.BrandBlueDeep
import com.aiaccounting.ui.theme.HairLine
import com.aiaccounting.ui.theme.IncomeGreen
import com.aiaccounting.ui.theme.PageBg
import com.aiaccounting.ui.theme.TextFaint
import com.aiaccounting.ui.theme.TextMain
import com.aiaccounting.ui.theme.TextSub

/**
 * 设置页。目前只放数据相关入口（导入/导出/云备份）和自动记账相关入口，
 * 后续「分类管理」「预算」之类按同样的 Row 结构往里加即可。
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
    onImport: () -> Unit,
    onImportBill: () -> Unit,
    onImportWeChat: () -> Unit,
    onWebDav: () -> Unit,
    onInbox: () -> Unit,
    onNotificationPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PageBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState()),
    ) {
        // 顶栏
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
                text = "设置",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextMain,
            )
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.width(32.dp))
        }

        Spacer(Modifier.height(6.dp))

        SettingsCard {
            SettingsRow(
                title = "导出数据",
                subtitle = "当前共 $recordCount 条记录，导出成 CSV 文件",
                onClick = onExport,
            )
            RowDivider()
            SettingsRow(
                title = "导入数据",
                subtitle = "选择 CSV 文件，覆盖恢复全部记录",
                onClick = onImport,
            )
            RowDivider()
            SettingsRow(
                title = "导入账单（支付宝）",
                subtitle = "选支付宝导出的 CSV，补历史账并自动跳过重复",
                onClick = onImportBill,
            )
            RowDivider()
            SettingsRow(
                title = "导入账单（微信）",
                subtitle = "选微信导出的 xlsx，补历史账并自动跳过重复",
                onClick = onImportWeChat,
            )
            RowDivider()
            SettingsRow(
                title = "云备份（WebDAV）",
                subtitle = if (webDavConfigured) "已配置，可上传/恢复" else "未配置，用坚果云做异地备份",
                onClick = onWebDav,
            )
        }

        Spacer(Modifier.height(12.dp))

        SettingsCard {
            SettingsRow(
                title = "待确认收件箱",
                subtitle = if (pendingCount > 0) "$pendingCount 笔等待确认" else "暂无待确认记录",
                value = if (pendingCount > 0) "$pendingCount" else null,
                onClick = onInbox,
            )
            RowDivider()
            SettingsRow(
                title = "通知读取权限",
                subtitle = "开启后自动捕捉微信/支付宝支付通知",
                onClick = onNotificationPermission,
            )
        }

        if (message != null) {
            Spacer(Modifier.height(12.dp))
            val isError = message.contains("失败")
            Text(
                text = message,
                fontSize = 12.sp,
                color = if (isError) AlertRed else IncomeGreen,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        }

        Spacer(Modifier.height(24.dp))
        Text(
            text = "AI 记账 v$versionName",
            fontSize = 11.sp,
            color = TextFaint,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
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
