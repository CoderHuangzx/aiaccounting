package com.aiaccounting.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aiaccounting.core.Money
import com.aiaccounting.domain.ReportBucket
import com.aiaccounting.ui.theme.BrandBlue
import com.aiaccounting.ui.theme.TextFaint
import com.aiaccounting.ui.theme.TextSub

/**
 * 支出趋势柱状图。纯 Canvas 绘制，不引第三方图表库。
 * 柱子按最大值归一化，超过 12 根时只显示部分横轴标签，避免糊成一片。
 */
@Composable
fun TrendBarChart(
    buckets: List<ReportBucket>,
    modifier: Modifier = Modifier,
    barColor: Color = BrandBlue,
    chartHeight: androidx.compose.ui.unit.Dp = 148.dp,
) {
    if (buckets.isEmpty()) return
    val max = buckets.maxOf { it.cents }.coerceAtLeast(1L)
    val showAll = buckets.size <= 12

    Column(modifier = modifier) {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text(text = "峰值", fontSize = 10.sp, color = TextFaint)
            Spacer(Modifier.width(4.dp))
            Text(text = Money.formatWithSymbol(max), fontSize = 10.sp, color = TextFaint)
        }
        Spacer(Modifier.height(6.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeight),
        ) {
            val width = size.width
            val height = size.height
            val bottom = height - 2.dp.toPx()
            val dash = PathEffect.dashPathEffect(floatArrayOf(6f, 8f), 0f)

            for (i in 1..3) {
                val y = bottom * i / 4f
                drawLine(
                    color = Color(0xFFE9EEF7),
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = dash,
                )
            }

            val slot = width / buckets.size
            val barWidth = (slot * 0.52f).coerceAtMost(14.dp.toPx()).coerceAtLeast(3.dp.toPx())
            val radius = CornerRadius(barWidth / 2f, barWidth / 2f)

            buckets.forEachIndexed { index, bucket ->
                if (bucket.cents <= 0L) return@forEachIndexed
                val barHeight = (bucket.cents.toFloat() / max) * (height - 10.dp.toPx())
                val left = index * slot + (slot - barWidth) / 2f
                val top = bottom - barHeight
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(barColor, barColor.copy(alpha = 0.55f)),
                        startY = top,
                        endY = bottom,
                    ),
                    topLeft = Offset(left, top),
                    size = Size(barWidth, barHeight),
                    cornerRadius = radius,
                )
            }
        }

        Spacer(Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            buckets.forEachIndexed { index, bucket ->
                val visible = showAll || index % 5 == 0 || index == buckets.lastIndex
                Text(
                    text = if (visible) bucket.label else "",
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = 9.sp,
                    color = TextSub,
                    maxLines = 1,
                )
            }
        }
    }
}
