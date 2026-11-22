package com.aiaccounting.ui.entry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.aiaccounting.data.AccountingRepository
import com.aiaccounting.data.local.AccountEntity
import com.aiaccounting.data.local.CategoryEntity
import com.aiaccounting.domain.AmountInput
import com.aiaccounting.domain.TxType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EntryUiState(
    val type: TxType = TxType.EXPENSE,
    val expression: String = "",
    val amountCents: Long? = null,
    val categoryId: Long? = null,
    val accountId: Long? = null,
    val note: String = "",
    val categories: List<CategoryEntity> = emptyList(),
    val accounts: List<AccountEntity> = emptyList(),
    val saving: Boolean = false,
    val saved: Boolean = false,
) {
    val canSave: Boolean
        get() = amountCents != null && amountCents > 0 && categoryId != null && accountId != null && !saving
}

class EntryViewModel(private val repository: AccountingRepository) : ViewModel() {

    private val _state = MutableStateFlow(EntryUiState())
    val state: StateFlow<EntryUiState> = _state.asStateFlow()

    init {
        loadCatalog(TxType.EXPENSE)
    }

    fun onKey(key: String) {
        _state.update { current ->
            val expression = AmountInput.press(current.expression, key)
            current.copy(
                expression = expression,
                amountCents = AmountInput.evaluate(expression),
            )
        }
    }

    fun onType(type: TxType) {
        if (_state.value.type == type) return
        _state.update { it.copy(type = type, categoryId = null) }
        loadCatalog(type)
    }

    fun onCategory(categoryId: Long) = _state.update { it.copy(categoryId = categoryId) }

    fun onAccount(accountId: Long) = _state.update { it.copy(accountId = accountId) }

    fun onNote(note: String) = _state.update { it.copy(note = note) }

    fun save() {
        val current = _state.value
        val amount = current.amountCents ?: return
        val categoryId = current.categoryId ?: return
        val accountId = current.accountId ?: return
        if (!current.canSave) return

        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            repository.addManualTransaction(
                type = current.type,
                amountCents = amount,
                categoryId = categoryId,
                accountId = accountId,
                note = current.note.takeIf { text -> text.isNotBlank() },
            )
            _state.update { it.copy(saving = false, saved = true) }
        }
    }

    /** 关闭面板后回到初始状态，下次打开是干净的一笔。 */
    fun reset() {
        _state.value = EntryUiState()
        loadCatalog(TxType.EXPENSE)
    }

    private fun loadCatalog(type: TxType) {
        viewModelScope.launch {
            repository.ensureSeeded()
            val categories = repository.categoriesOf(type)
            val accounts = repository.accounts()
            _state.update { current ->
                current.copy(
                    categories = categories,
                    accounts = accounts,
                    categoryId = categories.firstOrNull { it.id == current.categoryId }?.id
                        ?: categories.firstOrNull()?.id,
                    accountId = current.accountId ?: accounts.firstOrNull()?.id,
                )
            }
        }
    }

    companion object {
        fun factory(repository: AccountingRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { EntryViewModel(repository) }
        }
    }
}
