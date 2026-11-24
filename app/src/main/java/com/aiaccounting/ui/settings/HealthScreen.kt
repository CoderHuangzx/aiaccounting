package com.aiaccounting.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.aiaccounting.data.HealthSnapshot
import com.aiaccounting.ui.theme.AlertRed
import com.aiaccounting.ui.theme.BrandBlue
import com.aiaccounting.ui.theme.BrandBlueSofter
import com.aiaccounting.ui.theme.HairLine
import com.aiaccounting.ui.theme.IncomeGreen
import com.aiaccounting.ui.theme.TextFaint
import com.aiaccounting.ui.theme.TextMain
import com.aiaccounting.ui.theme.TextSub
import java.time.Duration
import java.time.Instant

/**
 * 运行状态：把「安静失败」变成看得见的红绿灯。
 *
 * 自动记账依赖通知使用权、无障碍、悬浮窗、电池白名单四样授权；
 * 任何一样掉了都不会报错，只会悄悄不记账。这里逐项显示状态并可一键跳转修复。
 *
 * 只负责内容，标题栏和滚动由设置页的 PageScaffold 提供。
 */
@Composable
fun HealthContent(
    snapshot: HealthSnapshot,
    onOpenNotificationAccess: () -> Unit,
    onOpenAccessibility: () -> Unit,
    onOpenOverlay: () -> Unit,
    onRequestBatteryWhitelist: () -> Unit,
    onRefresh: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    if (snapshot.issueCount == 0) IncomeGreen.copy(alpha = 0.10f)
                    else AlertRed.copy(alpha = 0.10f),
                )
                .padding(14.dp),
        ) {
            Text(
                text = if (snapshot.issueCount == 0) {
                    "全部就绪，自动记账在正常工作"
                } else {
                    "有 ${snapshot.issueCount} 项未就绪，自动记账可能悄悄失效"
                },
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = if (snapshot.issueCount == 0) IncomeGreen else AlertRed,
            )
        }

        Spacer(Modifier.height(12.dp))

        HealthCard {
            HealthRow(
                title = "通知读取权限",
                detail = if (snapshot.notificationListenerEnabled) {
                    "已开启，能接收支付通知"
                } else {
                    "未开启 —— 自动记账完全失效"
                },
                ok = snapshot.notificationListenerEnabled,
                onClick = onOpenNotificationAccess,
                tag = "healthNotification",
            )
            HealthDivider()
            HealthRow(
                title = "读屏记账（下单即记）",
                detail = if (snapshot.accessibilityEnabled) {
                    "已开启，能识别订单页"
                } else {
                    "未开启 —— 抓不到 App 内下单；覆盖安装后需重新授权"
                },
                ok = snapshot.accessibilityEnabled,
                onClick = onOpenAccessibility,
                tag = "healthAccessibility",
            )
            HealthDivider()
            HealthRow(
                title = "显示在其他应用上层",
                detail = if (snapshot.overlayGranted) {
                    "已授予，状态提示可保持"
                } else {
                    "未授予 —— 状态提示会消失（不影响记账）"
                },
                ok = snapshot.overlayGranted,
                onClick = onOpenOverlay,
                tag = "healthOverlay",
            )
            HealthDivider()
            HealthRow(
                title = "电池优化白名单",
                detail = if (snapshot.batteryUnrestricted) {
                    "已加入白名单，后台不易被清理"
                } else {
                    "未加入 —— 长时间不用可能被系统限制"
                },
                ok = snapshot.batteryUnrestricted,
                onClick = onRequestBatteryWhitelist,
                tag = "healthBattery",
            )
        }

        Spacer(Modifier.height(12.dp))

        HealthCard {
            HealthRow(
                title = "最近一次捕捉",
                detail = snapshot.lastCaptureAt?.let { "上次捕获于 ${relativeTime(it)}" }
                    ?: "还没有捕捉到任何通知",
                ok = true,
                onClick = onRefresh,
                tag = "healthLastCapture",
            )
            HealthDivider()
            HealthRow(
                title = "最近一次云备份",
                detail = snapshot.lastBackupAt?.let { "上次备份于 ${relativeTime(it)}" }
                    ?: "还没有备份过",
                ok = true,
                onClick = onRefresh,
                tag = "healthLastBackup",
            )
        }

        Spacer(Modifier.height(14.dp))
        Text(
            text = "小提示：小米手机还需要在「应用管理 → AI 记账 → 自启动」里允许自启动，" +
                "否则划掉后台后可能被彻底清理。",
            fontSize = 11.sp,
            color = TextFaint,
            lineHeight = 16.sp,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        Spacer(Modifier.height(14.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(BrandBlueSofter)
                .clickable { onRefresh() }
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "重新检测", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = BrandBlue)
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun HealthRow(
    title: String,
    detail: String,
    ok: Boolean,
    onClick: () -> Unit,
    tag: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(tag)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (ok) IncomeGreen else AlertRed),
                )
                Spacer(Modifier.width(8.dp))
                Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = TextMain)
            }
            Spacer(Modifier.height(3.dp))
            Text(
                text = detail,
                fontSize = 11.sp,
                color = if (ok) TextSub else AlertRed,
                modifier = Modifier.padding(start = 16.dp),
                lineHeight = 15.sp,
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(text = "›", fontSize = 18.sp, color = TextFaint)
    }
}

@Composable
private fun HealthCard(content: @Composable () -> Unit) {
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
private fun HealthDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp)
            .height(1.dp)
            .background(HairLine),
    )
}

/** 「3 小时前」这种相对时间，比时间戳好读。 */
internal fun relativeTime(timestamp: Long, now: Long = System.currentTimeMillis()): String {
    val minutes = Duration.between(Instant.ofEpochMilli(timestamp), Instant.ofEpochMilli(now)).toMinutes()
    return when {
        minutes < 1 -> "刚刚"
        minutes < 60 -> "$minutes 分钟前"
        minutes < 60 * 24 -> "${minutes / 60} 小时前"
        minutes < 60 * 24 * 30 -> "${minutes / (60 * 24)} 天前"
        else -> "${minutes / (60 * 24 * 30)} 个月前"
    }
}
