package com.karabotshivhase.vibe.data.favorites

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.karabotshivhase.vibe.VibeApp

class SyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): ListenableWorker.Result {
        val app = applicationContext as VibeApp
        val syncer = app.syncer
        val outcome = syncer.sync()
        return when (outcome) {
            SyncOutcome.Done,
            SyncOutcome.Skipped,
            SyncOutcome.SessionExpired,
            -> ListenableWorker.Result.success()

            SyncOutcome.Offline,
            SyncOutcome.Failed,
            -> ListenableWorker.Result.retry()
        }
    }
}
