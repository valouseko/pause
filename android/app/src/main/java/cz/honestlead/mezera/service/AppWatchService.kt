package cz.honestlead.mezera.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import cz.honestlead.mezera.data.Store
import cz.honestlead.mezera.ui.InterventionActivity

// Sleduje, která appka jde do popředí. Když je hlídaná, spustí intervenci.
class AppWatchService : AccessibilityService() {

    // Kdy jsme naposledy zasáhli u daného balíčku (kvůli klidu mezi dotazy).
    private val lastHandled = HashMap<String, Long>()

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName) return // vlastní okno (intervence) ignorujeme

        val store = Store.get(this)
        if (!store.enabled) return

        val target = store.getTarget(pkg) ?: return
        if (!target.enabled) return

        // Musí to být otevření celé appky, ne jen dialog uvnitř. Hrubý filtr:
        // TYPE_WINDOW_STATE_CHANGED se třídou aktivity je ok; necháme projít vše
        // a spoléháme na klid mezi dotazy (sessionGap).
        val now = System.currentTimeMillis()
        val last = lastHandled[pkg]
        val gapMs = target.sessionGapSec * 1000L
        if (last != null && now - last < gapMs) return

        lastHandled[pkg] = now

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
