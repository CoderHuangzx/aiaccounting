package com.aiaccounting.overlay

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.view.WindowManager

/**
 * 只占一个「在其他应用上层」的隐形窗口，不画任何可见内容。
 *
 * 目的：让 App 保持"正在其他应用的上层显示内容"这个状态（系统状态栏里能看到），
 * 但不打扰使用——窗口 1×1 像素、完全透明、不接收触摸。
 *
 * 需要「显示在其他应用上层」权限；没授予时静默失败，不影响其他功能。
 */
class OverlayAnchorService : Service() {

    private var windowManager: WindowManager? = null
    private var anchorView: View? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        attachAnchor()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        detachAnchor()
        super.onDestroy()
    }

    private fun attachAnchor() {
        if (anchorView != null) return
        val manager = windowManager ?: return

        val view = View(this).apply {
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
        }
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
            // 完全透明，不参与合成
            alpha = 0f
        }

        runCatching { manager.addView(view, params) }
            .onSuccess { anchorView = view }
    }

    private fun detachAnchor() {
        val view = anchorView ?: return
        runCatching { windowManager?.removeView(view) }
        anchorView = null
    }
}
