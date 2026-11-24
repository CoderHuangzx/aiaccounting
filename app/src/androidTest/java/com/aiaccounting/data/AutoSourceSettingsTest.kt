package com.aiaccounting.data

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.aiaccounting.domain.PaymentParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AutoSourceSettingsTest {

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private lateinit var store: AutoSourceSettings

    @Before
    fun setUp() {
        store = AutoSourceSettings(context)
        store.setAll(true)
    }

    @Test
    fun 默认全部开启() {
        store.setAll(true)
        AutoSource.entries.forEach { assertTrue("${it.label} 应默认开启", store.isEnabled(it)) }
    }

    @Test
    fun 可以单独关闭某个来源() {
        store.setEnabled(AutoSource.WECHAT, false)
        assertFalse(store.isEnabled(AutoSource.WECHAT))
        assertTrue(store.isEnabled(AutoSource.ALIPAY))
    }

    @Test
    fun 通知渠道能映射到对应开关() {
        store.setEnabled(AutoSource.ALIPAY, false)
        assertFalse(store.isChannelEnabled(PaymentParser.Channel.ALIPAY))
        assertTrue(store.isChannelEnabled(PaymentParser.Channel.WECHAT))
        // 未知渠道不放行
        assertFalse(store.isChannelEnabled(PaymentParser.Channel.UNKNOWN))
    }

    @Test
    fun 全部关闭与全部开启() {
        store.setAll(false)
        AutoSource.entries.forEach { assertFalse(store.isEnabled(it)) }

        store.setAll(true)
        AutoSource.entries.forEach { assertTrue(store.isEnabled(it)) }
    }

    @Test
    fun 渠道映射覆盖六个应用() {
        assertEquals(AutoSource.WECHAT, autoSourceOf(PaymentParser.Channel.WECHAT))
        assertEquals(AutoSource.ALIPAY, autoSourceOf(PaymentParser.Channel.ALIPAY))
        assertEquals(AutoSource.TAOBAO, autoSourceOf(PaymentParser.Channel.TAOBAO))
        assertEquals(AutoSource.JD, autoSourceOf(PaymentParser.Channel.JD))
        assertEquals(AutoSource.MEITUAN, autoSourceOf(PaymentParser.Channel.MEITUAN))
        assertEquals(AutoSource.DOUYIN, autoSourceOf(PaymentParser.Channel.DOUYIN))
        assertNull(autoSourceOf(PaymentParser.Channel.UNKNOWN))
    }
}
