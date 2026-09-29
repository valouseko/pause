package cz.honestlead.mezera.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import cz.honestlead.mezera.data.Store
import cz.honestlead.mezera.ui.InterventionActivity
import java.util.Calendar

// Sleduje, která appka jde do popředí. Když je hlídaná, spustí intervenci.
class AppWatchService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val pkg = event.packageName?.toString() ?: return
        // Vlastní okna (intervence, nastavení) nikdy nehlídáme. Jestli je intervence
        // zrovna vidět, hlásí sama InterventionActivity přes InterventionGate.
        if (pkg == packageName) {
            InterventionGate.onOtherForeground(pkg)
            return
        }

        val store = Store.get(this)
        if (!store.enabled) return

        val target = store.getTarget(pkg)
        if (target == null || !target.enabled) {
            // V popředí je něco, co nehlídáme. Zapamatujeme si to, ale nezasahujeme.
            InterventionGate.onOtherForeground(pkg)
            return
        }

        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance().apply { timeInMillis = now }
        val minuteOfDay = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
        val policy = target.policyAt(minuteOfDay)

        val decision = InterventionGate.onTargetForeground(
            pkg = pkg,
            now = now,
            gapMs = policy.sessionGapSec * 1000L,
            blocked = policy.blocked
        )
        if (!decision.intervene) return

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
            InterventionGate.launchFailed(pkg)
        }
    }

    override fun onInterrupt() {}
}
