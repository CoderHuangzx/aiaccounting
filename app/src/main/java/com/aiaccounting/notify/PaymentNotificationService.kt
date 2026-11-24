package com.aiaccounting.notify

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.aiaccounting.AccountingApp
import com.aiaccounting.data.AutoSourceSettings
import com.aiaccounting.domain.PaymentParser
import com.aiaccounting.domain.TxSource
import com.aiaccounting.overlay.OverlayAnchor
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

    /**
     * 服务刚绑上时，把还挂在通知栏里的通知补捞一遍。
     *
     * 覆盖安装、强制停止、系统重启之后，监听器会短暂掉线；这期间推送出来的
     * 支付通知如果不管，就永远丢了。只补最近 15 分钟的，避免每次重连都刷一堆旧记录。
     */
    override fun onListenerConnected() {
        super.onListenerConnected()

        // 系统把监听器重新绑上来了 → 顺手把隐形锚点也挂回去。
        // App 被划掉、进程被回收后，这条路径是「在其他应用上层」状态恢复的关键。
        runCatching { OverlayAnchor.ensureAttached(this) }

        runCatching {
            val cutoff = System.currentTimeMillis() - 15 * 60 * 1000
            activeNotifications?.forEach { sbn ->
                if (sbn.postTime >= cutoff) handle(sbn)
            }
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val notification = sbn ?: return
        handle(notification)
    }

    private fun handle(sbn: StatusBarNotification) {
        val notification = sbn.notification ?: return
        val packageName = sbn.packageName ?: return

        // 只处理名单里的 App（微信/支付宝/淘宝/京东/美团/抖音）
        if (!PaymentParser.isWatched(packageName)) return

        // 用户关掉这个来源就彻底不处理：不解析、不留日志、不生成记录
        if (!AutoSourceSettings(this).isChannelEnabled(PaymentParser.channelOf(packageName))) return

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
                // 只留「像支付」的通知，营销推送和消息提醒不进日志，免得噪音淹没真正有用的记录
                val haystack = listOfNotNull(title, text).joinToString(" ")
                val looksLikeMoney = listOf(
                    "支付", "付款", "收款", "到账", "入账", "实付", "退款", "转账", "¥", "￥", "元",
                ).any { haystack.contains(it) }
                if (parsed == null && !looksLikeMoney) return@runCatching

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
