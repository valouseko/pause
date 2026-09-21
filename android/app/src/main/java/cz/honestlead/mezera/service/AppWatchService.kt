package cz.honestlead.mezera.service

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.accessibility.AccessibilityEvent
import androidx.core.content.ContextCompat
import cz.honestlead.mezera.data.Store
import cz.honestlead.mezera.ui.InterventionActivity
import java.util.Calendar

// Sleduje, která appka jde do popředí. Když je hlídaná, spustí intervenci.
class AppWatchService : AccessibilityService() {

    companion object {
        const val ACTION_INTERVENTION_RESULT = "cz.honestlead.pause.INTERVENTION_RESULT"
        const val EXTRA_RESULT_PACKAGE = "resultPackage"
        const val EXTRA_RESULT_OUTCOME = "resultOutcome"
    }

    // Naposledy, kdy byl daný balíček v popředí (obnovuje se pokaždé, co v něm jsi).
    private val lastSeen = HashMap<String, Long>()

    // Poslední balíček v popředí (kvůli ignorování drobných událostí ve stejné appce).
    private var lastForegroundPkg: String? = null
    private var receiverRegistered = false

    private val resultReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val pkg = intent?.getStringExtra(EXTRA_RESULT_PACKAGE) ?: return
            if (intent.getStringExtra(EXTRA_RESULT_OUTCOME) == "abandoned") {
                // Rozmyšlení nesmí vytvořit cooldown. Při dalším otevření zastav znovu.
                lastSeen.remove(pkg)
                lastForegroundPkg = null
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        if (!receiverRegistered) {
            ContextCompat.registerReceiver(
                this,
                resultReceiver,
                IntentFilter(ACTION_INTERVENTION_RESULT),
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
            receiverRegistered = true
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val pkg = event.packageName?.toString() ?: return
        // Vlastní okno (intervence) ignorujeme a NEmažeme čas cíle - ať se po
        // zavření overlaye hned znovu nespustí.
        if (pkg == packageName) return

        val store = Store.get(this)
        if (!store.enabled) return

        val target = store.getTarget(pkg)
        if (target == null || !target.enabled) {
            // V popředí je něco, co nehlídáme. Zapamatujeme si to, ale nezasahujeme.
            lastForegroundPkg = pkg
            return
        }

        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance().apply { timeInMillis = now }
        val minuteOfDay = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
        val policy = target.policyAt(minuteOfDay)
        val last = lastSeen[pkg]
        val gapMs = policy.sessionGapSec * 1000L
        val enteringTarget = lastForegroundPkg != pkg

        // Zásah jen když jsme v cíli poprvé, nebo po delší pauze (ne krátký odskok).
        // Úplný blok se naopak ukáže při každém novém vstupu do appky.
        val intervene = if (policy.blocked) enteringTarget else last == null || now - last > gapMs

        // Čas obnovíme VŽDY, když je cíl v popředí (i bez zásahu) - takže dokud
        // appku aktivně používáš (i 20 minut), znovu to nevyskočí.
        lastSeen[pkg] = now
        lastForegroundPkg = pkg

        if (!intervene) return

        val intent = Intent(this, InterventionActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK or
                    Intent.FLAG_ACTIVITY_NO_ANIMATION
            )
            putExtra(InterventionActivity.EXTRA_PACKAGE, target.packageName)
            putExtra(InterventionActivity.EXTRA_LABEL, target.label)
            putExtra(InterventionActivity.EXTRA_COOLDOWN, policy.cooldownSec)
            putExtra(InterventionActivity.EXTRA_MIN_CHARS, policy.reasonMinChars)
            putExtra(InterventionActivity.EXTRA_BLOCKED, policy.blocked)
            putExtra(InterventionActivity.EXTRA_PAUSE_TEXT, target.pauseText)
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            // Kdyby systém start z pozadí odmítl (chybí překrytí), aspoň nespadneme.
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        if (receiverRegistered) {
            runCatching { unregisterReceiver(resultReceiver) }
            receiverRegistered = false
        }
        super.onDestroy()
    }
}
