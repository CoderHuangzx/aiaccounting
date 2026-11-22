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

    val allCategories: List<CategoryEntity> = expenseCategories + incomeCategories
}
