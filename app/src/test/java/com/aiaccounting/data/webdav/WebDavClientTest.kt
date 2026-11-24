package com.aiaccounting.data.webdav

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Base64

/**
 * 用 MockWebServer 起一个假的 WebDAV，验证 PUT/GET、认证头与错误提示。
 */
class WebDavClientTest {

    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun config(
        base: String = server.url("/dav/").toString(),
        user: String = "me@example.com",
        pass: String = "app-password",
    ) = WebDavConfig(baseUrl = base, username = user, password = pass)

    // ── 配置 ────────────────────────────────────────────────────────────

    @Test
    fun `baseUrl 缺少结尾斜杠时自动补上`() {
        assertEquals("https://dav.example.com/dav/", config(base = "https://dav.example.com/dav").normalizedBase())
        assertEquals("https://dav.example.com/dav/", config(base = "https://dav.example.com/dav/").normalizedBase())
        assertEquals(
            "https://dav.example.com/dav/aiaccounting/aiaccounting-backup.csv",
            config(base = " https://dav.example.com/dav ").backupUrl(),
        )
    }

    @Test
    fun `认证头是标准的 Basic base64`() {
        val expected = "Basic " + Base64.getEncoder()
            .encodeToString("me@example.com:app-password".toByteArray())
        assertEquals(expected, config().authHeader())
    }

    @Test
    fun `地址不是 http 或 https 视为未配置`() {
        assertTrue(!config(base = "dav.example.com/dav").isConfigured)
        assertTrue(config().isConfigured)
    }

    // ── 上传 / 下载 ──────────────────────────────────────────────────────

    @Test
    fun `上传的是 PUT 且带认证头`() {
        server.enqueue(MockResponse().setResponseCode(201))   // MKCOL 建目录
        server.enqueue(MockResponse().setResponseCode(201))   // PUT
        val client = WebDavClient(config())
        val csv = "日期,类型,金额\n2026-11-23 09:30,支出,52.50\n"

        client.upload(csv)

        val mkcol = server.takeRequest()
        assertEquals("MKCOL", mkcol.method)
        assertEquals("/dav/aiaccounting/", mkcol.path)

        val request = server.takeRequest()
        assertEquals("PUT", request.method)
        assertEquals("/dav/aiaccounting/aiaccounting-backup.csv", request.path)
        assertEquals(
            "Basic " + Base64.getEncoder().encodeToString("me@example.com:app-password".toByteArray()),
            request.getHeader("Authorization"),
        )
        assertEquals(csv, request.body.readUtf8())
    }

    @Test
    fun `下载能拿到文件内容`() {
        val csv = "日期,类型,金额\n2026-11-23 09:30,支出,52.50\n"
        server.enqueue(MockResponse().setResponseCode(200).setBody(csv))

        assertEquals(csv, WebDavClient(config()).download())
        assertEquals("GET", server.takeRequest().method)
    }

    @Test
    fun `服务器上没有备份时返回 null`() {
        server.enqueue(MockResponse().setResponseCode(404))
        assertNull(WebDavClient(config()).download())
    }

    @Test
    fun `未配置时上传返回失败结果`() {
        val result = WebDavClient(config(base = "", user = "", pass = "")).uploadResult("x")
        assertTrue(result.isFailure)
    }

    @Test
    fun `401 时提示账号或应用密码不对`() {
        server.enqueue(MockResponse().setResponseCode(401))

        val error = assertThrows(IllegalStateException::class.java) {
            WebDavClient(config()).upload("x")
        }
        assertTrue("错误信息应提到账号密码", error.message!!.contains("账号或应用密码不对"))
    }

    @Test
    fun `404 上传时提示地址可能不对`() {
        server.enqueue(MockResponse().setResponseCode(405))   // 目录已存在
        server.enqueue(MockResponse().setResponseCode(404))

        val error = assertThrows(IllegalStateException::class.java) {
            WebDavClient(config()).upload("x")
        }
        assertTrue("错误信息应提到地址", error.message!!.contains("服务器地址不对"))
    }

    // ── 连接自检 ────────────────────────────────────────────────────────

    @Test
    fun `自检_已有备份`() {
        server.enqueue(MockResponse().setResponseCode(200).setBody("日期,类型\n"))
        val result = WebDavClient(config()).probe()
        assertTrue(result.ok)
        assertTrue(result.message.contains("已有备份"))
    }

    @Test
    fun `自检_云端还没有备份也算连通`() {
        server.enqueue(MockResponse().setResponseCode(404))
        val result = WebDavClient(config()).probe()
        assertTrue(result.ok)
        assertTrue(result.message.contains("还没有备份"))
    }

    @Test
    fun `自检_账号密码错误`() {
        server.enqueue(MockResponse().setResponseCode(401))
        val result = WebDavClient(config()).probe()
        assertTrue(!result.ok)
        assertTrue(result.message.contains("401"))
    }

    @Test
    fun `自检_没填完整时直接提示`() {
        val result = WebDavClient(config(base = "", user = "", pass = "")).probe()
        assertTrue(!result.ok)
        assertTrue(result.message.contains("地址"))
    }

    @Test
    fun `上传失败时会带上服务器返回的说明`() {
        server.enqueue(MockResponse().setResponseCode(405))
        server.enqueue(MockResponse().setResponseCode(403).setBody("<html>Access denied by policy</html>"))

        val error = assertThrows(IllegalStateException::class.java) {
            WebDavClient(config()).upload("x")
        }
        assertTrue(error.message!!.contains("403") || error.message!!.contains("没有权限"))
        assertTrue("应带上服务器原文", error.message!!.contains("Access denied"))
    }

    @Test
    fun `上传请求带 Content-Length 而不是 chunked`() {
        server.enqueue(MockResponse().setResponseCode(405))   // MKCOL：目录已存在
        server.enqueue(MockResponse().setResponseCode(201))   // PUT
        val csv = "a,b\n1,2\n"
        WebDavClient(config()).upload(csv)

        server.takeRequest()   // MKCOL
        val request = server.takeRequest()
        assertEquals(csv.toByteArray().size.toString(), request.getHeader("Content-Length"))
        assertNull(request.getHeader("Transfer-Encoding"))
    }
}