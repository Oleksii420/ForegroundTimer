package com.example.privatroom

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

object ExchangeSync {

    private const val PERIODIC_WORK_NAME = "exchange_rates_periodic"
    fun oneTimeWorkName(date: String) = "exchange_rates_$date"

    private val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    fun schedulePeriodic(context: Context) {
        val request = PeriodicWorkRequestBuilder<ExchangeRatesSyncWorker>(1, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun refreshDate(context: Context, date: String) {
        val request = OneTimeWorkRequestBuilder<ExchangeRatesSyncWorker>()
            .setConstraints(constraints)
            .setInputData(workDataOf(KEY_DATE to date))
            .build()

        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            oneTimeWorkName(date),
            ExistingWorkPolicy.KEEP,
            request
        )
    }
}