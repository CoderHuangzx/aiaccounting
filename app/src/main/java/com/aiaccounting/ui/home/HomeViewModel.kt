package com.aiaccounting.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.aiaccounting.data.AccountingRepository
import com.aiaccounting.domain.DaySection
import com.aiaccounting.domain.HomeGrouper
import com.aiaccounting.domain.HomeSummary
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.ZoneId

data class HomeUiState(
    val sections: List<DaySection> = emptyList(),
    val summary: HomeSummary = HomeSummary(),
    val loaded: Boolean = false,
)

class HomeViewModel(private val repository: AccountingRepository) : ViewModel() {

    private val zone: ZoneId = ZoneId.systemDefault()

    val uiState: StateFlow<HomeUiState> = repository.observeTransactions()
        .map { txs ->
            val today = LocalDate.now(zone)
            HomeUiState(
                sections = HomeGrouper.group(txs, today, zone),
                summary = HomeGrouper.summary(txs, today, zone),
                loaded = true,
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    companion object {
        fun factory(repository: AccountingRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { HomeViewModel(repository) }
        }
    }
}
