package cz.honestlead.mezera.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import cz.honestlead.mezera.service.AppWatchService

// Zjištění a otevření potřebných oprávnění.
object Perm {

    fun accessibilityEnabled(context: Context): Boolean {
        val expected = ComponentName(context, AppWatchService::class.java).flattenToString()
        val setting = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return setting.split(':').any { it.equals(expected, ignoreCase = true) }
    }

    fun overlayGranted(context: Context): Boolean = Settings.canDrawOverlays(context)

    fun openAccessibilitySettings(context: Context) {
        try {
            context.startActivity(
                Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (e: Exception) {
        }
    }

    fun openOverlaySettings(context: Context) {
        try {
            context.startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + context.packageName)
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (e: Exception) {
        }
    }
}
