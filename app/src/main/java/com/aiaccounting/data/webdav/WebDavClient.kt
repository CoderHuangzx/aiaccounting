package com.aiaccounting.data.webdav

import java.net.HttpURLConnection
import java.net.URL

/**
 * 极简 WebDAV 客户端：只做「整个文件 PUT / GET」，够备份用，也不引任何三方库。
 *
 * 不依赖 Android API，可以直接在 JVM 单元测试里对着本地 HTTP 服务跑。
 */
class WebDavClient(
    private val config: WebDavConfig,
    private val timeoutMillis: Int = 15_000,
) {

    /** 上传备份文件；失败抛异常，调用方用 [uploadResult] 拿 Result。 */
    fun upload(content: String) {
        val connection = open("PUT") ?: error("地址不合法")
        try {
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "text/csv; charset=utf-8")
            connection.outputStream.use { it.write(content.toByteArray(Charsets.UTF_8)) }
            ensureSuccess(connection, "上传")
        } finally {
            connection.disconnect()
        }
    }

    /** 下载备份文件；服务器上没有备份（404）时返回 null。 */
    fun download(): String? {
        val connection = open("GET") ?: error("地址不合法")
        return try {
            val code = connection.responseCode
            when {
                code == HttpURLConnection.HTTP_NOT_FOUND -> null
                code in 200..299 -> connection.inputStream.bufferedReader(Charsets.UTF_8).readText()
                else -> error("下载失败：HTTP $code")
            }
        } finally {
            connection.disconnect()
        }
    }

    fun uploadResult(content: String): Result<Unit> = runCatching { upload(content) }

    fun downloadResult(): Result<String?> = runCatching { download() }

    private fun open(method: String): HttpURLConnection? {
        if (!config.isConfigured) return null
        val connection = URL(config.backupUrl()).openConnection() as? HttpURLConnection ?: return null
        return connection.apply {
            requestMethod = method
            connectTimeout = timeoutMillis
            readTimeout = timeoutMillis
            instanceFollowRedirects = true
            setRequestProperty("Authorization", config.authHeader())
            setRequestProperty("Accept", "*/*")
        }
    }

    private fun ensureSuccess(connection: HttpURLConnection, action: String) {
        val code = connection.responseCode
        if (code !in 200..299) {
            val hint = when (code) {
                401 -> "账号或应用密码不对"
                403 -> "没有权限"
                404 -> "服务器地址不对（通常是少了 /dav/ 之类的路径）"
                409 -> "目标目录不存在"
                else -> "HTTP $code"
            }
            error("$action 失败：$hint")
        }
    }
}
