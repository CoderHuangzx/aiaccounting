package com.aiaccounting.bubble

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BubbleSettingsStoreTest {

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private lateinit var store: BubbleSettingsStore

    @Before
    fun setUp() {
        store = BubbleSettingsStore(context)
        store.setEnabled(false)
    }

    @Test
    fun 默认是关闭的() {
        store.setEnabled(false)
        assertFalse(store.isEnabled())
    }

    @Test
    fun 开启后能读回() {
        store.setEnabled(true)
        assertTrue(store.isEnabled())
    }

    @Test
    fun 关闭后能读回() {
        store.setEnabled(true)
        store.setEnabled(false)
        assertFalse(store.isEnabled())
    }
}
