package cz.honestlead.mezera.data

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable

data class AppInfo(val packageName: String, val label: String)

// Seznam spustitelných appek (co má uživatel v šuplíku).
object AppCatalog {

    fun launchableApps(context: Context): List<AppInfo> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved = pm.queryIntentActivities(intent, 0)
        val seen = LinkedHashMap<String, String>()
        for (ri in resolved) {
            val pkg = ri.activityInfo.packageName
            if (pkg == context.packageName) continue
            if (!seen.containsKey(pkg)) {
                seen[pkg] = ri.loadLabel(pm).toString()
            }
        }
        return seen.map { AppInfo(it.key, it.value) }
            .sortedBy { it.label.lowercase() }
    }

    fun loadIcon(context: Context, pkg: String): Bitmap? {
        return try {
            drawableToBitmap(context.packageManager.getApplicationIcon(pkg))
        } catch (e: Exception) {
            null
        }
    }

    private fun drawableToBitmap(d: Drawable): Bitmap {
        if (d is BitmapDrawable && d.bitmap != null) return d.bitmap
        val w = if (d.intrinsicWidth > 0) d.intrinsicWidth else 108
        val h = if (d.intrinsicHeight > 0) d.intrinsicHeight else 108
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        d.setBounds(0, 0, canvas.width, canvas.height)
        d.draw(canvas)
        return bmp
    }
}
