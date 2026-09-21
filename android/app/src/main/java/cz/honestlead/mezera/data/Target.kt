package cz.honestlead.mezera.data

import org.json.JSONObject
import org.json.JSONArray

data class InterventionPolicy(
    val cooldownSec: Int,
    val reasonMinChars: Int,
    val sessionGapSec: Int,
    val blocked: Boolean = false
)

data class TimeRule(
    val startMinute: Int,
    val endMinute: Int,
    val cooldownSec: Int = 8,
    val reasonMinChars: Int = 20,
    val sessionGapSec: Int = 300,
    val blocked: Boolean = false
) {
    fun isActiveAt(minuteOfDay: Int): Boolean {
        val start = startMinute.coerceIn(0, 1439)
        val end = endMinute.coerceIn(0, 1439)
        return when {
            start == end -> true
            start < end -> minuteOfDay in start until end
            else -> minuteOfDay >= start || minuteOfDay < end
        }
    }

    fun toJson(): JSONObject = JSONObject().apply {
        put("startMinute", startMinute)
        put("endMinute", endMinute)
        put("cooldownSec", cooldownSec)
        put("reasonMinChars", reasonMinChars)
        put("sessionGapSec", sessionGapSec)
        put("blocked", blocked)
    }

    companion object {
        fun fromJson(o: JSONObject) = TimeRule(
            startMinute = o.optInt("startMinute", 0).coerceIn(0, 1439),
            endMinute = o.optInt("endMinute", 0).coerceIn(0, 1439),
            cooldownSec = o.optInt("cooldownSec", 8).coerceIn(0, 120),
            reasonMinChars = o.optInt("reasonMinChars", 20).coerceIn(0, 200),
            sessionGapSec = o.optInt("sessionGapSec", 300).coerceIn(0, 3600),
            blocked = o.optBoolean("blocked", false)
        )
    }
}

// Hlídaná appka. Na Androidu párujeme podle balíčku (spolehlivé, na rozdíl od titulku).
data class Target(
    val packageName: String,
    val label: String,
    val enabled: Boolean = true,
    val cooldownSec: Int = 8,
    val reasonMinChars: Int = 20,
    val sessionGapSec: Int = 300,
    val pauseText: String = "",
    val timeRules: List<TimeRule> = emptyList()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("packageName", packageName)
        put("label", label)
        put("enabled", enabled)
        put("cooldownSec", cooldownSec)
        put("reasonMinChars", reasonMinChars)
        put("sessionGapSec", sessionGapSec)
        put("pauseText", pauseText)
        put("timeRules", JSONArray().apply { timeRules.forEach { put(it.toJson()) } })
    }

    fun policyAt(minuteOfDay: Int): InterventionPolicy {
        val rule = timeRules.lastOrNull { it.isActiveAt(minuteOfDay) }
        return if (rule == null) {
            InterventionPolicy(cooldownSec, reasonMinChars, sessionGapSec)
        } else {
            InterventionPolicy(rule.cooldownSec, rule.reasonMinChars, rule.sessionGapSec, rule.blocked)
        }
    }

    companion object {
        fun fromJson(o: JSONObject) = Target(
            packageName = o.getString("packageName"),
            label = o.optString("label", o.getString("packageName")),
            enabled = o.optBoolean("enabled", true),
            cooldownSec = o.optInt("cooldownSec", 8),
            reasonMinChars = o.optInt("reasonMinChars", 20),
            sessionGapSec = o.optInt("sessionGapSec", 300),
            pauseText = o.optString("pauseText", "").take(240),
            timeRules = o.optJSONArray("timeRules")?.let { arr ->
                (0 until arr.length()).mapNotNull { i ->
                    runCatching { TimeRule.fromJson(arr.getJSONObject(i)) }.getOrNull()
                }
            } ?: emptyList()
        )
    }
}
