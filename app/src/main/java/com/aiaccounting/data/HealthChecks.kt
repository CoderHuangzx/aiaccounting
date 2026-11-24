package com.aiaccounting.data

import android.content.Context
import android.os.PowerManager
import android.provider.Settings

/**
 * 运行状态自检。
 *
 * 自动记账依赖三样系统授权，任何一个掉了都不会报错——只会安静地不再记账。
 * 这个页面就是把「安静失败」变成看得见的红绿灯。
 */
data class HealthSnapshot(
    val notificationListenerEnabled: Boolean,
    val accessibilityEnabled: Boolean,
    val overlayGranted: Boolean,
    val batteryUnrestricted: Boolean,
    /** 最近一次捕捉到通知的时间，null 表示从未捕捉到。 */
    val lastCaptureAt: Long?,
    /** 最近一次云备份时间，null 表示还没备份过。 */
    val lastBackupAt: Long?,
) {
    /** 有问题的事项数量，0 表示全部就绪。 */
    val issueCount: Int
        get() = listOf(
            notificationListenerEnabled,
            accessibilityEnabled,
            overlayGranted,
            batteryUnrestricted,
        ).count { !it }
}

object HealthChecks {

    private const val SECURE_NOTIFICATION_LISTENERS = "enabled_notification_listeners"
    private const val SECURE_ACCESSIBILITY = "enabled_accessibility_services"

    fun snapshot(context: Context, lastCaptureAt: Long?, lastBackupAt: Long?): HealthSnapshot =
        HealthSnapshot(
            notificationListenerEnabled = notificationListenerEnabled(context),
            accessibilityEnabled = accessibilityEnabled(context),
            overlayGranted = overlayGranted(context),
            batteryUnrestricted = batteryUnrestricted(context),
            lastCaptureAt = lastCaptureAt,
            lastBackupAt = lastBackupAt,
        )

    /** 「通知使用权」是否给了本 App。 */
    fun notificationListenerEnabled(context: Context): Boolean =
        secureSetting(context, SECURE_NOTIFICATION_LISTENERS)?.contains(context.packageName) == true

    /** 无障碍服务是否启用（覆盖安装后系统会自动关掉它）。 */
    fun accessibilityEnabled(context: Context): Boolean =
        secureSetting(context, SECURE_ACCESSIBILITY)?.contains(context.packageName) == true

    /** 「显示在其他应用上层」权限。 */
    fun overlayGranted(context: Context): Boolean = Settings.canDrawOverlays(context)

    /** 是否已加入电池优化白名单（否则后台容易被清理）。 */
    fun batteryUnrestricted(context: Context): Boolean {
        val power = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return false
        return power.isIgnoringBatteryOptimizations(context.packageName)
    }

    private fun secureSetting(context: Context, key: String): String? =
        runCatching { Settings.Secure.getString(context.contentResolver, key) }.getOrNull()
}
