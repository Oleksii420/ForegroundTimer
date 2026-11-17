package com.example.privatarchive

import android.util.Log
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private const val TAG = "PrivatArchive"

class ExchangeArchiveViewModel : ViewModel() {

    private val api = ArchiveNetwork.api

    private val _date = MutableStateFlow(yesterday())
    val date: StateFlow<String> = _date.asStateFlow()

    private val _dateError = MutableStateFlow<String?>(null)
    val dateError: StateFlow<String?> = _dateError.asStateFlow()

    private val _uiState = MutableStateFlow<ArchiveUiState>(ArchiveUiState.Idle)
    val uiState: StateFlow<ArchiveUiState> = _uiState.asStateFlow()

    private var currentCall: Call<ExchangeArchiveResponse>? = null

    fun onDateChange(newDate: String) {
        _date.value = newDate
        _dateError.value = null
    }

    fun loadRates() {
        val requestedDate = _date.value.trim()

        val error = validateDate(requestedDate)
        if (error != null) {
            _dateError.value = error
            return
        }

        currentCall?.cancel()
        _uiState.value = ArchiveUiState.Loading
        Log.d(TAG, "Запит курсів на дату $requestedDate")

        val call = api.getExchangeRates(requestedDate)
        currentCall = call

        call.enqueue(object : Callback<ExchangeArchiveResponse> {
            override fun onResponse(
                call: Call<ExchangeArchiveResponse>,
                response: Response<ExchangeArchiveResponse>
            ) {
                if (call !== currentCall || call.isCanceled) return

                val body = response.body()
                _uiState.value = when {
                    !response.isSuccessful -> ArchiveUiState.Error(
                        "Сервер повернув HTTP ${response.code()}. Спробуйте пізніше."
                    )
                    body == null -> ArchiveUiState.Error("Банк повернув відповідь без даних.")
                    else -> ArchiveUiState.Success(
                        date = body.date ?: requestedDate,
                        rates = body.exchangeRate.orEmpty()
                            .filter { !it.currency.isNullOrBlank() }
                    )
                }
                Log.d(TAG, "Відповідь отримано: HTTP ${response.code()}")
            }

            override fun onFailure(call: Call<ExchangeArchiveResponse>, t: Throwable) {
                if (call !== currentCall || call.isCanceled) return
                _uiState.value = ArchiveUiState.Error(
                    "Не вдалося завантажити курси. Перевірте з'єднання та спробуйте знову."
                )
                Log.e(TAG, "Помилка запиту", t)
            }
        })
    }

    override fun onCleared() {
        currentCall?.cancel()
        super.onCleared()
    }

    private fun validateDate(text: String): String? {
        if (!Regex("""\d{2}\.\d{2}\.\d{4}""").matches(text)) {
            return "Введіть дату у форматі дд.мм.рррр"
        }

        val format = SimpleDateFormat("dd.MM.yyyy", Locale.US).apply { isLenient = false }
        val date = try {
            format.parse(text)
        } catch (e: ParseException) {
            null
        } ?: return "Такої дати не існує"

        if (date.after(Date())) return "Не можна обирати майбутню дату"

        val fourYearsAgo = Calendar.getInstance().apply { add(Calendar.YEAR, -4) }.time
        if (date.before(fourYearsAgo)) return "Архів доступний лише за останні 4 роки"

        return null
    }

    private fun yesterday(): String {
        val calendar = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -1) }
        return SimpleDateFormat("dd.MM.yyyy", Locale.US).format(calendar.time)
    }
}