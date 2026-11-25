package com.aiaccounting.data

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.aiaccounting.data.local.AppDatabase
import com.aiaccounting.domain.TxSource
import com.aiaccounting.domain.TxType
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 跨来源判重：同一笔订单被通知和读屏各抓一次时，只能留一条。
 *
 * 真机上出现过：美团 25.90 先由通知抓（商户「美团」），42 分钟后打开订单页，
 * 读屏又抓一次（商户为空）——指纹里的商户不同、时间也超出 10 分钟窗，于是重复。
 */
@RunWith(AndroidJUnit4::class)
class CaptureDedupeTest {

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

    private suspend fun capture(
        amount: Long,
        at: Long,
        merchant: String?,
        note: String? = null,
    ) = repository.addCapturedTransaction(
        type = TxType.EXPENSE,
        amountCents = amount,
        categoryId = 10,
        accountId = 1,
        occurredAt = at,
        merchant = merchant,
        note = note,
        source = TxSource.NOTIFICATION,
    )

    @Test
    fun 通知与读屏抓到同一笔只记一次() = runBlocking {
        val at = 1_795_514_540_421L

        capture(amount = 2590, at = at, merchant = "美团")                 // 通知
        capture(amount = 2590, at = at + 42 * 60_000L, merchant = null, note = "读屏：订单已完成")

        assertEquals(1, repository.transactionCount())
    }

    @Test
    fun 不同金额的两笔都保留() = runBlocking {
        val at = 1_795_514_540_421L
        capture(2590, at, "美团")
        capture(2190, at + 60_000, "美团")
        assertEquals(2, repository.transactionCount())
    }

    @Test
    fun 微信两条同金额通知只记一笔() = runBlocking {
        // 微信扫个人收款码：支付成功通知 + 对方收款通知，金额一样、商户文案不同
        val at = 1_795_514_540_421L
        capture(350, at, merchant = "微信支付")
        capture(350, at + 15_000, merchant = "只是不想失去您")
        assertEquals(1, repository.transactionCount())
    }

    @Test
    fun 超过两小时窗的同金额算两笔() = runBlocking {
        val at = 1_795_514_540_421L
        capture(2590, at, "美团")
        capture(2590, at + 3 * 60 * 60_000L, "美团")
        assertEquals(2, repository.transactionCount())
    }

    @Test
    fun 手动记的同金额不会被自动捕捉吞掉() = runBlocking {
        val at = 1_795_514_540_421L
        maybeManual(2590, at)
        capture(2590, at + 5 * 60_000L, "美团")
        // 手动那条不在判重范围内，自动捕捉应正常入库
        assertEquals(2, repository.transactionCount())
    }

    private suspend fun maybeManual(amount: Long, at: Long) {
        repository.addManualTransaction(
            type = TxType.EXPENSE,
            amountCents = amount,
            categoryId = 10,
            accountId = 1,
            occurredAt = at,
        )
    }
}
