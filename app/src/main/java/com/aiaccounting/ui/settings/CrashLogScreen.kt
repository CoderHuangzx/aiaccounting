package com.aiaccounting.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aiaccounting.data.CrashStore
import com.aiaccounting.ui.theme.AlertRed
import com.aiaccounting.ui.theme.BrandBlue
import com.aiaccounting.ui.theme.BrandBlueSofter
import com.aiaccounting.ui.theme.HairLine
import com.aiaccounting.ui.theme.PageBg
import com.aiaccounting.ui.theme.TextFaint
import com.aiaccounting.ui.theme.TextMain
import com.aiaccounting.ui.theme.TextSub
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 崩溃页所需数据：列表 + 当前展开的那条的完整堆栈。 */
data class CrashLogState(
    val entries: List<CrashStore.Entry> = emptyList(),
    val detail: String? = null,
)

/**
 * 崩溃记录页（内容部分，标题栏由设置页的 PageScaffold 提供）。
 *
 * 只读本地文件，不联网。点一条看完整堆栈，可复制。
 */
@Composable
fun CrashLogContent(
    entries: List<CrashStore.Entry>,
    expandedText: String?,
    onOpen: (CrashStore.Entry) -> Unit,
    onCloseDetail: () -> Unit,
    onCopy: (String) -> Unit,
    onClear: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "只记录本机崩溃，不上传。复现问题时把这里的内容发我就能定位。",
            fontSize = 11.sp,
            color = TextSub,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
        )
        Spacer(Modifier.height(10.dp))

        if (entries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(140.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "还没有崩溃记录\n（这通常是好事）",
                    fontSize = 12.sp,
                    color = TextFaint,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp,
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White),
            ) {
                entries.forEachIndexed { index, entry ->
                    if (index > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp)
                                .height(1.dp)
                                .background(HairLine),
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("crash_${entry.occurredAt}")
                            .clickable { onOpen(entry) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = formatTime(entry.occurredAt),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextMain,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = entry.summary,
                                fontSize = 11.sp,
                                color = TextSub,
                                maxLines = 2,
                                lineHeight = 15.sp,
                            )
                        }
                        Text(text = "›", fontSize = 18.sp, color = TextFaint)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(PageBg)
                    .testTag("clearCrashes")
                    .clickable { onClear() }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "清空记录", fontSize = 14.sp, color = AlertRed)
            }
        }

        if (expandedText != null) {
            Spacer(Modifier.height(14.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(BrandBlueSofter)
                    .padding(14.dp),
            ) {
                Text(text = "完整堆栈", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = BrandBlue)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = expandedText,
                    fontSize = 10.sp,
                    color = TextMain,
                    lineHeight = 14.sp,
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color.White)
                            .testTag("copyCrash")
                            .clickable { onCopy(expandedText) }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    ) {
                        Text(text = "复制", fontSize = 12.sp, color = BrandBlue)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color.White)
                            .clickable { onCloseDetail() }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    ) {
                        Text(text = "收起", fontSize = 12.sp, color = TextSub)
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

private fun formatTime(millis: Long): String =
    SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault()).format(Date(millis))
