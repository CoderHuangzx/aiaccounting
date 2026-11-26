package com.aiaccounting.ui.entry

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import com.aiaccounting.ui.theme.HairLine
import com.aiaccounting.ui.theme.PageBg
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
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
import com.aiaccounting.data.local.CategoryEntity
import com.aiaccounting.domain.TxType
import com.aiaccounting.ui.components.SegmentToggle
import com.aiaccounting.ui.theme.AlertRed
import com.aiaccounting.ui.theme.BrandBlue
import com.aiaccounting.ui.theme.BrandBlueDeep
import com.aiaccounting.ui.theme.BrandBlueSoft
import com.aiaccounting.ui.theme.BrandBlueSofter
import com.aiaccounting.ui.theme.TextFaint
import com.aiaccounting.ui.theme.TextMain
import com.aiaccounting.ui.theme.TextSub
import com.aiaccounting.ui.theme.TrackBg
import java.time.Instant
import java.time.ZoneId

private val KEYPAD = listOf(
    "7", "8", "9", "⌫",
    "4", "5", "6", "+",
    "1", "2", "3", "-",
    ".", "0", "C", "完成",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntrySheet(
    state: EntryUiState,
    onKey: (String) -> Unit,
    onType: (TxType) -> Unit,
    onCategory: (Long) -> Unit,
    onAccount: (Long) -> Unit,
    onNote: (String) -> Unit,
    onDateChange: (Long) -> Unit,
    onDelete: () -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        EntryContent(
            state = state,
            onKey = onKey,
            onType = onType,
            onCategory = onCategory,
            onAccount = onAccount,
            onNote = onNote,
            onDateChange = onDateChange,
            onDelete = onDelete,
            onSave = onSave,
        )
    }
}

