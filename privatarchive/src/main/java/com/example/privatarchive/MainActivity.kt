package com.example.privatarchive

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.privatarchive.ui.theme.PrivatArchiveTheme
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PrivatArchiveTheme {
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
    val date by viewModel.date.collectAsStateWithLifecycle()
    val dateError by viewModel.dateError.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isLoading = uiState is ArchiveUiState.Loading

    var query by rememberSaveable { mutableStateOf("") }
    var sortAlphabetically by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("ПриватБанк — валюти", style = MaterialTheme.typography.titleLarge)

        OutlinedTextField(
            value = date,
            onValueChange = { viewModel.onDateChange(it) },
            label = { Text("Дата (дд.мм.рррр)") },
            isError = dateError != null,
            supportingText = { dateError?.let { Text(it) } },
            singleLine = true,
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = { viewModel.loadRates() },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Показати курси")
        }

        when (val state = uiState) {
            ArchiveUiState.Idle -> Text("Оберіть дату та натисніть «Показати курси».")

            ArchiveUiState.Loading -> CircularProgressIndicator(
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            is ArchiveUiState.Error -> {
                Text(state.message, color = MaterialTheme.colorScheme.error)
                OutlinedButton(onClick = { viewModel.loadRates() }) {
                    Text("Спробувати знову")
                }
            }

            is ArchiveUiState.Success -> {
                Text("Курси на ${state.date}", style = MaterialTheme.typography.titleMedium)

                if (state.rates.isEmpty()) {
                    Text("Банк не повернув курсів на цю дату.")
                } else {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = { Text("Пошук за кодом (наприклад, USD)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = sortAlphabetically,
                            onCheckedChange = { sortAlphabetically = it }
                        )
                        Text("За абеткою")
                    }

                    val visibleRates = state.rates
                        .filter { it.currency.orEmpty().contains(query.trim(), ignoreCase = true) }
                        .let { list -> if (sortAlphabetically) list.sortedBy { it.currency } else list }

                    if (visibleRates.isEmpty()) {
                        Text("Нічого не знайдено.")
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(visibleRates, key = { it.currency.orEmpty() }) { rate ->
                                RateCard(rate)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RateCard(rate: ExchangeRateItem) {
    val base = rate.baseCurrency ?: "UAH"
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("${rate.currency} / $base", style = MaterialTheme.typography.titleMedium)
            Text("Банк купує: ${formatRate(rate.purchaseRate, base)}")
            Text("Банк продає: ${formatRate(rate.saleRate, base)}")
        }
    }
}

private fun formatRate(value: Double?, base: String): String =
    if (value == null) "—" else String.format(Locale.US, "%.2f %s", value, base)