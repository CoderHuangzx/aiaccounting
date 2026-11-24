package com.aiaccounting.data.webdav

import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

/**
 * 极简 WebDAV 客户端：只做「整个文件 PUT / GET」，够备份用，也不引任何三方库。
 *
 * 不依赖 Android API，可以直接在 JVM 单元测试里对着本地 HTTP 服务跑。
 *
 * 踩过的坑（都已处理）：
 * - 不设 Content-Length 时 Android 会走 chunked，部分 WebDAV 服务直接拒收 → 改固定长度；
 * - HttpURLConnection 对 PUT 不会自动跟随 30x → 手动跟一次；
 * - 失败时服务器其实会返回一段说明，不看就永远是「上传失败」→ 读出来附在消息后面。
 */
class WebDavClient(
    private val config: WebDavConfig,
    private val timeoutMillis: Int = 15_000,
) {

    data class ProbeResult(val ok: Boolean, val message: String)

    /** 上传备份文件；失败抛异常，调用方用 [uploadResult] 拿 Result。 */
    fun upload(content: String) {
        val bytes = content.toByteArray(Charsets.UTF_8)
        // 坚果云等服务的根目录不允许直接建文件，所以先确保子目录存在（已存在会返回 405）
        ensureFolder()
        var connection = open("PUT", config.backupUrl()) ?: error("地址不合法")
        try {
            connection.setFixedLengthStreamingMode(bytes.size)
            connection.setRequestProperty("Content-Type", "text/csv; charset=utf-8")
            // 注意：不要再手动设 Content-Length，setFixedLengthStreamingMode 会自己写，
            // 手动加会导致重复头，服务端等不到结束而超时。
            connection.setRequestProperty("Expect", "")
            connection.doOutput = true
            connection.outputStream.use { it.write(bytes) }

            var code = connection.responseCode
            // PUT 的 30x 需要手动跟：拿到 Location 再发一次
            if (code in 301..308) {
                val location = connection.getHeaderField("Location")
                connection.disconnect()
                require(!location.isNullOrBlank()) { "上传失败：服务器要求跳转但没给出地址（HTTP $code）" }
                val target = if (location.startsWith("http")) {
                    location
                } else {
                    URL(URL(config.backupUrl()), location).toString()
                }
                connection = open("PUT", target) ?: error("地址不合法")
                connection.setFixedLengthStreamingMode(bytes.size)
                connection.setRequestProperty("Content-Type", "text/csv; charset=utf-8")
                connection.setRequestProperty("Content-Length", bytes.size.toString())
                connection.doOutput = true
                connection.outputStream.use { it.write(bytes) }
                code = connection.responseCode
            }

            if (code !in 200..299) {
                error(describeFailure(connection, code, "上传"))
            }
        } finally {
            runCatching { connection.disconnect() }
        }
    }

    /** 下载备份文件；服务器上没有备份（404）时返回 null。 */
    fun download(): String? {
        val connection = open("GET", config.backupUrl()) ?: error("地址不合法")
        return try {
            val code = connection.responseCode
            when {
                code == HttpURLConnection.HTTP_NOT_FOUND -> null
                code in 200..299 -> connection.inputStream.bufferedReader(Charsets.UTF_8).readText()
                else -> error(describeFailure(connection, code, "下载"))
            }
        } finally {
            connection.disconnect()
        }
    }

    /**
     * 连接自检：只发一个 GET，按状态码给结论。
     * 用来把「账号错 / 地址错 / 没有文件」三种情况区分开——否则用户只能看到「上传失败」。
     */
    fun probe(): ProbeResult {
        if (!config.isConfigured) {
            return ProbeResult(false, "还没填完整：地址、账号、应用密码都要有")
        }
        val connection = open("GET", config.backupUrl())
            ?: return ProbeResult(false, "地址不合法，要以 http:// 或 https:// 开头")
        return try {
            when (val code = connection.responseCode) {
                HttpURLConnection.HTTP_OK -> ProbeResult(true, "连接成功，云端已有备份文件")
                HttpURLConnection.HTTP_NOT_FOUND -> ProbeResult(true, "连接成功，云端还没有备份（首次上传会新建）")
                HttpURLConnection.HTTP_UNAUTHORIZED -> ProbeResult(false, "账号或应用密码不对（401）")
                HttpURLConnection.HTTP_FORBIDDEN -> ProbeResult(false, "没有权限（403），检查地址是否以 /dav/ 结尾")
                405 -> ProbeResult(false, "服务器不允许读取（405）")
                else -> ProbeResult(false, "连接失败：HTTP $code")
            }
        } catch (error: Exception) {
            ProbeResult(false, "连接失败：${error.message ?: error.javaClass.simpleName}")
        } finally {
            runCatching { connection.disconnect() }
        }
    }

    fun uploadResult(content: String): Result<Unit> = runCatching { upload(content) }

    fun downloadResult(): Result<String?> = runCatching { download() }

    /**
     * 确保备份目录存在。
     *
     * MKCOL 在目录已存在时返回 405，服务器不允许建目录时可能返回 403/409，
     * 这些都不算致命——真正的结论交给后面的 PUT。只有认证失败要立刻报出来。
     */
    private fun ensureFolder() {
        if (!config.isConfigured) return
        when (val code = mkcol(config.folderUrl())) {
            401 -> error("上传失败：账号或应用密码不对（401）")
            // 201 新建成功；405 已存在；其它（403/409 等）交给后面的 PUT 给结论
            else -> Unit
        }
    }

    /**
     * 发一个 MKCOL。HttpURLConnection 不允许非标准方法（会抛 Invalid HTTP method），
     * 所以这里直接用 socket 拼请求——只为了建目录这一件事。
     */
    private fun mkcol(url: String): Int {
        val target = runCatching { URL(url) }.getOrNull() ?: return -1
        val isHttps = target.protocol.equals("https", true)
        val host = target.host
        val port = if (target.port != -1) target.port else if (isHttps) 443 else 80
        val path = target.file.ifBlank { "/" }

        return runCatching {
            val raw = Socket()
            raw.connect(InetSocketAddress(host, port), timeoutMillis)
            val socket: Socket = if (isHttps) {
                val factory = SSLSocketFactory.getDefault() as SSLSocketFactory
                factory.createSocket(raw, host, port, true).also {
                    (it as SSLSocket).startHandshake()
                }
            } else {
                raw
            }
            socket.soTimeout = timeoutMillis

            val request = buildString {
                append("MKCOL ").append(path).append(" HTTP/1.1\r\n")
                append("Host: ").append(host).append("\r\n")
                append("Authorization: ").append(config.authHeader()).append("\r\n")
                append("Content-Length: 0\r\n")
                append("Connection: close\r\n\r\n")
            }
            socket.getOutputStream().apply {
                write(request.toByteArray(Charsets.UTF_8))
                flush()
            }
            // 必须把响应读完再关连接：只读状态行就 close，服务端会认为响应没发完，
            // 后续请求容易被挂住（MockWebServer 上复现为读超时）。
            val reader = socket.getInputStream().bufferedReader(Charsets.ISO_8859_1)
            val statusLine = reader.readLine()
            runCatching { reader.readText() }
            socket.close()
            statusLine?.split(" ")?.getOrNull(1)?.toIntOrNull() ?: -1
        }.getOrDefault(-1)
    }

    private fun open(method: String, url: String): HttpURLConnection? {
        if (!config.isConfigured) return null
        val connection = runCatching { URL(url).openConnection() as HttpURLConnection }.getOrNull() ?: return null
        return connection.apply {
            requestMethod = method
            connectTimeout = timeoutMillis
            readTimeout = timeoutMillis
            instanceFollowRedirects = false
            setRequestProperty("Authorization", config.authHeader())
            setRequestProperty("Accept", "*/*")
            setRequestProperty("User-Agent", "aiaccounting")
        }
    }

    /** 把状态码翻译成人话，并带上服务器返回的那段说明（截断到 160 字）。 */
    private fun describeFailure(connection: HttpURLConnection, code: Int, action: String): String {
        val body = runCatching {
            (connection.errorStream ?: connection.inputStream)
                ?.bufferedReader(Charsets.UTF_8)
                ?.use { it.readText().take(160).replace(Regex("\\s+"), " ").trim() }
        }.getOrNull().orEmpty()

        val hint = when (code) {
            301, 302, 307, 308 -> "服务器要求跳转，检查地址是否应为 https"
            401 -> "账号或应用密码不对"
            403 -> "没有权限，检查地址是否以 /dav/ 结尾"
            404 -> "服务器地址不对（通常是少了 /dav/ 之类的路径）；" +
                "如果地址正确，可能是服务端不允许在根目录建文件（坚果云需要子目录）"
            409 -> "目标目录不存在"
            411 -> "服务器要求 Content-Length"
            413 -> "文件太大"
            423 -> "文件被锁定"
            507 -> "网盘空间不足"
            else -> "HTTP $code"
        }
        return buildString {
            append("$action 失败：$hint")
            if (body.isNotEmpty()) append("（服务器返回：$body）")
        }
    }
}
