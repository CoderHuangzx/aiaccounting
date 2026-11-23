package com.aiaccounting.bubble

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import com.aiaccounting.MainActivity
import kotlin.math.abs

/**
 * 悬浮球：一个能拖动的圆球，点一下直接打开记账面板。
 *
 * 需要「显示在其他应用上层」权限；用前台服务承载，否则离开 App 后窗口会被系统回收。
 * 用途是给自动捕捉兜底——通知没抓到的时候，在任何 App 里都能一秒补记。
 */
class FloatingBubbleService : Service() {

    private lateinit var windowManager: WindowManager
    private var bubbleView: View? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        startAsForeground()
        showBubble()
        instance = java.lang.ref.WeakReference(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        instance = null
        removeBubble()
        super.onDestroy()
    }

    private fun startAsForeground() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "快捷记账悬浮球", NotificationManager.IMPORTANCE_MIN).apply {
                setShowBadge(false)
            },
        )
        val notification: Notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("快捷记账")
            .setContentText("点悬浮球记一笔")
            .setSmallIcon(android.R.drawable.ic_input_add)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun showBubble() {
        if (bubbleView != null) return

        val density = resources.displayMetrics.density
        val size = (52 * density).toInt()
        val margin = (12 * density).toInt()

        val view = TextView(this).apply {
            text = "¥"
            setTextColor(Color.WHITE)
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(0xFF2A5BF0.toInt())
                setStroke((1.5f * density).toInt(), 0x40FFFFFF)
            }
        }

        val params = WindowManager.LayoutParams(
            size,
            size,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = resources.displayMetrics.widthPixels - size - margin
            y = resources.displayMetrics.heightPixels / 2
        }

        attachDragBehaviour(view, params)
        runCatching { windowManager.addView(view, params) }
            .onSuccess {
                bubbleView = view
                layoutParams = params
            }
    }

    private fun attachDragBehaviour(view: View, params: WindowManager.LayoutParams) {
        val slop = (8 * resources.displayMetrics.density)
        var downRawX = 0f
        var downRawY = 0f
        var startX = 0
        var startY = 0
        var dragging = false

        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    downRawX = event.rawX
                    downRawY = event.rawY
                    startX = params.x
                    startY = params.y
                    dragging = false
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downRawX
                    val dy = event.rawY - downRawY
                    if (!dragging && (abs(dx) > slop || abs(dy) > slop)) dragging = true
                    if (dragging) {
                        params.x = startX + dx.toInt()
                        params.y = startY + dy.toInt()
                        runCatching { windowManager.updateViewLayout(view, params) }
                    }
                    true
                }

                MotionEvent.ACTION_UP -> {
                    if (!dragging) openEntry()
                    true
                }

                else -> false
            }
        }
    }

    private fun openEntry() {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(MainActivity.EXTRA_OPEN_ENTRY, true)
        }
        runCatching { startActivity(intent) }
    }

    private fun removeBubble() {
        bubbleView?.let { runCatching { windowManager.removeView(it) } }
        bubbleView = null
        layoutParams = null
    }

    companion object {
        private const val CHANNEL_ID = "bubble"
        private const val NOTIFICATION_ID = 1001

        private var instance: java.lang.ref.WeakReference<FloatingBubbleService>? = null

        /** 自己的 App 在前台时把球藏起来，免得挡住记账面板。 */
        fun setVisible(visible: Boolean) {
            instance?.get()?.bubbleView?.visibility = if (visible) View.VISIBLE else View.GONE
        }
    }
}
