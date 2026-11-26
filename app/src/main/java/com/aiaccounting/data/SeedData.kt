package com.aiaccounting.data

import com.aiaccounting.data.local.AccountEntity
import com.aiaccounting.data.local.CategoryEntity
import com.aiaccounting.domain.TxType

/** 首次启动写入的账户与分类。emoji 直接当图标用，省掉图标资源与设计成本。 */
object SeedData {

    val accounts: List<AccountEntity> = listOf(
        AccountEntity(name = "微信", emoji = "💬", sortOrder = 0),
        AccountEntity(name = "支付宝", emoji = "🔵", sortOrder = 1),
        AccountEntity(name = "现金", emoji = "💵", sortOrder = 2),
        AccountEntity(name = "银行卡", emoji = "💳", sortOrder = 3),
    )

    val expenseCategories: List<CategoryEntity> = listOf(
        CategoryEntity(name = "餐饮", emoji = "🍜", kind = TxType.EXPENSE.name, sortOrder = 0),
        CategoryEntity(name = "交通", emoji = "🚌", kind = TxType.EXPENSE.name, sortOrder = 1),
        CategoryEntity(name = "购物", emoji = "🛍️", kind = TxType.EXPENSE.name, sortOrder = 2),
        CategoryEntity(name = "日用", emoji = "🧻", kind = TxType.EXPENSE.name, sortOrder = 3),
        CategoryEntity(name = "娱乐", emoji = "🎮", kind = TxType.EXPENSE.name, sortOrder = 4),
        CategoryEntity(name = "医疗", emoji = "💊", kind = TxType.EXPENSE.name, sortOrder = 5),
        CategoryEntity(name = "住房", emoji = "🏠", kind = TxType.EXPENSE.name, sortOrder = 6),
        CategoryEntity(name = "通讯", emoji = "📱", kind = TxType.EXPENSE.name, sortOrder = 7),
        CategoryEntity(name = "人情", emoji = "🎁", kind = TxType.EXPENSE.name, sortOrder = 8),
        CategoryEntity(name = "其他", emoji = "📦", kind = TxType.EXPENSE.name, sortOrder = 9),
    )

    val incomeCategories: List<CategoryEntity> = listOf(
        CategoryEntity(name = "工资", emoji = "💰", kind = TxType.INCOME.name, sortOrder = 0),
        CategoryEntity(name = "奖金", emoji = "🎉", kind = TxType.INCOME.name, sortOrder = 1),
        CategoryEntity(name = "理财", emoji = "📈", kind = TxType.INCOME.name, sortOrder = 2),
        CategoryEntity(name = "红包", emoji = "🧧", kind = TxType.INCOME.name, sortOrder = 3),
        CategoryEntity(name = "其他", emoji = "💵", kind = TxType.INCOME.name, sortOrder = 4),
    )

    /** 内置子分类：家长名 → 子分类列表。参照小米钱包的分法，够日常用即可。 */
    val subCategories: Map<String, List<Pair<String, String>>> = mapOf(
        "餐饮" to listOf("早餐" to "🥐", "午餐" to "🍚", "晚餐" to "🍲", "外卖" to "🛵", "零食" to "🍪", "饮品" to "☕"),
        "交通" to listOf("公交地铁" to "🚇", "打车" to "🚕", "加油" to "⛽", "停车" to "🅿️"),
        "购物" to listOf("服饰" to "👕", "数码" to "💻", "家居" to "🪑", "美妆" to "💄"),
        "娱乐" to listOf("电影" to "🎬", "游戏" to "🕹️", "旅行" to "✈️"),
        "医疗" to listOf("药品" to "💉", "门诊" to "🏥"),
        "住房" to listOf("房租" to "🔑", "水电" to "💡", "物业" to "🏢"),
        "通讯" to listOf("话费" to "📞", "宽带" to "🌐"),
        "人情" to listOf("红包" to "🧧", "礼品" to "🎁"),
        "工资" to listOf("月薪" to "💰", "加班费" to "🌙"),
        "理财" to listOf("基金" to "📊", "股票" to "📈", "利息" to "🏦"),
    )

    val allCategories: List<CategoryEntity> = expenseCategories + incomeCategories

    /** 15 个一级分类 + 全部内置子分类。 */
    val totalCategoryCount: Int = allCategories.size + subCategories.values.sumOf { it.size }
}
