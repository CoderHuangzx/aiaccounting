package com.aiaccounting.ui.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.aiaccounting.data.AccountingRepository
import com.aiaccounting.domain.PeriodRange
import com.aiaccounting.domain.PeriodUnit
import com.aiaccounting.domain.ReportCalculator
import com.aiaccounting.domain.ReportData
import com.aiaccounting.domain.ReportPeriods
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.ZoneId

data class ReportUiState(
    val unit: PeriodUnit = PeriodUnit.MONTH,
    val range: PeriodRange = ReportPeriods.of(PeriodUnit.MONTH, LocalDate.now()),
    val data: ReportData = ReportData(),
)

class ReportViewModel(private val repository: AccountingRepository) : ViewModel() {

    private val zone: ZoneId = ZoneId.systemDefault()
    private val unit = MutableStateFlow(PeriodUnit.MONTH)
    private val anchor = MutableStateFlow(LocalDate.now(zone))

    val uiState: StateFlow<ReportUiState> = combine(
        unit,
        anchor,
        repository.observeTransactions(),
    ) { currentUnit, currentAnchor, txs ->
        val range = ReportPeriods.of(currentUnit, currentAnchor)
        ReportUiState(
            unit = currentUnit,
            range = range,
            data = ReportCalculator.build(txs, range, LocalDate.now(zone), zone),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReportUiState())

    fun selectUnit(newUnit: PeriodUnit) {
        unit.value = newUnit
        anchor.value = LocalDate.now(zone)
    }

    fun shift(delta: Int) {
        anchor.value = ReportPeriods.shift(unit.value, anchor.value, delta)
    }

    companion object {
        fun factory(repository: AccountingRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { ReportViewModel(repository) }
        }
    }
}
