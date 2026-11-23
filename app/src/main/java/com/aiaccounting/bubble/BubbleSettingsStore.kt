package com.aiaccounting.bubble

import android.content.Context

/** 悬浮球开关的持久化。 */
class BubbleSettingsStore(context: Context) {

    private val prefs = context.getSharedPreferences("bubble", Context.MODE_PRIVATE)

    fun isEnabled(): Boolean = prefs.getBoolean(KEY_ENABLED, false)

    fun setEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    private companion object {
        const val KEY_ENABLED = "enabled"
    }
}
