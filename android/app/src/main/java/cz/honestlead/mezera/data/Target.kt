package cz.honestlead.mezera.data

import org.json.JSONObject

// Hlídaná appka. Na Androidu párujeme podle balíčku (spolehlivé, na rozdíl od titulku).
data class Target(
    val packageName: String,
    val label: String,
    val enabled: Boolean = true,
    val cooldownSec: Int = 8,
    val reasonMinChars: Int = 20,
    val sessionGapSec: Int = 300
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("packageName", packageName)
        put("label", label)
        put("enabled", enabled)
        put("cooldownSec", cooldownSec)
        put("reasonMinChars", reasonMinChars)
        put("sessionGapSec", sessionGapSec)
    }

    companion object {
        fun fromJson(o: JSONObject) = Target(
            packageName = o.getString("packageName"),
            label = o.optString("label", o.getString("packageName")),
            enabled = o.optBoolean("enabled", true),
            cooldownSec = o.optInt("cooldownSec", 8),
            reasonMinChars = o.optInt("reasonMinChars", 20),
            sessionGapSec = o.optInt("sessionGapSec", 300)
        )
    }
}
