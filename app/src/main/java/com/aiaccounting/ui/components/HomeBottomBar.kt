package com.aiaccounting.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aiaccounting.ui.theme.BrandBlue
import com.aiaccounting.ui.theme.TextFaint
import com.aiaccounting.ui.theme.TextSub

/**
 * 底部导航：记账 / 中间凸起的「记一笔」/ 报表。
 * 参考图里 FAB 是跨在导航栏上沿的，这里用 offset 让圆形按钮压住栏顶。
 */
@Composable
fun HomeBottomBar(
    selectedTab: Int,
    onSelect: (Int) -> Unit,
    onAdd: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Surface(
            color = Color.White,
            shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
            shadowElevation = 14.dp,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .height(64.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NavSlot(
                    label = "记账",
                    selected = selectedTab == 0,
                    modifier = Modifier.weight(1f),
                    onClick = { onSelect(0) },
                )
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "记一笔",
                        fontSize = 11.sp,
                        color = TextSub,
                        modifier = Modifier.padding(top = 26.dp),
                    )
                }
                NavSlot(
                    label = "报表",
                    selected = selectedTab == 1,
                    modifier = Modifier.weight(1f),
                    onClick = { onSelect(1) },
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-26).dp)
                .size(56.dp)
                .shadow(14.dp, CircleShape)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF4C85FF), Color(0xFF2255EE)),
                    ),
                )
                .clickable { onAdd() },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "+",
                color = Color.White,
                fontSize = 30.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun NavSlot(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        NavGlyph(label = label, selected = selected)
        Spacer(Modifier.height(3.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = if (selected) BrandBlue else TextSub,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

@Composable
private fun NavGlyph(label: String, selected: Boolean) {
    val color = if (selected) BrandBlue else TextFaint
    if (label == "记账") {
        Text(text = "¥", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = color)
    } else {
        Canvas(modifier = Modifier.size(17.dp)) {
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 280f,
                useCenter = true,
            )
            drawCircle(
                color = Color.White,
                radius = size.minDimension * 0.19f,
                center = Offset(size.width * 0.58f, size.height * 0.60f),
            )
        }
    }
}
