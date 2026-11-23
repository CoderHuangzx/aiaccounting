package com.aiaccounting.data

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.aiaccounting.data.local.AppDatabase
import com.aiaccounting.domain.TxType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 编辑 / 删除记录的仓库层测试，用内存 Room 跑在设备上。
 * 重点验证：改完字段后来源与去重指纹不被破坏，删除只影响目标行。
 */
@RunWith(AndroidJUnit4::class)
class RecordEditingTest {

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
    fun 编辑金额分类时间后其余字段保持原样() = runBlocking {
        val id = repository.addCapturedTransaction(
            type = TxType.EXPENSE,
            amountCents = 2350,
            categoryId = 1,
            accountId = 1,
            occurredAt = 1_700_000_000_000,
            merchant = "美团外卖",
            source = com.aiaccounting.domain.TxSource.NOTIFICATION,
            tradeNo = "2026112200012345",
        )

        repository.updateTransaction(
            id = id,
            type = TxType.EXPENSE,
            amountCents = 1980,
            categoryId = 3,
            accountId = 2,
            occurredAt = 1_700_000_600_000,
            note = "改成购物",
        )

        val updated = db.transactionDao().findById(id)
        assertNotNull(updated)
        assertEquals(1980L, updated!!.amountCents)
        assertEquals(3L, updated.categoryId)
        assertEquals(2L, updated.accountId)
        assertEquals(1_700_000_600_000, updated.occurredAt)
        assertEquals("改成购物", updated.note)
        // 来源与去重指纹不能被编辑破坏，否则通知再推一次会重复入库
        assertEquals("NOTIFICATION", updated.source)
        assertNotNull(updated.dedupKey)
        assertEquals("PENDING", updated.status)
    }

    @Test
    fun 删除只移除目标记录() = runBlocking {
        val keepId = repository.addManualTransaction(
            type = TxType.EXPENSE,
            amountCents = 1000,
            categoryId = 1,
            accountId = 1,
        )
        val removeId = repository.addManualTransaction(
            type = TxType.EXPENSE,
            amountCents = 2000,
            categoryId = 2,
            accountId = 1,
        )
        assertEquals(2, repository.transactionCount())

        repository.deleteTransaction(removeId)

        assertEquals(1, repository.transactionCount())
        assertNull(db.transactionDao().findById(removeId))
        assertNotNull(db.transactionDao().findById(keepId))
    }

    @Test
    fun 编辑不存在的记录不会抛错也不新增() = runBlocking {
        repository.updateTransaction(
            id = 9999,
            type = TxType.INCOME,
            amountCents = 100,
            categoryId = 11,
            accountId = 1,
            occurredAt = 1_700_000_000_000,
        )
        assertEquals(0, repository.transactionCount())
    }

    @Test
    fun 编辑后列表流会返回新值() = runBlocking {
        val id = repository.addManualTransaction(
            type = TxType.EXPENSE,
            amountCents = 1000,
            categoryId = 1,
            accountId = 1,
        )
        repository.updateTransaction(
            id = id,
            type = TxType.EXPENSE,
            amountCents = 8888,
            categoryId = 2,
            accountId = 1,
            occurredAt = 1_700_000_000_000,
        )

        val txs = repository.observeTransactions().first()
        assertEquals(1, txs.size)
        assertEquals(8888L, txs.first().amountCents)
        assertEquals("交通", txs.first().categoryName)
    }
}
