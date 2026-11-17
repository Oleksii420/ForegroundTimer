package com.example.privatarchive

sealed interface ArchiveUiState {
    data object Idle : ArchiveUiState
    data object Loading : ArchiveUiState
    data class Success(
        val date: String,
        val rates: List<ExchangeRateItem>
    ) : ArchiveUiState
    data class Error(val message: String) : ArchiveUiState
}