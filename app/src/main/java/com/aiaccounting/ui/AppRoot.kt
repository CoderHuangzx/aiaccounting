package com.aiaccounting.ui

import android.widget.Toast
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aiaccounting.AccountingApp
import com.aiaccounting.ui.components.HomeBottomBar
import com.aiaccounting.ui.entry.EntrySheet
import com.aiaccounting.ui.entry.EntryViewModel
import com.aiaccounting.ui.home.HomeScreen
import com.aiaccounting.ui.home.HomeViewModel
import com.aiaccounting.ui.report.ReportScreen
import com.aiaccounting.ui.report.ReportViewModel
import com.aiaccounting.ui.theme.PageBg

@Composable
fun AppRoot() {
    val context = LocalContext.current
    val repository = (context.applicationContext as AccountingApp).repository

    val homeViewModel: HomeViewModel = viewModel(factory = HomeViewModel.factory(repository))
    val reportViewModel: ReportViewModel = viewModel(factory = ReportViewModel.factory(repository))
    val entryViewModel: EntryViewModel = viewModel(factory = EntryViewModel.factory(repository))

    val homeState by homeViewModel.uiState.collectAsStateWithLifecycle()
    val reportState by reportViewModel.uiState.collectAsStateWithLifecycle()
    val entryState by entryViewModel.state.collectAsStateWithLifecycle()

    var tab by rememberSaveable { mutableIntStateOf(0) }
    var showEntry by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(entryState.saved) {
        if (entryState.saved) {
            showEntry = false
            entryViewModel.reset()
            Toast.makeText(context, "已记一笔", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        containerColor = PageBg,
        bottomBar = {
            HomeBottomBar(
                selectedTab = tab,
                onSelect = { tab = it },
                onAdd = { showEntry = true },
            )
        },
    ) { innerPadding ->
        when (tab) {
            0 -> HomeScreen(
                state = homeState,
                modifier = Modifier.padding(innerPadding),
            )

            else -> ReportScreen(
                state = reportState,
                modifier = Modifier.padding(innerPadding),
                onSelectUnit = reportViewModel::selectUnit,
                onShift = reportViewModel::shift,
            )
        }
    }

    if (showEntry) {
        EntrySheet(
            state = entryState,
            onKey = entryViewModel::onKey,
            onType = entryViewModel::onType,
            onCategory = entryViewModel::onCategory,
            onAccount = entryViewModel::onAccount,
            onNote = entryViewModel::onNote,
            onDateChange = entryViewModel::onDateTimeChange,
            onSave = entryViewModel::save,
            onDismiss = {
                showEntry = false
                entryViewModel.reset()
            },
        )
    }
}
