package com.aiaccounting.data

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.aiaccounting.data.local.AppDatabase
import com.aiaccounting.domain.AlipayBillParser
import com.aiaccounting.domain.TxType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.ZoneId

/** 账单导入的去重与落库。 */
@RunWith(AndroidJUnit4::class)
class BillImportTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: AccountingRepository

    private val zone: ZoneId = ZoneId.of("Asia/Shanghai")

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

    private fun millis(day: Int, hour: Int = 9): Long =
        LocalDate.of(2026, 11, day).atTime(hour, 0).atZone(zone).toInstant().toEpochMilli()

    private fun row(
        day: Int,
        amount: Long,
        type: TxType = TxType.EXPENSE,
        merchant: String = "瑞幸咖啡",
        tradeNo: String? = null,
        hour: Int = 9,
    ) = AlipayBillParser.BillRow(
        occurredAt = millis(day, hour),
        amountCents = amount,
        type = type,
        counterparty = merchant,
        description = merchant,
        tradeNo = tradeNo,
    )

    @Test
    fun 与手动记录同金额同时间的账单会被判重() = runBlocking {
        val at = millis(13, 9)
        repository.addManualTransaction(
            type = TxType.EXPENSE,
            amountCents = 1200,
            categoryId = 1,
            accountId = 1,
            occurredAt = at,
        )

        val rows = listOf(
            row(13, 1200, tradeNo = "T1"),
            row(14, 5000, merchant = "超市", tradeNo = "T2"),
        )

        val preview = repository.previewBillImport(rows)
        assertEquals(2, preview.total)
        assertEquals(1, preview.importable.size)
        assertEquals(1, preview.duplicates)

        val imported = repository.applyBillImport(preview.importable)
        assertEquals(1, imported)
        assertEquals(2, repository.transactionCount())
    }

    @Test
    fun 同一份账单导第二次不会再翻倍() = runBlocking {
        val rows = listOf(
            row(13, 1200, tradeNo = "T1"),
            row(14, 1860, merchant = "滴滴出行", tradeNo = "T2"),
        )

        assertEquals(2, repository.applyBillImport(rows))
        assertEquals(2, repository.transactionCount())

        val second = repository.previewBillImport(rows)
        assertEquals(0, second.importable.size)
        assertEquals(2, second.duplicates)
        assertEquals(0, repository.applyBillImport(second.importable))
        assertEquals(2, repository.transactionCount())
    }

    @Test
    fun 导入的记录来源是账单且已确认() = runBlocking {
        repository.applyBillImport(listOf(row(13, 1200, tradeNo = "T1")))

        val txs = repository.observeTransactions().first()
        assertEquals(1, txs.size)
        assertEquals(com.aiaccounting.domain.TxSource.IMPORT, txs.first().source)
        assertEquals("瑞幸咖啡", txs.first().merchant)
        assertEquals("支付宝", txs.first().accountName)
    }

    @Test
    fun 收入行会落到收入分类() = runBlocking {
        repository.applyBillImport(
            listOf(row(16, 800000, type = TxType.INCOME, merchant = "工资", tradeNo = "T3")),
        )

        val txs = repository.observeTransactions().first()
        assertEquals(TxType.INCOME, txs.first().type)
        assertEquals("其他", txs.first().categoryName)
        assertTrue(txs.first().amountCents == 800000L)
    }

    @Test
    fun 空列表什么都不做() = runBlocking {
        assertEquals(0, repository.applyBillImport(emptyList()))
        assertEquals(0, repository.transactionCount())
    }
}
