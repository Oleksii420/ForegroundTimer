package com.example.privatroom

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.CancellationException
import java.io.IOException

const val KEY_DATE = "date"
const val KEY_MESSAGE = "message"
const val KEY_EMPTY = "empty"
private const val TAG = "ExchangeSync"
private const val MAX_ATTEMPTS = 3

class ExchangeRatesSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val date = inputData.getString(KEY_DATE) ?: DateUtils.today()
        Log.d(TAG, "Старт синхронізації за $date (спроба ${runAttemptCount + 1})")

        if (DateUtils.validate(date) != null) {
            return failure("Некоректна дата: $date")
        }

        val response = try {
            ArchiveNetwork.api.getExchangeRates(date)
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Log.w(TAG, "Мережева помилка", e)
            return retryOrFail("Немає з'єднання з сервером")
        } catch (e: Exception) {
            Log.e(TAG, "Не вдалося розібрати відповідь", e)
            return failure("Некоректна відповідь сервера")
        }

        if (!response.isSuccessful) {
            val code = response.code()
            return if (code >= 500 || code == 429) {
                retryOrFail("Сервер тимчасово недоступний (HTTP $code)")
            } else {
                failure("Сервер повернув HTTP $code")
            }
        }
        val body = response.body() ?: return failure("Сервер повернув відповідь без даних")
        if (body.date != date) {
            return failure("Сервер повернув курси за іншу дату (${body.date})")
        }

        val entities = body.exchangeRate.orEmpty().mapNotNull { item ->
            val code = item.currency?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            ExchangeRateEntity(
                date = date,
                currency = code,
                baseCurrency = item.baseCurrency ?: "UAH",
                purchaseRate = item.purchaseRate,
                saleRate = item.saleRate
            )
        }

        if (entities.isEmpty()) {
            Log.d(TAG, "Сервер не повернув курсів за $date")
            return Result.success(
                workDataOf(KEY_MESSAGE to "Сервер не повернув курсів", KEY_EMPTY to true)
            )
        }

        ExchangeDatabase.getDatabase(applicationContext).exchangeRateDao().upsertAll(entities)
        Log.d(TAG, "Збережено ${entities.size} записів за $date")
        return Result.success(workDataOf(KEY_MESSAGE to "Оновлено: ${entities.size} валют"))
    }

    private fun retryOrFail(message: String): Result =
        if (runAttemptCount + 1 < MAX_ATTEMPTS) Result.retry() else failure(message)

    private fun failure(message: String): Result =
        Result.failure(workDataOf(KEY_MESSAGE to message))
}