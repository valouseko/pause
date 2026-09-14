package cz.honestlead.mezera.data

import org.json.JSONObject

// Jeden zásah: kdy, kam, proč, a jestli tam nakonec šel nebo si to rozmyslel.
data class InterventionEvent(
    val packageName: String,
    val label: String,
    val ts: Long,
    val reason: String,
    val outcome: String // "continued" | "abandoned"
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("packageName", packageName)
        put("label", label)
        put("ts", ts)
        put("reason", reason)
        put("outcome", outcome)
    }

    companion object {
        fun fromJson(o: JSONObject) = InterventionEvent(
            packageName = o.optString("packageName", ""),
            label = o.optString("label", ""),
            ts = o.optLong("ts", 0L),
            reason = o.optString("reason", ""),
            outcome = o.optString("outcome", "continued")
        )
    }
}
