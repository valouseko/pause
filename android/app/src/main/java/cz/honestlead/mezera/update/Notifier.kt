package cz.honestlead.mezera.update

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import cz.honestlead.mezera.AppLanguage
import cz.honestlead.mezera.R

// Lokální upozornění na novou verzi (žádný server/Firebase).
object Notifier {

    private const val CHANNEL_ID = "updates"
    private const val NOTIF_ID = 1001

    fun ensureChannel(context: Context) {
        val localized = AppLanguage.localizedContext(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = context.getSystemService(NotificationManager::class.java)
            val ch = NotificationChannel(
                CHANNEL_ID,
                localized.getString(R.string.updates),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = localized.getString(R.string.updates_description)
            }
            mgr.createNotificationChannel(ch)
        }
    }

    fun showUpdate(context: Context, versionName: String) {
        val localized = AppLanguage.localizedContext(context)
        ensureChannel(context)
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pi = PendingIntent.getActivity(
            context,
            0,
            launch,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(localized.getString(R.string.update_notification_title, versionName))
            .setContentText(localized.getString(R.string.update_notification_body))
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(NOTIF_ID, notif)
        } catch (e: SecurityException) {
            // Bez povolených notifikací to prostě nic neudělá.
        }
    }
}
