package com.aiaccounting.data.webdav

import java.util.Base64

/**
 * WebDAV 配置。用「基础地址 + 固定文件名」的方式定位备份文件，
 * 不要求用户去理解路径拼接，也不依赖服务器上的目录是否存在。
 */
data class WebDavConfig(
    val baseUrl: String = "",
    val username: String = "",
    val password: String = "",
    val autoBackup: Boolean = false,
    val fileName: String = DEFAULT_FILE_NAME,
) {
    /** 补上结尾斜杠，避免拼出 `https://x/davaiaccounting.csv` 这种地址。 */
    fun normalizedBase(): String {
        val trimmed = baseUrl.trim()
        if (trimmed.isEmpty()) return ""
        return if (trimmed.endsWith("/")) trimmed else "$trimmed/"
    }

    fun backupUrl(): String = normalizedBase() + fileName

    /** HTTP Basic 认证头。 */
    fun authHeader(): String =
        "Basic " + Base64.getEncoder().encodeToString("$username:$password".toByteArray(Charsets.UTF_8))

    /** 地址看起来像不像一个 WebDAV 服务地址。 */
    fun hasValidUrl(): Boolean =
        baseUrl.trim().let { it.startsWith("http://") || it.startsWith("https://") }

    val isConfigured: Boolean
        get() = hasValidUrl() && username.isNotBlank() && password.isNotBlank()

    companion object {
        const val DEFAULT_FILE_NAME = "aiaccounting-backup.csv"
    }
}
