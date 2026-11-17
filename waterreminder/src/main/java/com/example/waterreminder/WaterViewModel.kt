package com.example.waterreminder

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

private const val WORK_NAME = "water_reminder"

class WaterViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = WaterRepository(application)
    private val workManager = WorkManager.getInstance(application)

    val reminderCount: StateFlow<Int> = repository.countFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val isEnabled: StateFlow<Boolean> = repository.enabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val intervalMinutes: StateFlow<Long> = repository.intervalFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 15L)

    fun startReminder(intervalMinutes: Long) {
        val safeInterval = intervalMinutes.coerceAtLeast(15L)   // системний мінімум

        val request = PeriodicWorkRequestBuilder<WaterReminderWorker>(
            safeInterval, TimeUnit.MINUTES
        ).build()

        workManager.enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )

        viewModelScope.launch { repository.saveSettings(enabled = true, interval = safeInterval) }
    }

    fun stopReminder() {
        workManager.cancelUniqueWork(WORK_NAME)
        viewModelScope.launch { repository.setEnabled(false) }
    }
}