package com.aiaccounting.data

import android.content.Context
import android.os.Build
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 崩溃留档。
 *
 * App 崩了只在 logcat 里一闪而过，重装就没了——尤其自用/给朋友用的 APK，
 * 用户只能说出「它闪退了」。这里把堆栈连同设备信息写成本地文件，
 * 设置里可以查看、复制，出问题时能定位。
 *
 * 不联网、不上传，只写应用私有目录。
 */
object CrashStore {

    private const val DIR_NAME = "crashes"
    private const val MAX_KEPT = 10
    private const val PREFS = "crash_prefs"
    private const val KEY_UNSEEN = "has_unseen_crash"

    data class Entry(
        val file: File,
        val occurredAt: Long,
        val summary: String,
    )

    /** 在 Application 启动时安装；保留系统默认行为（会继续抛给系统，弹崩溃对话框）。 */
    fun install(context: Context) {
        val appContext = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching { record(appContext, throwable) }
            // 交回系统，保持原有崩溃处理（否则静默退出，用户以为闪了一下）
            previous?.uncaughtException(thread, throwable)
        }
    }

    /** 写一份崩溃记录（internal 方便测试）。 */
    internal fun record(context: Context, throwable: Throwable): File? {
        val dir = dir(context) ?: return null
        val now = System.currentTimeMillis()
        val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date(now))
        val file = File(dir, "crash-$stamp.txt")
        val text = buildString {
            appendLine("时间：${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(now))}")
            appendLine("版本：${appVersion(context)}")
            appendLine("设备：${Build.MANUFACTURER} ${Build.MODEL} / Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("线程：${Thread.currentThread().name}")
            appendLine()
            appendLine(throwable.stackTraceToString())
        }
        runCatching { file.writeText(text) }

        // 只保留最近 10 个
        runCatching {
            dir.listFiles()?.sortedByDescending { it.lastModified() }?.drop(MAX_KEPT)?.forEach { it.delete() }
        }

        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_UNSEEN, true)
            .apply()
        return file
    }

    fun list(context: Context): List<Entry> =
        dir(context)?.listFiles()
            ?.sortedByDescending { it.lastModified() }
            ?.mapNotNull { file ->
                val text = runCatching { file.readText() }.getOrNull() ?: return@mapNotNull null
                Entry(
                    file = file,
                    occurredAt = file.lastModified(),
                    summary = summarize(text),
                )
            }
            .orEmpty()

    fun read(file: File): String = runCatching { file.readText() }.getOrDefault("")

    fun clear(context: Context) {
        runCatching { dir(context)?.listFiles()?.forEach { it.delete() } }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_UNSEEN, false)
            .apply()
    }

    /** 上次崩溃还没被用户看过？启动时用来提示一句。 */
    fun consumeUnseenFlag(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val seen = !prefs.getBoolean(KEY_UNSEEN, false)
        if (!seen) prefs.edit().putBoolean(KEY_UNSEEN, false).apply()
        return !seen
    }

    private fun dir(context: Context): File? =
        runCatching {
            File(context.filesDir, DIR_NAME).apply { if (!exists()) mkdirs() }
        }.getOrNull()

    /** 摘要：异常类型 + 第一行信息，列表里够看。 */
    private fun summarize(text: String): String {
        val lines = text.lines()
        val exceptionLine = lines.firstOrNull { it.isNotBlank() && it.contains("Exception") || it.contains("Error") }
            ?: lines.firstOrNull { it.isNotBlank() }
            ?: return "未知异常"
        return exceptionLine.trim().take(120)
    }

    private fun appVersion(context: Context): String = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    }.getOrNull() ?: "unknown"
}
