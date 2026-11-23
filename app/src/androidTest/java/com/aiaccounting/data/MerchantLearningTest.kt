package com.aiaccounting.data

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.aiaccounting.data.local.AccountEntity
import com.aiaccounting.data.local.AppDatabase
import com.aiaccounting.data.local.CategoryEntity
import com.aiaccounting.data.local.RawCaptureEntity
import com.aiaccounting.data.local.TransactionEntity
import com.aiaccounting.domain.BillRow
import com.aiaccounting.domain.TxSource
import com.aiaccounting.domain.TxType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** 商户 → 分类自学习。 */
@RunWith(AndroidJUnit4::class)
class MerchantLearningTest {

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

    private fun addFoodShop(merchant: String = "瑞幸咖啡"): Long = runBlocking {
        repository.addCapturedTransaction(
            type = TxType.EXPENSE,
            amountCents = 1200,
            categoryId = 10,   // 其他
            accountId = 1,
            occurredAt = 1_700_000_000_000,
            merchant = merchant,
        )
    }

    @Test
    fun 改分类会记住这个商户() = runBlocking {
        val id = addFoodShop()
        assertEquals(0, repository.merchantRuleCount())

        repository.updateTransaction(
            id = id,
            type = TxType.EXPENSE,
            amountCents = 1200,
            categoryId = 1,   // 餐饮
            accountId = 1,
            occurredAt = 1_700_000_000_000,
        )

        assertEquals(1, repository.merchantRuleCount())
        assertEquals(1L, repository.resolveCategoryId("瑞幸咖啡", TxType.EXPENSE))
    }

    @Test
    fun 编辑不会把商户名清空() = runBlocking {
        val id = addFoodShop("盒马鲜生")

        repository.updateTransaction(
            id = id,
            type = TxType.EXPENSE,
            amountCents = 8840,
            categoryId = 4,
            accountId = 1,
            occurredAt = 1_700_000_000_000,
            note = "周末采购",
        )

        val saved = db.transactionDao().findById(id)
        assertNotNull(saved)
        assertEquals("盒马鲜生", saved!!.merchant)
    }

    @Test
    fun 分支店名也能命中已学规则() = runBlocking {
        val id = addFoodShop("瑞幸咖啡")
        repository.updateTransaction(
            id = id,
            type = TxType.EXPENSE,
            amountCents = 1200,
            categoryId = 1,
            accountId = 1,
            occurredAt = 1_700_000_000_000,
        )

        // 同一家的另一家分店，带括号和地名
        assertEquals(1L, repository.resolveCategoryId("瑞幸咖啡（南京新街口店）", TxType.EXPENSE))
    }

    @Test
    fun 账单导入会使用学到的分类() = runBlocking {
        repository.saveMerchantRule("瑞幸咖啡", 1L)

        repository.applyBillImport(
            listOf(
                BillRow(
                    occurredAt = 1_700_000_000_000,
                    amountCents = 1200,
                    type = TxType.EXPENSE,
                    counterparty = "瑞幸咖啡（首蓿园店）",
                    description = "拿铁",
                    tradeNo = "T1",
                ),
            ),
        )

        val tx = repository.observeTransactions().first().single()
        assertEquals("餐饮", tx.categoryName)
    }

    @Test
    fun 学到的分类不会串到收入上() = runBlocking {
        repository.saveMerchantRule("某商户", 1L)   // 1 = 餐饮，属于支出分类

        // 收入记录不应该用上支出分类
        val resolved = repository.resolveCategoryId("某商户", TxType.INCOME)
        assertEquals("其他", db.categoryDao().all().first { it.id == resolved }.name)
    }
}
