package com.aiaccounting.ui.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aiaccounting.data.AccountingRepository
import com.aiaccounting.data.local.CategoryEntity
import com.aiaccounting.data.local.MerchantRuleEntity
import com.aiaccounting.domain.TxType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 分类管理的状态。
 *
 * 只有用户自建的分类能删；内置分类只允许改名换图标——这样「其他」这类兜底分类
 * 永远存在，自动记账和账单导入不会因为分类被删而无处可落。
 */
class CategoryViewModel(private val repository: AccountingRepository) : ViewModel() {

    val expenseCategories: StateFlow<List<CategoryEntity>> =
        repository.observeCategories(TxType.EXPENSE)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val incomeCategories: StateFlow<List<CategoryEntity>> =
        repository.observeCategories(TxType.INCOME)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val merchantRules: StateFlow<List<MerchantRuleEntity>> =
        repository.observeMerchantRules()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    var message by mutableStateOf<String?>(null)
        private set

    fun addCategory(name: String, emoji: String, kind: TxType) {
        viewModelScope.launch {
            val created = repository.addCategory(name, emoji, kind)
            message = if (created == null) "分类名重复或为空，换个名字试试" else "已添加「${created.name}」"
        }
    }

    fun updateCategory(id: Long, name: String, emoji: String) {
        viewModelScope.launch {
            val ok = repository.updateCategory(id, name, emoji)
            message = if (ok) "已保存" else "名字不能为空"
        }
    }

    fun deleteCategory(id: Long) {
        viewModelScope.launch {
            message = when (val result = repository.deleteCategory(id)) {
                is AccountingRepository.DeleteCategoryResult.Deleted -> "已删除"
                is AccountingRepository.DeleteCategoryResult.BuiltIn -> "内置分类不能删除，可以改名换图标"
                is AccountingRepository.DeleteCategoryResult.NotFound -> "分类不存在"
                is AccountingRepository.DeleteCategoryResult.InUse ->
                    "这个分类下还有 ${result.count} 笔记录，先改掉再删"
            }
        }
    }

    fun deleteMerchantRule(pattern: String) {
        viewModelScope.launch {
            repository.deleteMerchantRule(pattern)
            message = "已删除该商户规则，下次会重新学习"
        }
    }

    fun clearMessage() {
        message = null
    }

    companion object {
        fun factory(repository: AccountingRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                CategoryViewModel(repository) as T
        }
    }
}
