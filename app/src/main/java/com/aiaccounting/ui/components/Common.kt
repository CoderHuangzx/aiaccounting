package com.aiaccounting.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aiaccounting.ui.theme.BrandBlue
import com.aiaccounting.ui.theme.BrandBlueSoft
import com.aiaccounting.ui.theme.BrandBlueSofter
import com.aiaccounting.ui.theme.CardBg
import com.aiaccounting.ui.theme.TextSub

/** 圆角白卡片，参考图里所有内容块的统一容器。 */
@Composable
fun SoftCard(
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(22.dp),
    color: Color = CardBg,
    elevation: Dp = 6.dp,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = color,
        shadowElevation = elevation,
    ) {
        Column(modifier = Modifier.padding(contentPadding), content = content)
    }
}

/** 分类圆形图标：直接用 emoji，省掉一整套矢量图标依赖。 */
@Composable
fun EmojiBadge(
    emoji: String,
    size: Dp = 42.dp,
    background: Color = BrandBlueSoft,
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = emoji, fontSize = (size.value * 0.46f).sp)
    }
}

/** 来源标签：手动 / 通知 / 导入。 */
@Composable
fun SourceChip(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(BrandBlueSofter)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        color = BrandBlue,
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium,
    )
}

/** 底部导航用的分段胶囊（支出 / 收入）。 */
@Composable
fun SegmentToggle(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = BrandBlueSofter,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(containerColor)
            .padding(3.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (selected) BrandBlue else Color.Transparent)
                    .padding(horizontal = 20.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    color = if (selected) Color.White else TextSub,
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    modifier = Modifier.padding(horizontal = 2.dp),
                )
            }
        }
    }
}

/** 分组标题：左边竖线 + 标题，参考图里的「本周趋势」样式。 */
@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(width = 4.dp, height = 14.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(BrandBlue),
        )
        Spacer(Modifier.width(8.dp))
        Text(text = text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** 空状态。 */
@Composable
fun EmptyBlock(
    emoji: String,
    title: String,
    hint: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 26.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(78.dp)
                .clip(CircleShape)
                .background(BrandBlueSoft),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = emoji, fontSize = 32.sp)
        }
        Spacer(Modifier.height(12.dp))
        Text(text = title, fontSize = 14.sp, color = TextSub)
        Spacer(Modifier.height(4.dp))
        Text(text = hint, fontSize = 12.sp, color = TextSub.copy(alpha = 0.75f))
    }
}

/** 细分隔线。 */
@Composable
fun ThinDivider(modifier: Modifier = Modifier, color: Color = Color(0xFFEFF2F7)) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(color),
    )
}
