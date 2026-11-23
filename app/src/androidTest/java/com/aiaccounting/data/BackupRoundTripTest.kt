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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** 备份往返：导出 → 清空 → 导入，数据要能完整回来。 */
@RunWith(AndroidJUnit4::class)
class BackupRoundTripTest {

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
    fun 导出再导入数据完整回来() = runBlocking {
        repository.addManualTransaction(
            type = TxType.EXPENSE,
            amountCents = 5250,
            categoryId = 1,
            accountId = 1,
            occurredAt = 1_700_000_000_000,
            note = "午饭",
        )
        repository.addManualTransaction(
            type = TxType.INCOME,
            amountCents = 800000,
            categoryId = 11,
            accountId = 2,
            occurredAt = 1_700_090_000_000,
        )

        val csv = repository.exportBackupCsv()
        val exported = csv.lineSequence().count { it.isNotBlank() } - 1
        assertEquals(2, exported)

        // 清空后恢复
        db.transactionDao().deleteAll()
        assertEquals(0, repository.transactionCount())

        val restored = repository.importBackupCsv(csv)
        assertEquals(2, restored)

        val txs = repository.observeTransactions().first()
        assertEquals(2, txs.size)
        val expense = txs.first { it.type == TxType.EXPENSE }
        assertEquals(5250L, expense.amountCents)
        assertEquals("餐饮", expense.categoryName)
        assertEquals("微信", expense.accountName)
        assertEquals("午饭", expense.note)
        assertTrue(txs.any { it.type == TxType.INCOME && it.amountCents == 800000L })
    }

    @Test
    fun 导入是覆盖而不是追加() = runBlocking {
        repository.addManualTransaction(
            type = TxType.EXPENSE,
            amountCents = 1000,
            categoryId = 1,
            accountId = 1,
        )
        val csv = repository.exportBackupCsv()

        repository.addManualTransaction(
            type = TxType.EXPENSE,
            amountCents = 2000,
            categoryId = 2,
            accountId = 1,
        )
        assertEquals(2, repository.transactionCount())

        val restored = repository.importBackupCsv(csv)
        assertEquals(1, restored)
        assertEquals(1, repository.transactionCount())
    }

    @Test
    fun 空备份导入后清空所有记录() = runBlocking {
        repository.addManualTransaction(
            type = TxType.EXPENSE,
            amountCents = 1000,
            categoryId = 1,
            accountId = 1,
        )
        val emptyCsv = "日期,类型,金额,分类,账户,商户,备注,来源,指纹\n"

        val restored = repository.importBackupCsv(emptyCsv)

        assertEquals(0, restored)
        assertEquals(0, repository.transactionCount())
    }
}
