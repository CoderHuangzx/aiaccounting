package com.aiaccounting.ui.report

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aiaccounting.core.Money
import com.aiaccounting.domain.CategorySlice
import com.aiaccounting.domain.PeriodUnit
import com.aiaccounting.domain.ReportData
import com.aiaccounting.ui.components.EmojiBadge
import com.aiaccounting.ui.components.EmptyBlock
import com.aiaccounting.ui.components.SectionTitle
import com.aiaccounting.ui.components.SoftCard
import com.aiaccounting.ui.components.ThinDivider
import com.aiaccounting.ui.components.TrendBarChart
import com.aiaccounting.ui.theme.AlertRed
import com.aiaccounting.ui.theme.BrandBlue
import com.aiaccounting.ui.theme.BrandBlueSofter
import com.aiaccounting.ui.theme.IncomeGreen
import com.aiaccounting.ui.theme.TextFaint
import com.aiaccounting.ui.theme.TextMain
import com.aiaccounting.ui.theme.TextSub
import com.aiaccounting.ui.theme.TrackBg
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun ReportScreen(
    state: ReportUiState,
    modifier: Modifier = Modifier,
    onSelectUnit: (PeriodUnit) -> Unit,
    onShift: (Int) -> Unit,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 84.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "title") {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    text = "收支报表",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMain,
                )
            }
        }
        item(key = "tabs") {
            UnitTabs(selected = state.unit, onSelect = onSelectUnit)
        }
        item(key = "range") {
            RangeSwitcher(title = state.range.title, onShift = onShift)
        }
        item(key = "metrics") {
            MetricCard(data = state.data)
        }
        item(key = "trend") {
            TrendCard(data = state.data)
        }
        item(key = "categories") {
            CategoryCard(data = state.data)
        }
    }
}

@Composable
private fun UnitTabs(selected: PeriodUnit, onSelect: (PeriodUnit) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(5.dp),
    ) {
        PeriodUnit.entries.forEach { unit ->
            val isSelected = unit == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) BrandBlue else Color.Transparent)
                    .clickable { onSelect(unit) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = unit.label,
                    fontSize = 13.sp,
                    color = if (isSelected) Color.White else TextSub,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        }
    }
}

@Composable
private fun RangeSwitcher(title: String, onShift: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircleArrow(text = "‹", onClick = { onShift(-1) })
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = TextSub,
            )
        }
        CircleArrow(text = "›", onClick = { onShift(1) })
    }
}

@Composable
private fun CircleArrow(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(Color.White)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, fontSize = 16.sp, color = TextSub)
    }
}

@Composable
private fun MetricCard(data: ReportData) {
    SoftCard(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            MetricCell(
                label = "本期支出",
                value = Money.formatWithSymbol(data.expenseCents),
                modifier = Modifier.weight(1f),
            )
            MetricCell(
                label = "日均支出",
                value = Money.formatWithSymbol(data.averagePerDayCents),
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(14.dp))
        ThinDivider()
        Spacer(Modifier.height(14.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            MetricCell(
                label = "比上期",
                value = changeLabel(data),
                valueColor = changeColor(data),
                modifier = Modifier.weight(1f),
            )
            MetricCell(
                label = "收支结余",
                value = Money.formatWithSymbol(data.netCents),
                valueColor = if (data.netCents < 0) AlertRed else TextMain,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun MetricCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = TextMain,
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(width = 3.dp, height = 11.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(BrandBlue),
            )
            Spacer(Modifier.width(6.dp))
            Text(text = label, fontSize = 11.sp, color = TextSub)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = value,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor,
        )
    }
}

@Composable
private fun TrendCard(data: ReportData) {
    SoftCard(modifier = Modifier.fillMaxWidth()) {
        SectionTitle(text = "支出趋势")
        Spacer(Modifier.height(12.dp))
        if (data.buckets.any { it.cents > 0L }) {
            TrendBarChart(buckets = data.buckets)
        } else {
            EmptyBlock(emoji = "📈", title = "暂无记录", hint = "记一笔后这里会有趋势")
        }
    }
}

@Composable
private fun CategoryCard(data: ReportData) {
    SoftCard(modifier = Modifier.fillMaxWidth()) {
        SectionTitle(text = "分类占比")
        Spacer(Modifier.height(4.dp))
        if (data.categories.isEmpty()) {
            EmptyBlock(emoji = "🧩", title = "还没有支出分类", hint = "记一笔支出就能看到占比")
        } else {
            data.categories.forEach { slice ->
                CategoryRow(slice = slice)
            }
        }
    }
}

@Composable
private fun CategoryRow(slice: CategorySlice) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EmojiBadge(emoji = slice.emoji, size = 32.dp, background = BrandBlueSofter)
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = slice.name,
                    fontSize = 13.sp,
                    color = TextMain,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "${(slice.ratio * 100).roundToInt()}%",
                    fontSize = 10.sp,
                    color = TextFaint,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = Money.formatWithSymbol(slice.cents),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextMain,
                )
            }
            Spacer(Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(TrackBg),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(slice.ratio.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(BrandBlue),
                )
            }
        }
    }
}

private fun changeLabel(data: ReportData): String {
    if (data.previousExpenseCents == 0L) return "—"
    val percent = (abs(data.changeRatio) * 100).roundToInt()
    val sign = if (data.changeRatio >= 0f) "+" else "-"
    return "$sign$percent%"
}

private fun changeColor(data: ReportData): Color = when {
    data.previousExpenseCents == 0L -> TextSub
    data.changeRatio > 0f -> AlertRed
    data.changeRatio < 0f -> IncomeGreen
    else -> TextSub
}
