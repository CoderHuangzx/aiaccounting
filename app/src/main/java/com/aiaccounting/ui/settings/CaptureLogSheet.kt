package com.aiaccounting.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.aiaccounting.data.local.RawCaptureEntity
import com.aiaccounting.domain.PaymentParser
import com.aiaccounting.ui.theme.AlertRed
import com.aiaccounting.ui.theme.BrandBlue
import com.aiaccounting.ui.theme.BrandBlueSofter
import com.aiaccounting.ui.theme.IncomeGreen
import com.aiaccounting.ui.theme.PageBg
import com.aiaccounting.ui.theme.TextFaint
import com.aiaccounting.ui.theme.TextMain
import com.aiaccounting.ui.theme.TextSub
import java.time.ZoneId

/**
 * 捕捉日志：最近捕捉到的通知原文 + 解析结果。
 *
 * 存在的意义是「为什么这笔没记上」能自己查——规则调不准时，这里是唯一的证据。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureLogSheet(
    captures: List<RawCaptureEntity>,
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
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp),
        ) {
            Text(
                text = "捕捉日志",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextMain,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "微信 / 支付宝 / 淘宝 / 京东 / 美团 / 抖音 的通知都会留档；" +
                    "标「未匹配」的说明规则没认出来，可以照着原文补规则。",
                fontSize = 11.sp,
                color = TextSub,
                lineHeight = 16.sp,
            )
            Spacer(Modifier.height(14.dp))

            if (captures.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(PageBg),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "还没有捕捉到通知\n去「通知读取权限」里开启后再支付一笔试试",
                        fontSize = 12.sp,
                        color = TextFaint,
                        lineHeight = 18.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 460.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 4.dp),
                ) {
                    items(captures, key = { it.id }) { capture ->
                        CaptureRow(capture)
                    }
                }
            }
        }
    }
}

@Composable
private fun CaptureRow(capture: RawCaptureEntity) {
    val channel = PaymentParser.channelOf(capture.packageName)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(PageBg)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = channel.label,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = BrandBlue,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = DateLabels.time(capture.capturedAt, ZoneId.systemDefault()),
                fontSize = 11.sp,
                color = TextFaint,
            )
            Spacer(Modifier.weight(1f))
            StatusChip(parsed = capture.parsed, reason = capture.reason)
        }
        Spacer(Modifier.height(5.dp))
        Text(
            text = listOfNotNull(capture.title, capture.text)
                .joinToString("　")
                .takeIf { it.isNotBlank() } ?: "（无正文）",
            fontSize = 12.sp,
            color = TextMain,
            lineHeight = 17.sp,
            maxLines = 4,
        )
        if (!capture.parsed && !capture.reason.isNullOrBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(text = capture.reason!!, fontSize = 10.sp, color = TextFaint)
        }
    }
}

@Composable
private fun StatusChip(parsed: Boolean, reason: String?) {
    val background = if (parsed) IncomeGreen.copy(alpha = 0.12f) else AlertRed.copy(alpha = 0.10f)
    val color = if (parsed) IncomeGreen else AlertRed
    val text = if (parsed) "已记账" else "未匹配"
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(background)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(text = text, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = color)
    }
}
