package com.aiaccounting.accessibility

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.aiaccounting.AccountingApp
import com.aiaccounting.data.AutoSource
import com.aiaccounting.data.AutoSourceSettings
import com.aiaccounting.domain.OrderScreenParser
import com.aiaccounting.domain.WeChatPayScreenParser
import com.aiaccounting.domain.TxSource
import com.aiaccounting.domain.TxType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 读屏记账：在淘宝 / 天猫 / 京东 / 抖音 / 美团 的订单页上认「下单成功」「退款成功」。
 *
 * 为什么需要它：这类 App 内下单，真正扣款（以及通知）可能延迟到确认收货或 7 天后，
 * 靠通知抓不到「下单」这个时间点。读屏能拿到页面上的「实付款 ¥xx」，下单即记。
 *
 * 安全约束：
 * - 只在白名单 App 里跑，且只读当前窗口文字，不模拟点击；
 * - 认出来的记录一律进「待确认」收件箱，人工过一眼；
 * - 同一笔（类型+金额+店铺）60 秒内只记一次，避免页面刷新重复入账。
 */
class OrderAccessibilityService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // 用户关掉「读屏记账」就完全不看屏幕
        if (!AutoSourceSettings(this).isEnabled(AutoSource.ACCESSIBILITY)) return

        val packageName = event?.packageName?.toString() ?: return
        if (packageName !in WATCHED_PACKAGES) return

        val type = event.eventType
        if (type != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            type != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        ) {
            return
        }

        // 关键一道闸：event 里的包名可能是「上一个窗口」的残留，
        // 真正的前台必须以当前活动窗口的根节点为准。
        // 少了这一步会读到我们自己的记账界面，从「-5.11」「-4.11」里抠金额，形成自我记账的死循环。
        val root = rootInActiveWindow ?: return
        val foregroundPackage = root.packageName?.toString()
        if (foregroundPackage == null || foregroundPackage !in WATCHED_PACKAGES) return

        val texts = collectTexts(root) ?: return
        if (texts.isEmpty()) return

        // 微信走单独的解析：扫个人收款码付款时微信不发通知，只能读「支付成功」页面
        if (foregroundPackage == WECHAT_PACKAGE) {
            recordWeChatPayment(foregroundPackage, texts)
            return
        }

        val parsed = OrderScreenParser.parse(texts) ?: return

        // 有订单编号就按编号判重（同一单反复打开不会重复记）；
        // 没有就退回「类型+金额+店铺」。
        val signature = parsed.orderNo?.let { "no:$it" }
            ?: "${parsed.kind}|${parsed.amountCents}|${parsed.merchant.orEmpty()}"
        val now = System.currentTimeMillis()
        val prefs = getSharedPreferences("order_screen", MODE_PRIVATE)
        val lastSignature = prefs.getString(KEY_SIGNATURE, null)
        val lastAt = prefs.getLong(KEY_TIME, 0L)
        if (signature == lastSignature && now - lastAt < DEDUPE_WINDOW_MS) return
        prefs.edit().putString(KEY_SIGNATURE, signature).putLong(KEY_TIME, now).apply()

        val repository = (application as AccountingApp).repository
        scope.launch {
            runCatching {
                repository.ensureSeeded()
                repository.recordRawCapture(
                    packageName = foregroundPackage,
                    title = parsed.kind.label + "：" + parsed.evidence,
                    text = texts.joinToString(" ").take(300),
                    parsed = true,
                    reason = null,
                )

                val accountName = if (foregroundPackage.startsWith("com.taobao") ||
                    foregroundPackage.startsWith("com.tmall")
                ) {
                    "支付宝"
                } else {
                    "微信"
                }
                val accountId = repository.accountIdByName(accountName)
                    ?: repository.accounts().firstOrNull()?.id
                val categoryId = repository.resolveCategoryId(parsed.merchant, parsed.type)
                if (accountId != null && categoryId != null) {
                    repository.addCapturedTransaction(
                        type = parsed.type,
                        amountCents = parsed.amountCents,
                        categoryId = categoryId,
                        accountId = accountId,
                        occurredAt = now,
                        merchant = parsed.merchant,
                        note = "读屏：" + parsed.evidence,
                        source = TxSource.NOTIFICATION,
                    )
                }
            }
        }
    }

    override fun onInterrupt() = Unit

    /**
     * 微信「支付成功」页面 → 记一笔支出。
     *
     * 微信给个人收款码付款时**不推系统通知**（通知只覆盖商户收款），
     * 通知捕捉这条路对这类支付是断的，只能靠读屏。
     */
    private fun recordWeChatPayment(packageName: String, texts: List<String>) {
        val parsed = WeChatPayScreenParser.parse(texts) ?: return

        val signature = "wechat|${parsed.type}|${parsed.amountCents}|${parsed.merchant.orEmpty()}"
        val now = System.currentTimeMillis()
        val prefs = getSharedPreferences("order_screen", MODE_PRIVATE)
        val lastSignature = prefs.getString(KEY_SIGNATURE, null)
        val lastAt = prefs.getLong(KEY_TIME, 0L)
        if (signature == lastSignature && now - lastAt < DEDUPE_WINDOW_MS) return
        prefs.edit().putString(KEY_SIGNATURE, signature).putLong(KEY_TIME, now).apply()

        val repository = (application as AccountingApp).repository
        scope.launch {
            runCatching {
                repository.ensureSeeded()
                repository.recordRawCapture(
                    packageName = packageName,
                    title = parsed.evidence,
                    text = texts.joinToString(" ").take(300),
                    parsed = true,
                    reason = null,
                )
                val accountId = repository.accountIdByName("微信")
                    ?: repository.accounts().firstOrNull()?.id
                val categoryId = repository.resolveCategoryId(parsed.merchant, TxType.EXPENSE)
                if (accountId != null && categoryId != null) {
                    repository.addCapturedTransaction(
                        type = TxType.EXPENSE,
                        amountCents = parsed.amountCents,
                        categoryId = categoryId,
                        accountId = accountId,
                        occurredAt = now,
                        merchant = parsed.merchant,
                        note = "读屏：" + parsed.evidence,
                        source = TxSource.NOTIFICATION,
                    )
                }
            }
        }
    }

    private fun collectTexts(root: AccessibilityNodeInfo?): List<String>? {
        val node = root ?: return null
        val result = mutableListOf<String>()
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(node)
        var visited = 0

        while (queue.isNotEmpty() && visited < MAX_NODES && result.size < MAX_TEXTS) {
            val current = queue.removeFirst()
            visited++
            current.text?.toString()?.trim()?.takeIf { it.isNotEmpty() }?.let { result.add(it) }
            current.contentDescription?.toString()?.trim()?.takeIf { it.isNotEmpty() }
                ?.let { result.add(it) }

            for (index in 0 until current.childCount) {
                current.getChild(index)?.let { queue.add(it) }
            }
        }
        return result
    }

    private companion object {
        val WATCHED_PACKAGES = setOf(
            "com.taobao.taobao",
            "com.tmall.wireless",
            "com.jingdong.app.mall",
            "com.ss.android.ugc.aweme",
            "com.xunmeng.pinduoduo",
            "com.sankuai.meituan",
            "com.sankuai.meituan.takeoutnew",
            "com.tencent.mm",
        )
        const val WECHAT_PACKAGE = "com.tencent.mm"
        const val MAX_NODES = 400
        const val MAX_TEXTS = 200
        const val KEY_SIGNATURE = "last_signature"
        const val KEY_TIME = "last_time"
        const val DEDUPE_WINDOW_MS = 12 * 60 * 60 * 1000L
    }
}
