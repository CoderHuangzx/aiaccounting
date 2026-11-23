package com.aiaccounting.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.aiaccounting.data.AccountingRepository
import com.aiaccounting.domain.DaySection
import com.aiaccounting.domain.HomeFilter
import com.aiaccounting.domain.HomeGrouper
import com.aiaccounting.domain.HomeSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.ZoneId

data class HomeUiState(
    val sections: List<DaySection> = emptyList(),
    val summary: HomeSummary = HomeSummary(),
    val loaded: Boolean = false,
    val query: String = "",
) {
    val isSearching: Boolean get() = query.isNotBlank()
}

class HomeViewModel(private val repository: AccountingRepository) : ViewModel() {

    private val zone: ZoneId = ZoneId.systemDefault()
    private val query = MutableStateFlow("")

    val uiState: StateFlow<HomeUiState> = combine(
        repository.observeTransactions(),
        query,
    ) { txs, currentQuery ->
        val today = LocalDate.now(zone)
        val visible = HomeFilter.apply(txs, currentQuery)
        HomeUiState(
            sections = HomeGrouper.group(visible, today, zone),
            // 头部的今日/本月数字始终按全部数据算，搜索时不跟着变小
            summary = HomeGrouper.summary(txs, today, zone),
            loaded = true,
            query = currentQuery,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun onQueryChange(value: String) {
        query.value = value
    }

    companion object {
        fun factory(repository: AccountingRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { HomeViewModel(repository) }
        }
    }
}
