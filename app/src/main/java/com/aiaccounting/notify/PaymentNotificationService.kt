package com.aiaccounting.notify

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.aiaccounting.AccountingApp
import com.aiaccounting.domain.PaymentParser
import com.aiaccounting.domain.TxSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 支付 / 购物通知的捕捉入口。
 *
 * 三件事：留档原文 → 规则解析 → 生成一条「待确认」记录。
 * 监听范围内的 App 即使解析失败也会留档（parsed=false + 原因），
 * 这样规则调不准时能拿真实文案回溯，而不是「为什么这笔没记上」查不清。
 */
class PaymentNotificationService : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val notification = sbn?.notification ?: return
        val packageName = sbn.packageName ?: return

        // 只处理名单里的 App（微信/支付宝/淘宝/京东/美团/抖音）
        if (!PaymentParser.isWatched(packageName)) return

        val extras = notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
        if (title.isNullOrBlank() && text.isNullOrBlank()) return

        val parsed = PaymentParser.parse(packageName, title, text)
        val repository = (application as AccountingApp).repository

        scope.launch {
            runCatching {
                repository.ensureSeeded()
                repository.recordRawCapture(
                    packageName = packageName,
                    title = title,
                    text = text,
                    parsed = parsed != null,
                    reason = if (parsed == null) "没匹配到支付成功规则" else null,
                )
                if (parsed == null) return@runCatching

                val accountId = parsed.accountName?.let { repository.accountIdByName(it) }
                    ?: repository.accountIdByName("微信")
                    ?: repository.accounts().firstOrNull()?.id
                // 商户之前学过分类就直接命中
                val categoryId = repository.resolveCategoryId(parsed.merchant, parsed.type)

                if (accountId != null && categoryId != null) {
                    repository.addCapturedTransaction(
                        type = parsed.type,
                        amountCents = parsed.amountCents,
                        categoryId = categoryId,
                        accountId = accountId,
                        occurredAt = System.currentTimeMillis(),
                        merchant = parsed.merchant,
                        note = null,
                        source = TxSource.NOTIFICATION,
                    )
                }
            }
        }
    }
}
