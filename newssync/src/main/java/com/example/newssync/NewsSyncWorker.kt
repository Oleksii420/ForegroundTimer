package com.example.newssync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import kotlinx.coroutines.delay

const val KEY_USER_TOKEN = "user_token"
const val KEY_RESULT = "result"
private const val TAG = "NewsSync"

class NewsSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val token = inputData.getString(KEY_USER_TOKEN) ?: return Result.failure()
        Log.d(TAG, "Старт: токен = $token, потік = ${Thread.currentThread().name}")

        delay(3000)

        val newsCount = 15
        Log.d(TAG, "Синхронізовано $newsCount новин")
        val out = Data.Builder()
            .putString(KEY_RESULT, "Успіх: завантажено $newsCount новин")
            .build()
        return Result.success(out)
    }
}