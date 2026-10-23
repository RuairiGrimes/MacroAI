package com.macroai.tracker.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.macroai.tracker.BuildConfig
import com.macroai.tracker.data.local.dao.FoodLogDao
import com.macroai.tracker.data.remote.sheets.SheetsWebhookClient
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class SheetsSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val foodLogDao: FoodLogDao,
    private val sheetsWebhookClient: SheetsWebhookClient
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val webhookUrl = sheetsWebhookClient.getEffectiveWebhookUrl()
        if (webhookUrl.isBlank()) {
            return Result.success()
        }

        val unsyncedLogs = foodLogDao.getUnsyncedLogs()
        if (unsyncedLogs.isEmpty()) {
            return Result.success()
        }

        val postResult = sheetsWebhookClient.postUnsyncedLogs(unsyncedLogs)
        return if (postResult.getOrDefault(false)) {
            foodLogDao.markLogsAsSynced(unsyncedLogs.map { it.id })
            Result.success()
        } else {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    companion object {
        const val UNIQUE_WORK_NAME = "macro_ai_sheets_sync_work"

        fun enqueue(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = OneTimeWorkRequestBuilder<SheetsSyncWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    30,
                    TimeUnit.SECONDS
                )
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_WORK_NAME,
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                syncRequest
            )
        }
    }
}

