package com.aiaccounting.data.webdav

import java.util.Base64

/**
 * WebDAV 配置。用「基础地址 + 固定子目录 + 固定文件名」的方式定位备份文件。
 *
 * 为什么要有子目录：坚果云的 `/dav/` 根目录**不允许直接新建文件**
 * （PUT 会返回 404 ObjectNotFound），但允许 MKCOL 建子目录、在子目录里 PUT。
 * 所以备份统一放在 `<base>/aiaccounting/` 下，上传前自动建目录。
 */
data class WebDavConfig(
    val baseUrl: String = "",
    val username: String = "",
    val password: String = "",
    val autoBackup: Boolean = false,
    val fileName: String = DEFAULT_FILE_NAME,
    val folder: String = DEFAULT_FOLDER,
) {
    /** 补上结尾斜杠，避免拼出 `https://x/davaiaccounting.csv` 这种地址。 */
    fun normalizedBase(): String {
        val trimmed = baseUrl.trim()
        if (trimmed.isEmpty()) return ""
        return if (trimmed.endsWith("/")) trimmed else "$trimmed/"
    }

    /** 备份所在目录（上传前会先 MKCOL 建出来）。 */
    fun folderUrl(): String = normalizedBase() + folder.trim('/') + "/"

    fun backupUrl(): String = folderUrl() + fileName

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

        /** 备份子目录名。坚果云根目录不能直接放文件，必须有这一层。 */
        const val DEFAULT_FOLDER = "aiaccounting"
    }
}
