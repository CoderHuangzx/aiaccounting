package com.aiaccounting.data

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.aiaccounting.data.local.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 种子数据只能写一次。
 *
 * 真机上出现过 30 个分类（内置 15 个被写了两遍），原因是 Application、界面、
 * 后台服务并发调用 ensureSeeded 且没有加锁。
 */
@RunWith(AndroidJUnit4::class)
class SeedingTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: AccountingRepository

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = AccountingRepository(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun 并发初始化只会写一份种子数据() = runBlocking {
        val jobs = (1..12).map {
            async(Dispatchers.Default) { repository.ensureSeeded() }
        }
        jobs.awaitAll()

        assertEquals("分类应为内置的 15 个", 15, db.categoryDao().count())
        assertEquals("账户应为 4 个", 4, db.accountDao().count())
    }

    @Test
    fun 重复调用不会重复写入() = runBlocking {
        repeat(5) { repository.ensureSeeded() }
        assertEquals(15, db.categoryDao().count())
        assertEquals(4, db.accountDao().count())
    }

    @Test
    fun 已有数据时不会覆盖() = runBlocking {
        repository.ensureSeeded()
        repository.addCategory("奶茶", "🧋", com.aiaccounting.domain.TxType.EXPENSE)

        repository.ensureSeeded()

        assertTrue(
            "自建分类应保留",
            db.categoryDao().all().any { it.name == "奶茶" },
        )
        assertEquals(16, db.categoryDao().count())
    }
}
