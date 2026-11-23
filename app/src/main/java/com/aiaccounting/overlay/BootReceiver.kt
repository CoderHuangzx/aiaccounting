package com.aiaccounting.overlay

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings

/**
 * 开机 / 覆盖安装之后把隐形锚点服务拉起来，省得每次都要先打开 App。
 * 没有悬浮窗权限时什么都不做。
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (!Settings.canDrawOverlays(context)) return
        runCatching {
            context.startForegroundService(Intent(context, OverlayAnchorService::class.java))
        }
    }
}