/** 面板内容。抽出来是为了能在测试里脱离 ModalBottomSheet 单独渲染。 */
@Composable
fun EntryContent(
    state: EntryUiState,
    onKey: (String) -> Unit,
    onType: (TxType) -> Unit,
    onCategory: (Long) -> Unit,
    onAccount: (Long) -> Unit,
    onNote: (String) -> Unit,
    onDateChange: (Long) -> Unit = {},
    onDelete: () -> Unit = {},
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDatePicker by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
                .padding(bottom = 18.dp),
        ) {
            if (state.isEditing) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "编辑记录", fontSize = 13.sp, color = TextSub)
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "删除",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = AlertRed,
                        modifier = Modifier
                            .testTag("entryDelete")
                            .clip(RoundedCornerShape(50))
                            .clickable { onDelete() }
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
                Spacer(Modifier.height(6.dp))
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                SegmentToggle(
                    options = listOf("支出", "收入"),
                    selectedIndex = if (state.type == TxType.EXPENSE) 0 else 1,
                    onSelect = { index ->
                        onType(if (index == 0) TxType.EXPENSE else TxType.INCOME)
                    },
                )
                Spacer(Modifier.weight(1f))
                DateChip(
                    millis = state.occurredAt,
                    onClick = { showDatePicker = true },
                )
            }

            AmountDisplay(state)

            CategoryGrid(state = state, onCategory = onCategory)

            AccountRow(state = state, onAccount = onAccount)

            NoteField(note = state.note, onNote = onNote)

            Keypad(state = state, onKey = onKey, onSave = onSave)
        }

        if (showDatePicker) {
            PickerScrim(
                onDismiss = { showDatePicker = false },
                modifier = Modifier.matchParentSize(),
            )
            DatePickerPanel(
                initialMillis = state.occurredAt,
                onConfirm = { millis ->
                    onDateChange(millis)
                    showDatePicker = false
                },
                onDismiss = { showDatePicker = false },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

/** 记账时间入口：显示当前选中的日期时间，点开滚轮选择器。 */
@Composable
private fun DateChip(millis: Long, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .testTag("entryDateChip")
            .clip(RoundedCornerShape(50))
            .background(BrandBlueSofter)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CalendarGlyph(color = BrandBlue)
        Spacer(Modifier.width(6.dp))
        Text(
            text = DateLabels.chipLabel(millis, ZoneId.systemDefault()),
            fontSize = 12.sp,
            color = BrandBlueDeep,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun CalendarGlyph(color: Color) {
    Canvas(modifier = Modifier.size(13.dp)) {
        val stroke = 1.4.dp.toPx()
        drawRoundRect(
            color = color,
            topLeft = Offset(0f, size.height * 0.16f),
            size = Size(size.width, size.height * 0.8f),
            cornerRadius = CornerRadius(size.width * 0.2f),
            style = Stroke(width = stroke),
        )
        drawLine(
            color = color,
            start = Offset(0f, size.height * 0.4f),
            end = Offset(size.width, size.height * 0.4f),
            strokeWidth = stroke,
        )
        drawLine(
            color = color,
            start = Offset(size.width * 0.3f, 0f),
            end = Offset(size.width * 0.3f, size.height * 0.24f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = Offset(size.width * 0.7f, 0f),
            end = Offset(size.width * 0.7f, size.height * 0.24f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
private fun AmountDisplay(state: EntryUiState) {
    Column(modifier = Modifier.padding(top = 16.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = "¥",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (state.amountCents != null) TextMain else TextFaint,
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = Money.format(state.amountCents ?: 0L),
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = if (state.amountCents != null) TextMain else TextFaint,
            )
        }
        val hasOperator = state.expression.contains('+') || state.expression.contains('-')
        Text(
            text = if (hasOperator) "${state.expression} = ${Money.plain(state.amountCents ?: 0L)}" else " ",
            fontSize = 11.sp,
            color = TextSub,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
private fun CategoryGrid(state: EntryUiState, onCategory: (Long) -> Unit) {
    // 两级分类：先选一级，再选子分类；选了子分类就记在子分类上
    val parents = state.categories.filter { it.parentId == null }
    val selected = state.categories.firstOrNull { it.id == state.categoryId }
    val activeParentId = selected?.parentId ?: selected?.id
    val children = state.categories.filter { it.parentId != null && it.parentId == activeParentId }

    Column(
        modifier = Modifier.padding(top = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        parents.chunked(5).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                row.forEach { category ->
                    CategoryCell(
                        category = category,
                        selected = category.id == activeParentId,
                        modifier = Modifier.weight(1f),
                        onClick = onCategory,
                    )
                }
                repeat(5 - row.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }

        if (children.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
                    .height(1.dp)
                    .background(HairLine),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                children.forEach { child ->
                    val active = child.id == state.categoryId
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (active) BrandBlueSofter else PageBg)
                            .clickable { onCategory(child.id) }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(text = child.emoji, fontSize = 16.sp)
                        Text(
                            text = child.name,
                            fontSize = 11.sp,
                            color = if (active) BrandBlue else TextSub,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryCell(
    category: CategoryEntity,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: (Long) -> Unit,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick(category.id) }
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(if (selected) BrandBlueSoft else BrandBlueSofter)
                .then(
                    if (selected) {
                        Modifier.border(1.5.dp, BrandBlue, CircleShape)
                    } else {
                        Modifier
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = category.emoji, fontSize = 19.sp)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = category.name,
            fontSize = 11.sp,
            color = if (selected) BrandBlue else TextSub,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

@Composable
private fun AccountRow(state: EntryUiState, onAccount: (Long) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(top = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        state.accounts.forEach { account ->
            val selected = account.id == state.accountId
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (selected) BrandBlueSoft else BrandBlueSofter)
                    .clickable { onAccount(account.id) }
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = account.emoji, fontSize = 13.sp)
                Spacer(Modifier.width(5.dp))
                Text(
                    text = account.name,
                    fontSize = 12.sp,
                    color = if (selected) BrandBlueDeep else TextSub,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        }
    }
}

@Composable
private fun NoteField(note: String, onNote: (String) -> Unit) {
    BasicTextField(
        value = note,
        onValueChange = onNote,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(BrandBlueSofter)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        textStyle = TextStyle(fontSize = 13.sp, color = TextMain),
        singleLine = true,
        decorationBox = { inner ->
            Box {
                if (note.isEmpty()) {
                    Text(text = "备注（可选）", fontSize = 13.sp, color = TextFaint)
                }
                inner()
            }
        },
    )
}

@Composable
private fun Keypad(state: EntryUiState, onKey: (String) -> Unit, onSave: () -> Unit) {
    Column(
        modifier = Modifier.padding(top = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        KEYPAD.chunked(4).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { key ->
                    if (key == "完成") {
                        CompleteKey(
                            state = state,
                            modifier = Modifier.weight(1f),
                            onSave = onSave,
                        )
                    } else {
                        KeyCell(key = key, modifier = Modifier.weight(1f), onKey = onKey)
                    }
                }
            }
        }
    }
}

@Composable
private fun KeyCell(key: String, modifier: Modifier = Modifier, onKey: (String) -> Unit) {
    val accent = key in listOf("+", "-", "⌫", "C")
    Box(
        modifier = modifier
            .height(50.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (accent) TrackBg else BrandBlueSofter)
            .clickable { onKey(key) },
        contentAlignment = Alignment.Center,
    ) {
        if (key == "⌫") {
            BackspaceGlyph(color = BrandBlue)
        } else {
            Text(
                text = key,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = if (accent) BrandBlue else TextMain,
            )
        }
    }
}

/** 退格键图标：符号字体里的 ⌫ 字重不一，直接画一个更稳。 */
@Composable
private fun BackspaceGlyph(color: Color) {
    Canvas(modifier = Modifier.size(21.dp)) {
        val width = size.width
        val height = size.height
        val strokeWidth = 1.8.dp.toPx()

        val body = Path().apply {
            moveTo(width * 0.04f, height * 0.5f)
            lineTo(width * 0.30f, height * 0.16f)
            lineTo(width * 0.96f, height * 0.16f)
            lineTo(width * 0.96f, height * 0.84f)
            lineTo(width * 0.30f, height * 0.84f)
            close()
        }
        drawPath(
            path = body,
            color = color,
            style = Stroke(width = strokeWidth, join = StrokeJoin.Round),
        )
        drawLine(
            color = color,
            start = Offset(width * 0.46f, height * 0.38f),
            end = Offset(width * 0.78f, height * 0.62f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = Offset(width * 0.78f, height * 0.38f),
            end = Offset(width * 0.46f, height * 0.62f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
private fun CompleteKey(
    state: EntryUiState,
    modifier: Modifier = Modifier,
    onSave: () -> Unit,
) {
    Box(
        modifier = modifier
            .height(50.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (state.canSave) BrandBlue else BrandBlue.copy(alpha = 0.35f))
            .clickable(enabled = state.canSave) { onSave() },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = when {
                state.saving -> "保存中"
                state.isEditing -> "保存"
                else -> "完成"
            },
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
