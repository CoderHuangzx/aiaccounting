package com.aiaccounting.data.webdav

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** 云备份配置的持久化，尤其是「备份捕捉日志」开关。 */
@RunWith(AndroidJUnit4::class)
class WebDavSettingsStoreTest {

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private lateinit var store: WebDavSettingsStore

    @Before
    fun setUp() {
        store = WebDavSettingsStore(context)
        store.clear()
    }

    @Test
    fun 保存后能读回全部字段() {
        store.save(
            WebDavConfig(
                baseUrl = "https://dav.jianguoyun.com/dav/",
                username = "me@example.com",
                password = "app-pass",
                autoBackup = true,
                backupCaptures = false,
            ),
        )

        val loaded = store.load()
        assertEquals("https://dav.jianguoyun.com/dav/", loaded.baseUrl)
        assertEquals("me@example.com", loaded.username)
        assertEquals("app-pass", loaded.password)
        assertTrue(loaded.autoBackup)
        assertFalse("捕捉日志开关应被持久化", loaded.backupCaptures)
    }

    @Test
    fun 捕捉日志默认开启() {
        store.clear()
        assertTrue(store.load().backupCaptures)
    }

    @Test
    fun 最近备份时间能读回() {
        assertTrue(store.lastBackupAt() == null)
        store.setLastBackupAt(1_800_000_000_000)
        assertEquals(1_800_000_000_000, store.lastBackupAt())
    }

    @Test
    fun 清空后回到未配置() {
        store.save(WebDavConfig(baseUrl = "https://x/dav/", username = "u", password = "p"))
        store.clear()
        assertEquals("", store.load().baseUrl)
        assertFalse(store.load().isConfigured)
    }
}
