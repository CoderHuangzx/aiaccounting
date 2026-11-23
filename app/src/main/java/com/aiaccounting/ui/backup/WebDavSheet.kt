package com.aiaccounting.ui.backup

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aiaccounting.data.webdav.WebDavConfig
import com.aiaccounting.ui.theme.AlertRed
import com.aiaccounting.ui.theme.BrandBlue
import com.aiaccounting.ui.theme.BrandBlueDeep
import com.aiaccounting.ui.theme.BrandBlueSofter
import com.aiaccounting.ui.theme.IncomeGreen
import com.aiaccounting.ui.theme.TextFaint
import com.aiaccounting.ui.theme.TextMain
import com.aiaccounting.ui.theme.TextSub

/**
 * WebDAV 云备份设置：填服务器地址 / 账号 / 应用密码，可开自动备份，也能手动上传与恢复。
 */
private const val DEFAULT_JIANGUOYUN_URL = "https://dav.jianguoyun.com/dav/"
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebDavSheet(
    initial: WebDavConfig,
    busy: Boolean,
    message: String?,
    onSave: (WebDavConfig) -> Unit,
    onUpload: () -> Unit,
    onDownload: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    var url by remember(initial.baseUrl) {
        mutableStateOf(initial.baseUrl.ifBlank { DEFAULT_JIANGUOYUN_URL })
    }
    var user by remember(initial.username) { mutableStateOf(initial.username) }
    var password by remember(initial.password) { mutableStateOf(initial.password) }
    var auto by remember(initial.autoBackup) { mutableStateOf(initial.autoBackup) }

    fun current() = WebDavConfig(
        baseUrl = url,
        username = user,
        password = password,
        autoBackup = auto,
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 22.dp),
        ) {
            Text(
                text = "云备份（WebDAV）",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextMain,
            )
            Spacer(Modifier.height(10.dp))

            // 一步步照着填就行，不用去查文档
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(BrandBlueSofter)
                    .padding(14.dp),
            ) {
                Text(
                    text = "坚果云怎么填（三步）",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BrandBlueDeep,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "1. 网页登录坚果云 → 右上角头像 → 「账户信息」→「安全选项」\n" +
                        "2. 找到「第三方应用管理 / 添加应用密码」，起个名字（比如 AI记账）\n" +
                        "3. 把生成的密码一次性复制到这里；账号填你的坚果云注册邮箱",
                    fontSize = 11.sp,
                    color = TextSub,
                    lineHeight = 17.sp,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "打开坚果云网页 →",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = BrandBlue,
                    modifier = Modifier.clickable {
                        runCatching {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse("https://www.jianguoyun.com/")),
                            )
                        }
                    },
                )
            }

            Spacer(Modifier.height(14.dp))

            Field(
                label = "服务器地址（坚果云固定这个）",
                value = url,
                placeholder = DEFAULT_JIANGUOYUN_URL,
                onValueChange = { url = it },
            )
            Spacer(Modifier.height(10.dp))
            Field(
                label = "账号",
                value = user,
                placeholder = "邮箱",
                keyboardType = KeyboardType.Email,
                onValueChange = { user = it },
            )
            Spacer(Modifier.height(10.dp))
            Field(
                label = "应用密码",
                value = password,
                placeholder = "不是登录密码",
                isPassword = true,
                onValueChange = { password = it },
            )

            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "自动备份", fontSize = 14.sp, color = TextMain, fontWeight = FontWeight.Medium)
                    Text(
                        text = "每次保存、删除或恢复后自动上传一份",
                        fontSize = 11.sp,
                        color = TextFaint,
                    )
                }
                Switch(
                    checked = auto,
                    onCheckedChange = { auto = it },
                    colors = SwitchDefaults.colors(checkedTrackColor = BrandBlue),
                )
            }

            if (message != null) {
                Spacer(Modifier.height(12.dp))
                val isError = message.startsWith("失败") || message.contains("失败")
                Text(
                    text = message,
                    fontSize = 12.sp,
                    color = if (isError) AlertRed else IncomeGreen,
                )
            }

            Spacer(Modifier.height(18.dp))

            BigButton(
                text = if (busy) "请稍候…" else "保存并上传",
                primary = true,
                enabled = !busy,
                onClick = {
                    val config = current()
                    onSave(config)
                    if (config.isConfigured) onUpload()
                },
            )

            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BigButton(
                    text = "从云端恢复",
                    primary = false,
                    enabled = !busy,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        onSave(current())
                        onDownload()
                    },
                )
                BigButton(
                    text = "关闭",
                    primary = false,
                    enabled = true,
                    modifier = Modifier.weight(1f),
                    onClick = onDismiss,
                )
            }
        }
    }
}

@Composable
private fun Field(
    label: String,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, fontSize = 11.sp, color = TextSub)
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text(placeholder, fontSize = 13.sp, color = TextFaint) },
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp),
            visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BrandBlue,
                unfocusedBorderColor = Color(0xFFE6EBF3),
                focusedContainerColor = Color.White,
                unfocusedContainerColor = BrandBlueSofter,
            ),
        )
    }
}

@Composable
private fun BigButton(
    text: String,
    primary: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val background = when {
        primary && enabled -> BrandBlue
        primary -> BrandBlue.copy(alpha = 0.4f)
        else -> BrandBlueSofter
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (primary) Color.White else BrandBlueDeep,
        )
    }
}
