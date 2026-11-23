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
            "https://dav.example.com/dav/aiaccounting-backup.csv",
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
        server.enqueue(MockResponse().setResponseCode(201))
        val client = WebDavClient(config())
        val csv = "日期,类型,金额\n2026-11-23 09:30,支出,52.50\n"

        client.upload(csv)

        val request = server.takeRequest()
        assertEquals("PUT", request.method)
        assertEquals("/dav/aiaccounting-backup.csv", request.path)
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
        server.enqueue(MockResponse().setResponseCode(404))

        val error = assertThrows(IllegalStateException::class.java) {
            WebDavClient(config()).upload("x")
        }
        assertTrue("错误信息应提到地址", error.message!!.contains("服务器地址不对"))
    }
}
