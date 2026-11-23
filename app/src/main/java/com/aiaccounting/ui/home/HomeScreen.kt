package com.aiaccounting.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
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
import com.aiaccounting.ui.theme.BrandBlue
import com.aiaccounting.ui.theme.BrandBlueDeep
import com.aiaccounting.ui.theme.BrandBlueSofter
import com.aiaccounting.ui.theme.HairLine
import com.aiaccounting.ui.theme.IncomeGreen
import com.aiaccounting.ui.theme.TextFaint
import com.aiaccounting.ui.theme.TextMain
import com.aiaccounting.ui.theme.TextSub
import java.time.ZoneId
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun HomeScreen(
    state: HomeUiState,
    modifier: Modifier = Modifier,
    onTxClick: (Tx) -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onQueryChange: (String) -> Unit = {},
    pendingCount: Int = 0,
    onPendingClick: () -> Unit = {},
) {
    var hideAmounts by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 84.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(key = "topbar") {
            HomeTopBar(onSettingsClick = onSettingsClick)
        }

        item(key = "header") {
            HomeHeader(
                summary = state.summary,
                hideAmounts = hideAmounts,
                onToggleAmounts = { hideAmounts = !hideAmounts },
            )
        }

        item(key = "search") {
            SearchBar(query = state.query, onQueryChange = onQueryChange)
        }

        if (pendingCount > 0) {
            item(key = "pending") {
                PendingBanner(count = pendingCount, onClick = onPendingClick)
            }
        }

        if (state.sections.isEmpty()) {
            item(key = "empty") {
                SoftCard(modifier = Modifier.fillMaxWidth()) {
                    EmptyBlock(
                        emoji = if (state.isSearching) "🔍" else "🧾",
                        title = when {
                            !state.loaded -> "正在加载…"
                            state.isSearching -> "没有匹配的记录"
                            else -> "还没有记录"
                        },
                        hint = if (state.isSearching) "换个关键词试试" else "点下面的 + 记第一笔",
                    )
                }
            }
        } else {
            state.sections.forEach { section ->
                // 一天一张卡，卡内用细分割线分行（参考图的排版）
                item(key = "day-${section.date}") {
                    DayHeader(section)
                }
                item(key = "card-${section.date}") {
                    DayCard(section = section, onTxClick = onTxClick)
                }
            }
        }
    }
}

@Composable
private fun HomeTopBar(onSettingsClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "记账",
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            color = TextMain,
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .testTag("homeSettings")
                .clip(CircleShape)
                .clickable { onSettingsClick() }
                .padding(6.dp),
        ) {
            HexagonGlyph(color = BrandBlue)
        }
    }
}

@Composable
private fun HomeHeader(
    summary: HomeSummary,
    hideAmounts: Boolean,
    onToggleAmounts: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
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

        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "今日支出（元）",
                    color = Color.White.copy(alpha = 0.82f),
                    fontSize = 12.sp,
                )
                Spacer(Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .testTag("toggleAmounts")
                        .clip(CircleShape)
                        .clickable { onToggleAmounts() }
                        .padding(4.dp),
                ) {
                    EyeGlyph(color = Color.White.copy(alpha = 0.85f), crossed = hideAmounts)
                }
            }

            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "¥",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = if (hideAmounts) "****" else Money.format(summary.todayExpenseCents),
                    color = Color.White,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color.White.copy(alpha = 0.16f)),
            )
            Spacer(Modifier.height(12.dp))

            Row {
                HeaderMetric(
                    label = "本月支出（元）",
                    value = if (hideAmounts) "****" else Money.format(summary.monthExpenseCents),
                    modifier = Modifier.weight(1f),
                )
                HeaderMetric(
                    label = "本月收入（元）",
                    value = if (hideAmounts) "****" else Money.format(summary.monthIncomeCents),
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
            .padding(top = 4.dp, start = 4.dp, end = 4.dp),
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
private fun DayCard(section: DaySection, onTxClick: (Tx) -> Unit) {
    SoftCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        elevation = 3.dp,
        contentPadding = PaddingValues(vertical = 2.dp),
    ) {
        section.items.forEachIndexed { index, tx ->
            TxRow(tx = tx, onClick = { onTxClick(tx) })
            if (index != section.items.lastIndex) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 64.dp, end = 14.dp)
                        .height(1.dp)
                        .background(HairLine),
                )
            }
        }
    }
}

