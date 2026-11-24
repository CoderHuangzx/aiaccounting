package com.aiaccounting.data

import android.content.Context
import com.aiaccounting.domain.PaymentParser

/**
 * 自动记账的「来源」开关。
 *
 * 每个渠道单独一个开关：关掉的渠道既不解析、也不留日志、更不会生成待确认记录。
 * 默认全开——用户装完就能用，之后嫌吵再关。
 */
enum class AutoSource(val key: String, val label: String, val detail: String) {
    WECHAT("wechat", "微信支付", "解析微信支付通知"),
    ALIPAY("alipay", "支付宝", "解析支付宝支出 / 收款通知"),
    TAOBAO("taobao", "淘宝", "解析淘宝订单通知"),
    JD("jd", "京东", "解析京东订单通知"),
    MEITUAN("meituan", "美团", "解析美团 / 美团外卖通知"),
    DOUYIN("douyin", "抖音", "解析抖音商城订单通知"),
    PINDUODUO("pinduoduo", "拼多多", "解析拼多多订单通知"),
    ACCESSIBILITY("accessibility", "读屏记账（下单即记）", "读取淘宝 / 京东 / 抖音 / 美团 / 拼多多订单页"),
}

/** 通知渠道 → 对应的开关项。 */
fun autoSourceOf(channel: PaymentParser.Channel): AutoSource? = when (channel) {
    PaymentParser.Channel.WECHAT -> AutoSource.WECHAT
    PaymentParser.Channel.ALIPAY -> AutoSource.ALIPAY
    PaymentParser.Channel.TAOBAO -> AutoSource.TAOBAO
    PaymentParser.Channel.JD -> AutoSource.JD
    PaymentParser.Channel.MEITUAN -> AutoSource.MEITUAN
    PaymentParser.Channel.DOUYIN -> AutoSource.DOUYIN
    PaymentParser.Channel.PINDUODUO -> AutoSource.PINDUODUO
    PaymentParser.Channel.UNKNOWN -> null
}

/** 开关的持久化。 */
class AutoSourceSettings(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("auto_sources", Context.MODE_PRIVATE)

    fun isEnabled(source: AutoSource): Boolean = prefs.getBoolean(source.key, true)

    fun setEnabled(source: AutoSource, enabled: Boolean) {
        prefs.edit().putBoolean(source.key, enabled).apply()
    }

    fun snapshot(): Map<AutoSource, Boolean> =
        AutoSource.entries.associateWith { isEnabled(it) }

    fun setAll(enabled: Boolean) {
        prefs.edit().apply {
            AutoSource.entries.forEach { putBoolean(it.key, enabled) }
        }.apply()
    }

    /** 供后台服务快速判断，避免每次都构造 map。 */
    fun isChannelEnabled(channel: PaymentParser.Channel): Boolean {
        val source = autoSourceOf(channel) ?: return false
        return isEnabled(source)
    }
}
