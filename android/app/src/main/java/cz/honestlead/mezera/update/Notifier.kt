package cz.honestlead.mezera.update

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

// Lokální upozornění na novou verzi (žádný server/Firebase).
object Notifier {

    private const val CHANNEL_ID = "updates"
    private const val NOTIF_ID = 1001

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = context.getSystemService(NotificationManager::class.java)
            if (mgr.getNotificationChannel(CHANNEL_ID) == null) {
                val ch = NotificationChannel(
                    CHANNEL_ID,
                    "Aktualizace",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Upozornění, když je nová verze Pause."
                }
                mgr.createNotificationChannel(ch)
            }
        }
    }

    fun showUpdate(context: Context, versionName: String) {
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
            .setContentTitle("Nová verze Pause ($versionName)")
            .setContentText("Ťukni a aktualizuj. Data i nastavení zůstanou.")
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
