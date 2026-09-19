package com.example.duonote

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.first

class BackgroundSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val qrContent = QRDataStore(applicationContext).qrContent.first()
        if (qrContent.isNullOrBlank()) return Result.success()

        return try {
            FirebaseDatabase.getInstance().goOnline()
            NoteWidget.updateWidget(applicationContext)
            Result.success()
        } catch (error: Exception) {
            Result.retry()
        }
    }
}