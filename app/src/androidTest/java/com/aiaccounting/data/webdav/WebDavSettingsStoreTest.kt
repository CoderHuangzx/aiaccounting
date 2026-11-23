package com.aiaccounting.data.webdav

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WebDavSettingsStoreTest {

    private lateinit var store: WebDavSettingsStore

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setUp() {
        store = WebDavSettingsStore(context)
        store.clear()
    }

    @After
    fun tearDown() {
        store.clear()
    }

    @Test
    fun 默认是空配置() {
        val config = store.load()
        assertFalse(config.isConfigured)
        assertFalse(config.autoBackup)
    }

    @Test
    fun 保存后能原样读回来() {
        store.save(
            WebDavConfig(
                baseUrl = "https://dav.jianguoyun.com/dav/",
                username = "me@example.com",
                password = "app-pass",
                autoBackup = true,
            ),
        )

        val loaded = store.load()
        assertTrue(loaded.isConfigured)
        assertEquals("https://dav.jianguoyun.com/dav/", loaded.baseUrl)
        assertEquals("me@example.com", loaded.username)
        assertEquals("app-pass", loaded.password)
        assertTrue(loaded.autoBackup)
    }

    @Test
    fun 自动备份开关可以单独改() {
        store.save(
            WebDavConfig(
                baseUrl = "https://dav.example.com/dav/",
                username = "u",
                password = "p",
                autoBackup = false,
            ),
        )

        store.setAutoBackup(true)

        assertTrue(store.load().autoBackup)
        // 其他字段不能被顺手清掉
        assertEquals("https://dav.example.com/dav/", store.load().baseUrl)
    }
}