@Composable
private fun TxRow(tx: Tx, onClick: () -> Unit = {}) {
    val subtitle = buildSubtitle(tx)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EmojiBadge(emoji = tx.categoryEmoji, size = 38.dp)
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

@Composable
private fun PendingBanner(count: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BrandBlueSofter)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(BrandBlue),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "$count",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "有 $count 笔待确认",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = BrandBlueDeep,
            )
            Text(
                text = "从微信/支付宝通知里捕捉到的",
                fontSize = 11.sp,
                color = TextSub,
            )
        }
        Text(
            text = "去确认",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = BrandBlue,
        )
    }
}

@Composable
private fun SearchBar(query: String, onQueryChange: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MagnifierGlyph(color = TextFaint)
        Spacer(Modifier.width(8.dp))
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            singleLine = true,
            textStyle = TextStyle(fontSize = 13.sp, color = TextMain),
            cursorBrush = SolidColor(BrandBlue),
            decorationBox = { inner ->
                Box {
                    if (query.isEmpty()) {
                        Text(
                            text = "搜索分类、商户、备注或金额",
                            fontSize = 13.sp,
                            color = TextFaint,
                        )
                    }
                    inner()
                }
            },
        )
        if (query.isNotEmpty()) {
            Text(
                text = "✕",
                fontSize = 13.sp,
                color = TextSub,
                modifier = Modifier
                    .testTag("searchClear")
                    .clip(RoundedCornerShape(50))
                    .clickable { onQueryChange("") }
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
    }
}

@Composable
private fun MagnifierGlyph(color: Color) {
    Canvas(modifier = Modifier.size(14.dp)) {
        val stroke = 1.6.dp.toPx()
        drawCircle(
            color = color,
            radius = size.minDimension * 0.32f,
            center = Offset(size.width * 0.42f, size.height * 0.42f),
            style = Stroke(width = stroke),
        )
        drawLine(
            color = color,
            start = Offset(size.width * 0.64f, size.height * 0.64f),
            end = Offset(size.width * 0.95f, size.height * 0.95f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

/** 六边形设置图标，照着参考图做的。 */
@Composable
private fun HexagonGlyph(color: Color) {
    Canvas(modifier = Modifier.size(20.dp)) {
        val radius = size.minDimension * 0.44f
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val path = Path()
        repeat(6) { index ->
            val angle = Math.toRadians(-90.0 + index * 60.0)
            val x = centerX + (radius * cos(angle)).toFloat()
            val y = centerY + (radius * sin(angle)).toFloat()
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 1.6.dp.toPx(), join = StrokeJoin.Round),
        )
        drawCircle(color = color, radius = radius * 0.22f, center = Offset(centerX, centerY))
    }
}

/** 眼睛图标：点击隐藏/显示金额。 */
@Composable
private fun EyeGlyph(color: Color, crossed: Boolean) {
    Canvas(modifier = Modifier.size(15.dp)) {
        val width = size.width
        val height = size.height
        val stroke = 1.3.dp.toPx()
        val path = Path().apply {
            moveTo(0f, height / 2f)
            cubicTo(width * 0.25f, height * 0.12f, width * 0.75f, height * 0.12f, width, height / 2f)
            cubicTo(width * 0.75f, height * 0.88f, width * 0.25f, height * 0.88f, 0f, height / 2f)
            close()
        }
        drawPath(path = path, color = color, style = Stroke(width = stroke))
        drawCircle(color = color, radius = height * 0.15f, center = Offset(width / 2f, height / 2f))
        if (crossed) {
            drawLine(
                color = color,
                start = Offset(width * 0.12f, height * 0.88f),
                end = Offset(width * 0.88f, height * 0.12f),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
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
