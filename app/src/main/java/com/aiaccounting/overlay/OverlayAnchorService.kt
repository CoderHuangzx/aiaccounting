package com.aiaccounting.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder

/**
 * 承载隐形锚点的前台服务。
 *
 * 双保险的第一层：正常运行时由它维持窗口；
 * 第二层在通知监听服务里（见 OverlayAnchor 的注释），App 被划掉后靠系统重绑恢复。
 */
class OverlayAnchorService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startAsForeground()
        OverlayAnchor.ensureAttached(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        OverlayAnchor.ensureAttached(this)
        return START_STICKY
    }

    override fun onDestroy() {
        // 只在前台服务真正停止时拆窗口；进程被杀时窗口由系统回收，这里不会执行
        OverlayAnchor.detach(this)
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

    private companion object {
        const val CHANNEL_ID = "overlay_anchor"
        const val NOTIFICATION_ID = 1002
    }
}
