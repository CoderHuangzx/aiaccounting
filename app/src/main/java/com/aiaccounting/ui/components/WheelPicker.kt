package com.aiaccounting.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aiaccounting.ui.theme.BrandBlue
import com.aiaccounting.ui.theme.BrandBlueSofter
import com.aiaccounting.ui.theme.TextFaint
import com.aiaccounting.ui.theme.TextSub
import kotlinx.coroutines.flow.filter

/**
 * 滚轮选择器：LazyColumn + 吸附对齐，中间一行是选中值。
 * 不引第三方 wheel 库，保持依赖干净。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WheelPicker(
    items: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    visibleCount: Int = 3,
    itemHeight: Dp = 40.dp,
    fontSize: TextUnit = 17.sp,
    selectedFontSize: TextUnit = 20.sp,
    highlightColor: Color = BrandBlueSofter,
) {
    if (items.isEmpty()) return

    val safeIndex = selectedIndex.coerceIn(0, items.lastIndex)
    val state = rememberLazyListState(initialFirstVisibleItemIndex = safeIndex)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = state)
    val itemHeightPx = with(LocalDensity.current) { itemHeight.toPx() }

    val centeredIndex by remember {
        derivedStateOf {
            val offset = state.firstVisibleItemScrollOffset
            (state.firstVisibleItemIndex + if (offset > itemHeightPx / 2f) 1 else 0)
                .coerceIn(0, items.lastIndex)
        }
    }

    // 滚动停稳后把结果回传给外部
    LaunchedEffect(state, items.size) {
        snapshotFlow { state.isScrollInProgress }
            .filter { inProgress -> !inProgress }
            .collect {
                if (centeredIndex != selectedIndex) onSelected(centeredIndex)
            }
    }

    // 外部改了选中值（比如换月后日期被夹紧）时，把滚轮滚过去
    LaunchedEffect(selectedIndex) {
        if (!state.isScrollInProgress && state.firstVisibleItemIndex != safeIndex) {
            state.animateScrollToItem(safeIndex)
        }
    }

    Box(
        modifier = modifier.height(itemHeight * visibleCount),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .clip(RoundedCornerShape(12.dp))
                .background(highlightColor),
        )

        LazyColumn(
            state = state,
            flingBehavior = flingBehavior,
            contentPadding = PaddingValues(vertical = itemHeight * ((visibleCount - 1) / 2)),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            itemsIndexed(items) { index, label ->
                val selected = index == centeredIndex
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        fontSize = if (selected) selectedFontSize else fontSize,
                        color = when {
                            selected -> BrandBlue
                            else -> TextFaint
                        },
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/** 滚轮两侧的静态标签，例如「年」「月」「日」。 */
@Composable
fun WheelSuffix(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        fontSize = 12.sp,
        color = TextSub,
        modifier = modifier,
    )
}
