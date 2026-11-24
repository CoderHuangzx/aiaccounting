package com.aiaccounting.data.webdav

import android.content.Context

/**
 * WebDAV 配置的持久化。
 *
 * 用应用私有的 SharedPreferences 保存，文件不导出（Android 9+ 无法被其他应用读取）。
 * 密码建议用「应用密码」（坚果云可单独生成），即使泄露也能单独吊销。
 */
class WebDavSettingsStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("webdav_config", Context.MODE_PRIVATE)

    fun load(): WebDavConfig = WebDavConfig(
        baseUrl = prefs.getString(KEY_URL, "").orEmpty(),
        username = prefs.getString(KEY_USER, "").orEmpty(),
        password = prefs.getString(KEY_PASS, "").orEmpty(),
        autoBackup = prefs.getBoolean(KEY_AUTO, false),
        backupCaptures = prefs.getBoolean(KEY_CAPTURES, true),
    )

    fun save(config: WebDavConfig) {
        prefs.edit()
            .putString(KEY_URL, config.baseUrl.trim())
            .putString(KEY_USER, config.username.trim())
            .putString(KEY_PASS, config.password)
            .putBoolean(KEY_AUTO, config.autoBackup)
            .putBoolean(KEY_CAPTURES, config.backupCaptures)
            .apply()
    }

    fun setAutoBackup(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO, enabled).apply()
    }

    /** 记录最近一次成功上传的时间，自检页用来显示「多久没备份了」。 */
    fun setLastBackupAt(timestamp: Long) {
        prefs.edit().putLong(KEY_LAST_BACKUP, timestamp).apply()
    }

    fun lastBackupAt(): Long? =
        prefs.getLong(KEY_LAST_BACKUP, 0L).takeIf { it > 0L }

    fun clear() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val KEY_URL = "base_url"
        const val KEY_USER = "username"
        const val KEY_PASS = "password"
        const val KEY_AUTO = "auto_backup"
        const val KEY_CAPTURES = "backup_captures"
        const val KEY_LAST_BACKUP = "last_backup_at"
    }
}
