package com.aiaccounting.overlay

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager

/**
 * 「正在其他应用的上层显示内容」的隐形锚点。
 *
 * 就是一个 1×1 像素、全透明、不接收触摸的 overlay 窗口——界面上完全看不到，
 * 但系统状态栏里会保留那条提示。
 *
 * 进程级的单例：谁先调 [ensureAttached] 谁负责把窗口挂上。目前有两个入口：
 * - [OverlayAnchorService]：前台服务，正常情况下常驻；
 * - 通知监听服务：系统绑定的服务，App 被划掉后会被系统重新拉起，重新挂窗口。
 *
 * 因为窗口是进程资源，进程一死窗口就没了；靠"进程被重建后重新挂"来恢复，
 * 比单纯依赖前台服务更抗小米的后台清理。
 */
object OverlayAnchor {

    private var anchorView: View? = null

    fun ensureAttached(context: Context) {
        if (anchorView != null) return
        // 没给「显示在其他应用上层」权限就直接放弃，不抛异常
        if (!Settings.canDrawOverlays(context)) return

        val manager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return

        val view = View(context).apply { setBackgroundColor(Color.TRANSPARENT) }
        val params = WindowManager.LayoutParams(
            1,
            1,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 0
            alpha = 0f
        }

        runCatching { manager.addView(view, params) }
            .onSuccess { anchorView = view }
    }

    fun detach(context: Context) {
        val view = anchorView ?: return
        val manager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        runCatching { manager?.removeView(view) }
        anchorView = null
    }
}
