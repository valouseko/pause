package cz.honestlead.mezera.feedback

import android.os.Build
import cz.honestlead.mezera.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

// Odeslání feedbacku (ticketu). Přes ntfy.sh (veřejný relay, bez klíče v appce);
// GitHub Action to na druhé straně sesbírá jako Issue, které si Claude přečte.
object Feedback {

    // Neuhodnutelný kanál (slouží zároveň jako sdílené tajemství).
    private const val ENDPOINT = "https://ntfy.sh/pause-fb-92be18075e331c16"

    // Vrací true JEN když server odpověď opravdu přijal (HTTP 2xx).
    suspend fun send(text: String): Boolean = withContext(Dispatchers.IO) {
        val body = JSONObject().apply {
            put("kind", "pause-feedback")
            put("text", text.trim())
            put("version", BuildConfig.VERSION_NAME)
            put("versionCode", BuildConfig.VERSION_CODE)
            put("device", "${Build.MANUFACTURER} ${Build.MODEL}")
            put("android", Build.VERSION.SDK_INT)
            put("ts", System.currentTimeMillis())
        }.toString()

        var conn: HttpURLConnection? = null
        try {
            conn = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 15000
                readTimeout = 20000
                setRequestProperty("Title", "Pause feedback")
                setRequestProperty("Tags", "speech_balloon")
                setRequestProperty("Content-Type", "text/plain; charset=utf-8")
            }
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            code in 200..299
        } catch (e: Exception) {
            false
        } finally {
            conn?.disconnect()
        }
    }
}
