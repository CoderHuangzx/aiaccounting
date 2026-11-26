package com.aiaccounting.data

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.aiaccounting.data.local.AppDatabase
import com.aiaccounting.data.local.CategoryEntity
import com.aiaccounting.domain.TxType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** 两级分类：内置子分类、自建子分类、删除保护。 */
@RunWith(AndroidJUnit4::class)
class SubCategoryTest {

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

    private fun parent(name: String, kind: TxType = TxType.EXPENSE): CategoryEntity =
        runBlocking { repository.categoriesOf(kind) }.first { it.name == name && it.parentId == null }

    @Test
    fun 首次启动会写入内置子分类() = runBlocking {
        val food = parent("餐饮")
        val children = repository.childrenOf(food.id)
        assertTrue("餐饮应有子分类", children.isNotEmpty())
        assertTrue("应包含午餐", children.any { it.name == "午餐" })
        assertTrue("子分类都是内置", children.all { it.isBuiltIn })
        assertTrue("子分类挂在对的父级下", children.all { it.parentId == food.id })
    }

    @Test
    fun 重复初始化不会重复写子分类() = runBlocking {
        val before = repository.categoriesOf(TxType.EXPENSE).size
        repository.ensureSeeded()
        repository.ensureSeeded()
        assertEquals(before, repository.categoriesOf(TxType.EXPENSE).size)
    }

    @Test
    fun 可以给一级分类加子分类() = runBlocking {
        val food = parent("餐饮")
        val created = repository.addCategory("夜宵", "🌙", TxType.EXPENSE, food.id)
        assertNotNull(created)
        assertEquals(food.id, created!!.parentId)
        assertTrue(repository.childrenOf(food.id).any { it.name == "夜宵" })
    }

    @Test
    fun 同一父级下不能重名_不同父级可以() = runBlocking {
        val food = parent("餐饮")
        val transport = parent("交通")
        assertNull(repository.addCategory("午餐", "🍚", TxType.EXPENSE, food.id))   // 已有同名
        assertNotNull(repository.addCategory("午餐", "🍚", TxType.EXPENSE, transport.id))
    }

    @Test
    fun 一级分类之间也不能重名() = runBlocking {
        assertNull(repository.addCategory("餐饮", "🍜", TxType.EXPENSE, null))
    }

    @Test
    fun 有子分类的一级分类不能删() = runBlocking {
        // 用自建的一级分类来测：内置分类会先被「不能删」拦住
        val custom = repository.addCategory("夜宵档", "🌃", TxType.EXPENSE, null)!!
        repository.addCategory("烤串", "🍢", TxType.EXPENSE, custom.id)

        val result = repository.deleteCategory(custom.id)
        assertTrue(result is AccountingRepository.DeleteCategoryResult.HasChildren)
        assertEquals(1, (result as AccountingRepository.DeleteCategoryResult.HasChildren).count)
    }

    @Test
    fun 子分类有记录时不能删() = runBlocking {
        val food = parent("餐饮")
        val child = repository.addCategory("夜宵", "🌙", TxType.EXPENSE, food.id)!!
        repository.addManualTransaction(
            type = TxType.EXPENSE,
            amountCents = 2000,
            categoryId = child.id,
            accountId = 1,
            occurredAt = 1_700_000_000_000,
        )

        val result = repository.deleteCategory(child.id)
        assertTrue(result is AccountingRepository.DeleteCategoryResult.InUse)
        assertEquals(1, (result as AccountingRepository.DeleteCategoryResult.InUse).count)
    }

    @Test
    fun 没记录的自建子分类可以删() = runBlocking {
        val food = parent("餐饮")
        val child = repository.addCategory("夜宵", "🌙", TxType.EXPENSE, food.id)!!
        val result = repository.deleteCategory(child.id)
        assertTrue(result is AccountingRepository.DeleteCategoryResult.Deleted)
        assertTrue(repository.childrenOf(food.id).none { it.id == child.id })
    }

    @Test
    fun 记账挂到子分类后能带出一级分类() = runBlocking {
        val food = parent("餐饮")
        val lunch = repository.childrenOf(food.id).first { it.name == "午餐" }
        repository.addManualTransaction(
            type = TxType.EXPENSE,
            amountCents = 1800,
            categoryId = lunch.id,
            accountId = 1,
            occurredAt = System.currentTimeMillis(),
        )

        val tx = repository.observeTransactions().first().first()
        assertEquals("午餐", tx.categoryName)
        assertEquals("餐饮", tx.parentCategoryName)
    }
}
