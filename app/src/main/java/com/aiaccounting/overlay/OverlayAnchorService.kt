package com.aiaccounting.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
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
 * 用**前台服务**承载，这样从最近任务划掉 App 后仍然存活（普通后台服务会被回收，
 * 窗口随之消失，系统那条提示也就没了）。代价是一条常驻通知，收在通知栏底部。
 */
class OverlayAnchorService : Service() {

    private var windowManager: WindowManager? = null
    private var anchorView: View? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startAsForeground()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        attachAnchor()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        detachAnchor()
        super.onDestroy()
    }

    private fun startAsForeground() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "后台常驻", NotificationManager.IMPORTANCE_MIN).apply {
                setShowBadge(false)
                setSound(null, null)
                enableVibration(false)
            },
        )

        val notification: Notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("AI 记账")
            .setContentText("正在其他应用的上层显示内容")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setOngoing(true)
            .setShowWhen(false)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun attachAnchor() {
        if (anchorView != null) return
        val manager = windowManager ?: return

        val view = View(this).apply { setBackgroundColor(Color.TRANSPARENT) }
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

    private fun detachAnchor() {
        val view = anchorView ?: return
        runCatching { windowManager?.removeView(view) }
        anchorView = null
    }

    private companion object {
        const val CHANNEL_ID = "overlay_anchor"
        const val NOTIFICATION_ID = 1002
    }
}
