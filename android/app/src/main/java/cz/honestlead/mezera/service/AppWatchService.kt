package cz.honestlead.mezera.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import cz.honestlead.mezera.data.Store
import cz.honestlead.mezera.ui.InterventionActivity

// Sleduje, která appka jde do popředí. Když je hlídaná, spustí intervenci.
class AppWatchService : AccessibilityService() {

    // Naposledy, kdy byl daný balíček v popředí (obnovuje se pokaždé, co v něm jsi).
    private val lastSeen = HashMap<String, Long>()

    // Poslední balíček v popředí (kvůli ignorování drobných událostí ve stejné appce).
    private var lastForegroundPkg: String? = null

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
        val last = lastSeen[pkg]
        val gapMs = target.sessionGapSec * 1000L

        // Zásah jen když jsme v cíli poprvé, nebo po delší pauze (ne krátký odskok).
        val intervene = last == null || now - last > gapMs

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
            putExtra(InterventionActivity.EXTRA_COOLDOWN, target.cooldownSec)
            putExtra(InterventionActivity.EXTRA_MIN_CHARS, target.reasonMinChars)
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            // Kdyby systém start z pozadí odmítl (chybí překrytí), aspoň nespadneme.
        }
    }

    override fun onInterrupt() {}
}
