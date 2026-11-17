package com.example.privatroom

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

sealed interface RatesUiState {
    data object Loading : RatesUiState
    data class Loaded(val date: String, val rates: List<ExchangeRateEntity>) : RatesUiState
}

sealed interface SyncStatus {
    data object Idle : SyncStatus
    data object Waiting : SyncStatus
    data object Running : SyncStatus
    data class Succeeded(val message: String, val emptyResponse: Boolean) : SyncStatus
    data class Failed(val message: String) : SyncStatus
}

@OptIn(ExperimentalCoroutinesApi::class)
class ExchangeArchiveViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = ExchangeDatabase.getDatabase(application).exchangeRateDao()
    private val workManager = WorkManager.getInstance(application)

    private val _dateInput = MutableStateFlow(DateUtils.yesterday())
    val dateInput: StateFlow<String> = _dateInput.asStateFlow()

    private val _dateError = MutableStateFlow<String?>(null)
    val dateError: StateFlow<String?> = _dateError.asStateFlow()

    private val _selectedDate = MutableStateFlow(DateUtils.yesterday())
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    val ratesState: StateFlow<RatesUiState> = _selectedDate
        .flatMapLatest { date ->
            dao.observeRates(date).map { rates -> RatesUiState.Loaded(date, rates) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RatesUiState.Loading)

    val syncStatus: StateFlow<SyncStatus> = _selectedDate
        .flatMapLatest { date ->
            workManager.getWorkInfosForUniqueWorkFlow(ExchangeSync.oneTimeWorkName(date))
                .map { infos -> infos.toSyncStatus() }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SyncStatus.Idle)

    val savedDates: StateFlow<List<String>> = dao.observeSavedDates()
        .map { dates -> dates.sortedByDescending { DateUtils.parse(it)?.time ?: 0L } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onDateInputChange(text: String) {
        _dateInput.value = text
        _dateError.value = null
    }

    fun showRates() {
        applyDate()
    }

    fun refresh() {
        val date = applyDate() ?: return
        ExchangeSync.refreshDate(getApplication(), date)
    }

    fun selectSavedDate(date: String) {
        _dateInput.value = date
        _dateError.value = null
        _selectedDate.value = date
    }

    private fun applyDate(): String? {
        val date = _dateInput.value.trim()
        val error = DateUtils.validate(date)
        if (error != null) {
            _dateError.value = error
            return null
        }
        _selectedDate.value = date
        return date
    }
}

private fun List<WorkInfo>.toSyncStatus(): SyncStatus {
    val info = firstOrNull { !it.state.isFinished } ?: lastOrNull() ?: return SyncStatus.Idle
    return when (info.state) {
        WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> SyncStatus.Waiting
        WorkInfo.State.RUNNING -> SyncStatus.Running
        WorkInfo.State.SUCCEEDED -> SyncStatus.Succeeded(
            message = info.outputData.getString(KEY_MESSAGE) ?: "Оновлено",
            emptyResponse = info.outputData.getBoolean(KEY_EMPTY, false)
        )
        WorkInfo.State.FAILED -> SyncStatus.Failed(
            info.outputData.getString(KEY_MESSAGE) ?: "Не вдалося оновити курси"
        )
        WorkInfo.State.CANCELLED -> SyncStatus.Failed("Оновлення скасовано")
    }
}