package cz.honestlead.mezera.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import cz.honestlead.mezera.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val notes: String,
    val apkUrl: String
)

// Aktualizace ze serveru (GitHub Releases). Stálý podpis = instaluje se přes starou
// verzi, data i oprávnění zůstávají.
object Updater {

    private const val REPO = "valouseko/pause"
    private const val VERSION_URL =
        "https://github.com/$REPO/releases/latest/download/version.json"
    private const val APK_URL_FALLBACK =
        "https://github.com/$REPO/releases/latest/download/Pause.apk"

    fun currentVersionName(): String = BuildConfig.VERSION_NAME
    fun currentVersionCode(): Int = BuildConfig.VERSION_CODE

    // Vrátí info o novější verzi, nebo null když je aktuální / nedostupné.
    suspend fun check(): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val text = fetchText(VERSION_URL)
            val o = JSONObject(text)
            val vc = o.getInt("versionCode")
            if (vc > BuildConfig.VERSION_CODE) {
                UpdateInfo(
                    versionCode = vc,
                    versionName = o.optString("versionName", vc.toString()),
                    notes = o.optString("notes", ""),
                    apkUrl = o.optString("apkUrl", APK_URL_FALLBACK)
                )
            } else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun downloadApk(
        context: Context,
        url: String,
        onProgress: (Float) -> Unit = {}
    ): File = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "updates")
        dir.mkdirs()
        val out = File(dir, "Pause.apk")
        if (out.exists()) out.delete()
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = true
            connectTimeout = 20000
            readTimeout = 60000
        }
        try {
            val total = conn.contentLengthLong
            conn.inputStream.use { input ->
                out.outputStream().use { output ->
                    val buf = ByteArray(64 * 1024)
                    var readTotal = 0L
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        output.write(buf, 0, n)
                        readTotal += n
                        if (total > 0) {
                            onProgress((readTotal.toFloat() / total).coerceIn(0f, 1f))
                        }
                    }
                }
            }
        } finally {
            conn.disconnect()
        }
        out
    }

    fun installApk(context: Context, file: File) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    // Android vyžaduje povolení "instalovat neznámé appky" pro tuhle appku.
    fun canInstall(context: Context): Boolean =
        context.packageManager.canRequestPackageInstalls()

    fun openUnknownSourcesSettings(context: Context) {
        try {
            context.startActivity(
                Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:" + context.packageName)
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (e: Exception) {
        }
    }

    private fun fetchText(url: String): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = true
            connectTimeout = 15000
            readTimeout = 20000
        }
        return try {
            conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }
}
