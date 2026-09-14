package cz.honestlead.mezera.data

import android.content.Context
import org.json.JSONArray

// Lokální úložiště (SharedPreferences + JSON). Nic neodchází ven.
class Store private constructor(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("mezera", Context.MODE_PRIVATE)

    var enabled: Boolean
        get() = prefs.getBoolean("enabled", true)
        set(v) {
            prefs.edit().putBoolean("enabled", v).apply()
        }

    // ---- Cíle ----
    fun getTargets(): List<Target> {
        val s = prefs.getString("targets", null) ?: return emptyList()
        return try {
            val arr = JSONArray(s)
            (0 until arr.length()).map { Target.fromJson(arr.getJSONObject(it)) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveTargets(list: List<Target>) {
        val arr = JSONArray()
        list.forEach { arr.put(it.toJson()) }
        prefs.edit().putString("targets", arr.toString()).apply()
    }

    fun isTarget(pkg: String): Boolean = getTargets().any { it.packageName == pkg }

    fun getTarget(pkg: String): Target? = getTargets().firstOrNull { it.packageName == pkg }

    fun upsertTarget(t: Target) {
        val list = getTargets().toMutableList()
        val i = list.indexOfFirst { it.packageName == t.packageName }
        if (i >= 0) list[i] = t else list.add(t)
        saveTargets(list)
    }

    fun removeTarget(pkg: String) {
        saveTargets(getTargets().filter { it.packageName != pkg })
    }

    // ---- Události ----
    fun getEvents(): List<InterventionEvent> {
        val s = prefs.getString("events", null) ?: return emptyList()
        return try {
            val arr = JSONArray(s)
            (0 until arr.length()).map { InterventionEvent.fromJson(arr.getJSONObject(it)) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun addEvent(e: InterventionEvent) {
        val list = getEvents().toMutableList()
        list.add(e)
        while (list.size > 2000) list.removeAt(0)
        val arr = JSONArray()
        list.forEach { arr.put(it.toJson()) }
        prefs.edit().putString("events", arr.toString()).apply()
    }

    fun clearEvents() {
        prefs.edit().remove("events").apply()
    }

    companion object {
        @Volatile
        private var inst: Store? = null

        fun get(context: Context): Store =
            inst ?: synchronized(this) {
                inst ?: Store(context).also { inst = it }
            }
    }
}
