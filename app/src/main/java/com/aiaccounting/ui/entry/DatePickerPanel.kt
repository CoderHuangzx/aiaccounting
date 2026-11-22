package com.aiaccounting.ui.entry

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aiaccounting.domain.DateTimeDraft
import com.aiaccounting.ui.components.ThinDivider
import com.aiaccounting.ui.components.WheelPicker
import com.aiaccounting.ui.theme.BrandBlue
import com.aiaccounting.ui.theme.BrandBlueDeep
import com.aiaccounting.ui.theme.BrandBlueSofter
import com.aiaccounting.ui.theme.TextFaint
import com.aiaccounting.ui.theme.TextMain
import com.aiaccounting.ui.theme.TextSub
import java.time.LocalDate
import java.time.ZoneId

/**
 * 记账时间选择面板：年/月/日 三个滚轮 + 时刻滚轮，底部取消/确认。
 */
@Composable
fun DatePickerPanel(
    initialMillis: Long,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    today: LocalDate = LocalDate.now(),
    zone: ZoneId = ZoneId.systemDefault(),
) {
    var draft by remember(initialMillis) { mutableStateOf(DateTimeDraft.from(initialMillis, zone)) }

    val years = remember(today) { (today.year - 5..today.year + 1).toList() }
    val months = remember { (1..12).toList() }
    val days = remember(draft.year, draft.month) { (1..draft.daysInMonth).toList() }
    val hours = remember { (0..23).toList() }
    val minutes = remember { (0..59).toList() }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
        color = Color.White,
        shadowElevation = 24.dp,
    ) {
        Column(modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 18.dp)) {
            Text(
                text = "编辑日期",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextMain,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(10.dp))
            ThinDivider()
            Spacer(Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                WheelPicker(
                    items = years.map { "${it}年" },
                    selectedIndex = years.indexOf(draft.year).coerceAtLeast(0),
                    onSelected = { index -> draft = draft.withYear(years[index]) },
                    modifier = Modifier.weight(1.3f),
                )
                WheelPicker(
                    items = months.map { "${it}月" },
                    selectedIndex = draft.month - 1,
                    onSelected = { index -> draft = draft.withMonth(months[index]) },
                    modifier = Modifier.weight(1f),
                )
                WheelPicker(
                    items = days.map { "${it}日" },
                    selectedIndex = draft.day - 1,
                    onSelected = { index -> draft = draft.withDay(days[index]) },
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(8.dp))
            ThinDivider()
            Spacer(Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "时刻",
                    fontSize = 14.sp,
                    color = TextMain,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                )
                WheelPicker(
                    items = hours.map { pad(it) },
                    selectedIndex = draft.hour,
                    onSelected = { index -> draft = draft.withHour(hours[index]) },
                    visibleCount = 3,
                    itemHeight = 34.dp,
                    fontSize = 15.sp,
                    selectedFontSize = 17.sp,
                    modifier = Modifier.width(56.dp),
                )
                Text(text = ":", fontSize = 16.sp, color = TextSub, modifier = Modifier.padding(horizontal = 2.dp))
                WheelPicker(
                    items = minutes.map { pad(it) },
                    selectedIndex = draft.minute,
                    onSelected = { index -> draft = draft.withMinute(minutes[index]) },
                    visibleCount = 3,
                    itemHeight = 34.dp,
                    fontSize = 15.sp,
                    selectedFontSize = 17.sp,
                    modifier = Modifier.width(56.dp),
                )
                Spacer(Modifier.weight(1f))
            }

            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PickerButton(
                    text = "取消",
                    primary = false,
                    modifier = Modifier.weight(1f),
                    onClick = onDismiss,
                )
                PickerButton(
                    text = "确认",
                    primary = true,
                    modifier = Modifier.weight(1f),
                    onClick = { onConfirm(draft.toMillis(zone)) },
                )
            }
        }
    }
}

@Composable
private fun PickerButton(
    text: String,
    primary: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (primary) BrandBlue else BrandBlueSofter)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (primary) Color.White else BrandBlueDeep,
        )
    }
}

private fun pad(value: Int): String = value.toString().padStart(2, '0')

/** 面板上面那层半透明遮罩，点一下收起。 */
@Composable
fun PickerScrim(onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.32f))
            .clickable { onDismiss() },
    )
}

/** 选择面板底部的提示文字，避免用户忘了当前在改哪笔。 */
@Composable
fun PickerHint(text: String) {
    Text(text = text, fontSize = 11.sp, color = TextFaint)
}
