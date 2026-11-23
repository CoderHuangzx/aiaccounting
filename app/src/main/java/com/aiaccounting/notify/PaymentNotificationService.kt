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
 * 微信 / 支付宝支付通知的捕捉入口。
 *
 * 只做三件事：留档原文 → 规则解析 → 生成一条「待确认」记录。
 * 解析失败的也会留档，方便之后对照真实文案调规则（规则一定会有调不准的时候）。
 */
class PaymentNotificationService : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val notification = sbn?.notification ?: return
        val packageName = sbn.packageName ?: return

        val channel = PaymentParser.channelOf(packageName)
        if (channel == PaymentParser.Channel.UNKNOWN) return

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
                )
                if (parsed == null) return@runCatching

                val accountName = when (parsed.channel) {
                    PaymentParser.Channel.WECHAT -> "微信"
                    PaymentParser.Channel.ALIPAY -> "支付宝"
                    else -> null
                }
                val accountId = accountName?.let { repository.accountIdByName(it) }
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
                        source = TxSource.NOTIFICATION,
                    )
                }
            }
        }
    }
}
