package cz.honestlead.mezera.update

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import cz.honestlead.mezera.data.Store
import java.util.concurrent.TimeUnit

// Periodická kontrola nové verze na pozadí + notifikace (bez serveru).
class UpdateWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val info = Updater.check() ?: return Result.success()
        val store = Store.get(applicationContext)
        if (info.versionCode > store.lastNotifiedVersion) {
            Notifier.showUpdate(applicationContext, info.versionName)
            store.lastNotifiedVersion = info.versionCode
        }
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "pause-update-check"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<UpdateWorker>(12, TimeUnit.HOURS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
