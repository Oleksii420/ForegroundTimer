package com.example.privatroom

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.privatroom.ui.theme.PrivatRoomTheme
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PrivatRoomTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ExchangeArchiveScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun ExchangeArchiveScreen(
    modifier: Modifier = Modifier,
    viewModel: ExchangeArchiveViewModel = viewModel()
) {
    val dateInput by viewModel.dateInput.collectAsStateWithLifecycle()
    val dateError by viewModel.dateError.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val ratesState by viewModel.ratesState.collectAsStateWithLifecycle()
    val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()
    val savedDates by viewModel.savedDates.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("ПриватБанк — валюти", style = MaterialTheme.typography.titleLarge)

        OutlinedTextField(
            value = dateInput,
            onValueChange = viewModel::onDateInputChange,
            label = { Text("Дата (дд.мм.рррр)") },
            isError = dateError != null,
            supportingText = { dateError?.let { Text(it) } },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = viewModel::showRates, modifier = Modifier.weight(1f)) {
                Text("Показати")
            }
            Button(onClick = viewModel::refresh, modifier = Modifier.weight(1f)) {
                Text("Оновити")
            }
        }

        if (savedDates.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(savedDates, key = { it }) { date ->
                    FilterChip(
                        selected = date == selectedDate,
                        onClick = { viewModel.selectSavedDate(date) },
                        label = { Text(date) }
                    )
                }
            }
        }

        SyncStatusText(syncStatus)

        when (val state = ratesState) {
            RatesUiState.Loading -> CircularProgressIndicator()

            is RatesUiState.Loaded -> {
                Text("Курси на ${state.date}", style = MaterialTheme.typography.titleMedium)

                if (state.rates.isEmpty()) {
                    Text("Курси за цю дату ще не завантажені")
                } else {
                    val status = syncStatus
                    val showingOldData = status is SyncStatus.Failed ||
                            (status is SyncStatus.Succeeded && status.emptyResponse)
                    if (showingOldData) {
                        Text(
                            "Показано раніше збережені курси",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.rates, key = { it.currency }) { rate ->
                            RateCard(rate)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SyncStatusText(status: SyncStatus) {
    when (status) {
        SyncStatus.Idle -> Unit
        SyncStatus.Waiting -> Text("⏳ Очікує мережі / запуску…")
        SyncStatus.Running -> Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            Text("Оновлюється…")
        }
        is SyncStatus.Succeeded -> Text("✓ ${status.message}")
        is SyncStatus.Failed -> Text(
            "Помилка оновлення: ${status.message}",
            color = MaterialTheme.colorScheme.error
        )
    }
}

@Composable
fun RateCard(rate: ExchangeRateEntity) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("${rate.currency} / ${rate.baseCurrency}", style = MaterialTheme.typography.titleMedium)
            Text("Банк купує: ${formatRate(rate.purchaseRate, rate.baseCurrency)}")
            Text("Банк продає: ${formatRate(rate.saleRate, rate.baseCurrency)}")
        }
    }
}

private fun formatRate(value: Double?, base: String): String =
    if (value == null) "—" else String.format(Locale.US, "%.2f %s", value, base)