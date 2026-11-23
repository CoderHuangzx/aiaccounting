package com.aiaccounting.ui.backup

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
import com.aiaccounting.ui.theme.AlertRed
import com.aiaccounting.ui.theme.BrandBlue
import com.aiaccounting.ui.theme.BrandBlueDeep
import com.aiaccounting.ui.theme.BrandBlueSofter
import com.aiaccounting.ui.theme.IncomeGreen
import com.aiaccounting.ui.theme.TextFaint
import com.aiaccounting.ui.theme.TextMain
import com.aiaccounting.ui.theme.TextSub

/**
 * 备份与恢复面板：导出 CSV / 从 CSV 恢复。
 * 刻意做成「本地文件」而不是云同步——数据不出手机，也不依赖任何付费服务。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupSheet(
    recordCount: Int,
    message: String?,
    onExport: () -> Unit,
    onImport: () -> Unit,
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
                .padding(bottom = 22.dp),
        ) {
            Text(
                text = "备份与恢复",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextMain,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "当前共 $recordCount 条记录。导出的 CSV 可以直接用 Excel 打开，也可以存到网盘做备份。",
                fontSize = 12.sp,
                color = TextSub,
                lineHeight = 18.sp,
            )

            Spacer(Modifier.height(18.dp))

            ActionRow(
                title = "导出为 CSV",
                subtitle = "生成一份包含全部流水的文件",
                primary = true,
                onClick = onExport,
            )

            Spacer(Modifier.height(10.dp))

            ActionRow(
                title = "从 CSV 恢复",
                subtitle = "会用文件内容覆盖当前所有记录，恢复前请先导出一份",
                primary = false,
                onClick = onImport,
            )

            if (message != null) {
                Spacer(Modifier.height(14.dp))
                val isError = message.startsWith("失败") || message.startsWith("导出失败") || message.startsWith("导入失败")
                Text(
                    text = message,
                    fontSize = 12.sp,
                    color = if (isError) AlertRed else IncomeGreen,
                )
            }

            Spacer(Modifier.height(18.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(BrandBlueSofter)
                    .clickable { onDismiss() },
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "关闭", fontSize = 14.sp, color = BrandBlueDeep, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun ActionRow(
    title: String,
    subtitle: String,
    primary: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (primary) BrandBlue else BrandBlueSofter)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (primary) Color.White else BrandBlueDeep,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = if (primary) Color.White.copy(alpha = 0.78f) else TextFaint,
            )
        }
        Text(
            text = if (primary) "导出" else "选择文件",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = if (primary) Color.White else BrandBlue,
        )
    }
}
