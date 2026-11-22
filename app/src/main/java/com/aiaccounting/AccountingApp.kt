package com.aiaccounting

import android.app.Application
import com.aiaccounting.data.AccountingRepository
import com.aiaccounting.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AccountingApp : Application() {

    lateinit var repository: AccountingRepository
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        repository = AccountingRepository(AppDatabase.get(this))
        appScope.launch { repository.ensureSeeded() }
    }
}
