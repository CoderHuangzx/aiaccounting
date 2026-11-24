package com.aiaccounting.data

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.aiaccounting.data.local.AppDatabase
import com.aiaccounting.domain.TxType
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** 分类的新增 / 改名 / 删除保护。 */
@RunWith(AndroidJUnit4::class)
class CategoryManagementTest {

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
        runBlocking { repository.ensureSeeded() }
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun 内置分类被标记为内置() = runBlocking {
        val expense = repository.categoriesOf(TxType.EXPENSE)
        assertTrue("种子分类应全部是内置", expense.all { it.isBuiltIn })
    }

    @Test
    fun 新增分类归到对应类型且不是内置() = runBlocking {
        val created = repository.addCategory("奶茶", "🧋", TxType.EXPENSE)
        assertNotNull(created)
        assertFalse(created!!.isBuiltIn)
        assertEquals(TxType.EXPENSE.name, created.kind)
        assertTrue(repository.categoriesOf(TxType.EXPENSE).any { it.name == "奶茶" })
        assertTrue(repository.categoriesOf(TxType.INCOME).none { it.name == "奶茶" })
    }

    @Test
    fun 重名或空名不能新建() = runBlocking {
        assertNull(repository.addCategory("餐饮", "🍚", TxType.EXPENSE))   // 与内置重名
        assertNull(repository.addCategory("   ", "🍚", TxType.EXPENSE))
        assertNull(repository.addCategory("", "🍚", TxType.EXPENSE))
    }

    @Test
    fun 可以改名和换图标() = runBlocking {
        val created = repository.addCategory("奶茶", "🧋", TxType.EXPENSE)!!
        assertTrue(repository.updateCategory(created.id, "咖啡", "☕"))
        val updated = repository.categoriesOf(TxType.EXPENSE).first { it.id == created.id }
        assertEquals("咖啡", updated.name)
        assertEquals("☕", updated.emoji)
    }

    @Test
    fun 内置分类不允许删除() = runBlocking {
        val food = repository.categoriesOf(TxType.EXPENSE).first { it.name == "餐饮" }
        val result = repository.deleteCategory(food.id)
        assertTrue(result is AccountingRepository.DeleteCategoryResult.BuiltIn)
        assertTrue(repository.categoriesOf(TxType.EXPENSE).any { it.id == food.id })
    }

    @Test
    fun 有记录的自建分类不能删() = runBlocking {
        val created = repository.addCategory("奶茶", "🧋", TxType.EXPENSE)!!
        repository.addManualTransaction(
            type = TxType.EXPENSE,
            amountCents = 1500,
            categoryId = created.id,
            accountId = 1,
            occurredAt = 1_700_000_000_000,
        )

        val result = repository.deleteCategory(created.id)
        assertTrue(result is AccountingRepository.DeleteCategoryResult.InUse)
        assertEquals(1, (result as AccountingRepository.DeleteCategoryResult.InUse).count)
        assertTrue(repository.categoriesOf(TxType.EXPENSE).any { it.id == created.id })
    }

    @Test
    fun 没记录的自建分类可以删() = runBlocking {
        val created = repository.addCategory("奶茶", "🧋", TxType.EXPENSE)!!
        val result = repository.deleteCategory(created.id)
        assertTrue(result is AccountingRepository.DeleteCategoryResult.Deleted)
        assertTrue(repository.categoriesOf(TxType.EXPENSE).none { it.id == created.id })
    }

    @Test
    fun 新分类沿用最大排序值() = runBlocking {
        val first = repository.addCategory("奶茶", "🧋", TxType.EXPENSE)!!
        val second = repository.addCategory("宠物", "🐶", TxType.EXPENSE)!!
        assertTrue(second.sortOrder > first.sortOrder)
    }
}
