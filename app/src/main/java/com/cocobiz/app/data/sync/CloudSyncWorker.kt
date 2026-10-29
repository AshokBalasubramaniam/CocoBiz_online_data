package com.cocobiz.app.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cocobiz.app.data.local.AuthPreferences
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import retrofit2.HttpException
import java.io.IOException

/** Uploads the account's data; WorkManager re-runs it when the network comes back. */
@HiltWorker
class CloudSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val syncManager: CloudSyncManager,
    private val authPrefs: AuthPreferences
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // Without a session the changes stay pending and upload right after the next sign-in.
        if (!authPrefs.hasValidSession) return Result.success()
        return try {
            syncManager.push()
            Result.success()
        } catch (e: HttpException) {
            if (e.code() == 401) {
                authPrefs.endSession()
                Result.failure()
            } else {
                Result.retry()
            }
        } catch (e: IOException) {
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "CocoBizCloudSync"
    }
}
