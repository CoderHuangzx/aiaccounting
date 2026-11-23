package com.aiaccounting.ui.inbox

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aiaccounting.core.DateLabels
import com.aiaccounting.core.Money
import com.aiaccounting.domain.Tx
import com.aiaccounting.domain.TxType
import com.aiaccounting.ui.components.EmojiBadge
import com.aiaccounting.ui.theme.AlertRed
import com.aiaccounting.ui.theme.BrandBlue
import com.aiaccounting.ui.theme.BrandBlueDeep
import com.aiaccounting.ui.theme.BrandBlueSofter
import com.aiaccounting.ui.theme.IncomeGreen
import com.aiaccounting.ui.theme.TextFaint
import com.aiaccounting.ui.theme.TextMain
import com.aiaccounting.ui.theme.TextSub
import java.time.ZoneId

/**
 * 待确认收件箱：通知捕捉进来的记录先落在这里，人工点一下才进正式账本。
 * 一条错账比十条漏账更伤信任，所以不做「零确认」。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxSheet(
    pending: List<Tx>,
    onConfirm: (Long) -> Unit,
    onIgnore: (Long) -> Unit,
    onOpenPermission: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 22.dp),
        ) {
            Text(
                text = "待确认",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextMain,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (pending.isEmpty()) {
                    "暂时没有捕捉到新的账。去系统设置里开启「通知使用权」，之后微信/支付宝的支付通知会自动进来。"
                } else {
                    "共 ${pending.size} 笔从通知里捕捉到的记录，确认后才计入账本。"
                },
                fontSize = 12.sp,
                color = TextSub,
                lineHeight = 18.sp,
            )

            Spacer(Modifier.height(16.dp))

            pending.forEach { tx ->
                PendingRow(
                    tx = tx,
                    onConfirm = { onConfirm(tx.id) },
                    onIgnore = { onIgnore(tx.id) },
                )
                Spacer(Modifier.height(8.dp))
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(BrandBlueSofter)
                    .clickable { onOpenPermission() }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "通知读取权限",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BrandBlueDeep,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "小米手机在「设置 → 通知与控制中心 → 通知使用权」里开启",
                        fontSize = 11.sp,
                        color = TextFaint,
                    )
                }
                Text(text = "去设置", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = BrandBlue)
            }

            Spacer(Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(BrandBlueSofter)
                    .clickable { onDismiss() },
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "关闭", fontSize = 14.sp, color = BrandBlueDeep, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun PendingRow(tx: Tx, onConfirm: () -> Unit, onIgnore: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EmojiBadge(emoji = tx.categoryEmoji, size = 38.dp)
        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = tx.merchant?.takeIf { it.isNotBlank() } ?: tx.categoryName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextMain,
                    maxLines = 1,
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = "${DateLabels.time(tx.occurredAt, ZoneId.systemDefault())} · ${tx.categoryName} · ${tx.accountName}",
                fontSize = 11.sp,
                color = TextSub,
                maxLines = 1,
            )
        }

        Text(
            text = if (tx.type == TxType.EXPENSE) "-" + Money.format(tx.amountCents) else "+" + Money.format(tx.amountCents),
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (tx.type == TxType.EXPENSE) TextMain else IncomeGreen,
        )

        Spacer(Modifier.width(10.dp))

        CircleAction(
            text = "✓",
            background = BrandBlue,
            textColor = Color.White,
            onClick = onConfirm,
        )
        Spacer(Modifier.width(6.dp))
        CircleAction(
            text = "✕",
            background = BrandBlueSofter,
            textColor = AlertRed,
            onClick = onIgnore,
        )
    }
}

@Composable
private fun CircleAction(
    text: String,
    background: Color,
    textColor: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(background)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, fontSize = 14.sp, color = textColor, fontWeight = FontWeight.SemiBold)
    }
}
