package com.aiaccounting.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aiaccounting.core.DateLabels
import com.aiaccounting.core.Money
import com.aiaccounting.domain.DaySection
import com.aiaccounting.domain.HomeSummary
import com.aiaccounting.domain.Tx
import com.aiaccounting.domain.TxSource
import com.aiaccounting.domain.TxType
import com.aiaccounting.ui.components.EmojiBadge
import com.aiaccounting.ui.components.EmptyBlock
import com.aiaccounting.ui.components.SoftCard
import com.aiaccounting.ui.components.SourceChip
import com.aiaccounting.ui.theme.IncomeGreen
import com.aiaccounting.ui.theme.TextFaint
import com.aiaccounting.ui.theme.TextMain
import com.aiaccounting.ui.theme.TextSub
import java.time.ZoneId

@Composable
fun HomeScreen(
    state: HomeUiState,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 84.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "header") {
            HomeHeader(summary = state.summary)
            Spacer(Modifier.height(6.dp))
        }

        if (state.sections.isEmpty()) {
            item(key = "empty") {
                SoftCard(modifier = Modifier.fillMaxWidth()) {
                    EmptyBlock(
                        emoji = "🧾",
                        title = if (state.loaded) "还没有记录" else "正在加载…",
                        hint = "点下面的 + 记第一笔",
                    )
                }
            }
        } else {
            state.sections.forEach { section ->
                item(key = "day-${section.date}") {
                    DayHeader(section)
                }
                items(items = section.items, key = { it.id }) { tx ->
                    TxRow(tx)
                }
            }
        }
    }
}

@Composable
private fun HomeHeader(summary: HomeSummary) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF4C85FF), Color(0xFF2A5BF0)),
                    start = Offset.Zero,
                    end = Offset(1200f, 700f),
                ),
            ),
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawCircle(
                color = Color.White.copy(alpha = 0.08f),
                radius = size.height * 0.62f,
                center = Offset(size.width * 0.94f, size.height * 0.06f),
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.06f),
                radius = size.height * 0.40f,
                center = Offset(size.width * 0.62f, size.height * 1.02f),
            )
        }

        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "今日支出（元）",
                    color = Color.White.copy(alpha = 0.82f),
                    fontSize = 12.sp,
                )
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.18f))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = summary.monthLabel.ifBlank { "本月" },
                        color = Color.White,
                        fontSize = 11.sp,
                    )
                }
            }

            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "¥",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = Money.format(summary.todayExpenseCents),
                    color = Color.White,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color.White.copy(alpha = 0.16f)),
            )
            Spacer(Modifier.height(12.dp))

            Row {
                HeaderMetric(
                    label = "本月支出",
                    value = Money.formatWithSymbol(summary.monthExpenseCents),
                    modifier = Modifier.weight(1f),
                )
                HeaderMetric(
                    label = "本月收入",
                    value = Money.formatWithSymbol(summary.monthIncomeCents),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun HeaderMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(text = label, color = Color.White.copy(alpha = 0.78f), fontSize = 11.sp)
        Spacer(Modifier.height(3.dp))
        Text(
            text = value,
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun DayHeader(section: DaySection) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = section.label,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextMain,
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = "支 ${Money.format(section.expenseCents)} 收 ${Money.format(section.incomeCents)}",
            fontSize = 11.sp,
            color = TextSub,
        )
    }
}

@Composable
private fun TxRow(tx: Tx) {
    val subtitle = buildSubtitle(tx)
    SoftCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        elevation = 3.dp,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            EmojiBadge(emoji = tx.categoryEmoji, size = 40.dp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = tx.categoryName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextMain,
                    )
                    if (tx.source != TxSource.MANUAL) {
                        Spacer(Modifier.width(6.dp))
                        SourceChip(text = tx.source.label())
                    }
                }
                if (subtitle.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = TextSub,
                        maxLines = 1,
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (tx.type == TxType.EXPENSE) {
                    "-" + Money.format(tx.amountCents)
                } else {
                    "+" + Money.format(tx.amountCents)
                },
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (tx.type == TxType.EXPENSE) TextMain else IncomeGreen,
            )
        }
    }
}

private fun buildSubtitle(tx: Tx): String {
    val time = DateLabels.time(tx.occurredAt, ZoneId.systemDefault())
    val who = tx.merchant?.takeIf { it.isNotBlank() } ?: tx.accountName
    val note = tx.note?.takeIf { it.isNotBlank() }
    return listOfNotNull(time, who, note).joinToString(" · ")
}

private fun TxSource.label(): String = when (this) {
    TxSource.MANUAL -> "手动"
    TxSource.NOTIFICATION -> "通知记账"
    TxSource.IMPORT -> "账单导入"
}
