package com.example.newssync

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.newssync.ui.theme.NewsSyncTheme
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    private val statusFlow = MutableStateFlow("Очікує...")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NewsSyncTheme {
                val status by statusFlow.collectAsStateWithLifecycle()
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NewsSyncScreen(
                        status = status,
                        onSyncNowClick = { startOneTimeSync() },
                        onEnableNightSyncClick = { enableNightSync() },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    private fun startOneTimeSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<NewsSyncWorker>()
            .setConstraints(constraints)
            .setInputData(Data.Builder().putString(KEY_USER_TOKEN, "abc123").build())
            .build()

        val workManager = WorkManager.getInstance(this)
        workManager.enqueue(request)

        workManager.getWorkInfoByIdLiveData(request.id).observe(this) { workInfo ->
            if (workInfo != null) {
                statusFlow.value = when (workInfo.state) {
                    WorkInfo.State.ENQUEUED -> "Очікує..."
                    WorkInfo.State.RUNNING -> "Виконується..."
                    WorkInfo.State.SUCCEEDED -> workInfo.outputData.getString(KEY_RESULT) ?: "Успіх"
                    WorkInfo.State.FAILED -> "Помилка"
                    WorkInfo.State.CANCELLED -> "Скасовано"
                    WorkInfo.State.BLOCKED -> "Заблоковано"
                }
            }
        }
    }

    private fun enableNightSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED)
            .setRequiresCharging(true)
            .build()

        val periodicRequest = PeriodicWorkRequestBuilder<NewsSyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .setInputData(Data.Builder().putString(KEY_USER_TOKEN, "abc123").build())
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "night_news_sync",
            ExistingPeriodicWorkPolicy.KEEP,
            periodicRequest
        )

        Toast.makeText(this, "Нічну синхронізацію увімкнено", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun NewsSyncScreen(
    status: String,
    onSyncNowClick: () -> Unit,
    onEnableNightSyncClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Новини", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        Text("Статус: $status", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(24.dp))

        Button(onClick = onSyncNowClick, modifier = Modifier.fillMaxWidth()) {
            Text("Синхронізувати зараз")
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onEnableNightSyncClick, modifier = Modifier.fillMaxWidth()) {
            Text("Увімкнути нічну синхронізацію")
        }
    }
}