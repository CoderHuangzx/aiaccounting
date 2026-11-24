package com.aiaccounting.data

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** 崩溃留档：写文件、读回来、保留上限、清空。 */
@RunWith(AndroidJUnit4::class)
class CrashStoreTest {

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setUp() {
        CrashStore.clear(context)
    }

    @Test
    fun 崩溃能写成本地文件并读回堆栈() {
        val file = CrashStore.record(context, IllegalStateException("测试用崩溃"))
        assertNotNull(file)
        assertTrue(file!!.exists())

        val text = CrashStore.read(file)
        assertTrue("应包含异常信息", text.contains("测试用崩溃"))
        assertTrue("应包含设备信息", text.contains("Android"))
        assertTrue("应包含版本", text.contains("版本"))
    }

    @Test
    fun 列表能拿到摘要() {
        CrashStore.record(context, NullPointerException("空指针测试"))
        val entries = CrashStore.list(context)
        assertEquals(1, entries.size)
        assertTrue("摘要应包含异常类型", entries.first().summary.contains("NullPointerException"))
    }

    @Test
    fun 最多保留十条() {
        repeat(12) { index ->
            CrashStore.record(context, RuntimeException("第 $index 次"))
        }
        assertTrue("超过上限应被裁剪", CrashStore.list(context).size <= 10)
    }

    @Test
    fun 清空后列表为空() {
        CrashStore.record(context, RuntimeException("x"))
        CrashStore.clear(context)
        assertTrue(CrashStore.list(context).isEmpty())
    }

    @Test
    fun 未读标记只消费一次() {
        CrashStore.record(context, RuntimeException("标记测试"))
        assertTrue("第一次应报告有未读崩溃", CrashStore.consumeUnseenFlag(context))
        assertFalse("第二次不应再报告", CrashStore.consumeUnseenFlag(context))
    }
}
