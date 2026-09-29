package cz.honestlead.mezera.service

// Společný stav mezi AppWatchService a InterventionActivity (běží ve stejném procesu).
// Drží, které zásahy ještě nikdo nevyřídil: dokud nedáš "Pokračovat", appka se přes
// nedávné appky, notifikaci ani zabitím obrazovky Pause otevřít nedá, zásah naskočí znovu.
object InterventionGate {

    // Po spuštění zásahu chvíli ignorujeme další události cíle (appka se ještě dokresluje).
    private const val LAUNCH_SETTLE_MS = 1500L

    private val lastSeen = HashMap<String, Long>()
    private val pending = HashSet<String>()
    private var lastForegroundPkg: String? = null
    private var showing = false
    private var lastLaunchAt = 0L

    data class Decision(val intervene: Boolean)

    @Synchronized
    fun onOtherForeground(pkg: String) {
        lastForegroundPkg = pkg
    }

    @Synchronized
    fun onTargetForeground(pkg: String, now: Long, gapMs: Long, blocked: Boolean): Decision {
        // Zásah je právě na obrazovce (nebo se zrovna otevírá): drobné události cíle pod ním nic nemění.
        if (showing || now - lastLaunchAt < LAUNCH_SETTLE_MS) {
            lastForegroundPkg = pkg
            return Decision(false)
        }

        val entering = lastForegroundPkg != pkg
        val last = lastSeen[pkg]
        val intervene = when {
            // Nevyřízený zásah se vrací při každém dalším objevení cíle.
            pkg in pending -> true
            // Úplný blok se ukáže při každém novém vstupu do appky.
            blocked -> entering || last == null
            // Jinak jen při prvním vstupu nebo po delší pauze (ne krátký odskok).
            else -> last == null || now - last > gapMs
        }

        // Čas obnovíme VŽDY, když je cíl v popředí, takže dokud appku aktivně
        // používáš, znovu to nevyskočí. Neodkliknutý zásah ale drží pending výš.
        lastSeen[pkg] = now
        lastForegroundPkg = pkg

        if (intervene) {
            pending.add(pkg)
            lastLaunchAt = now
        }
        return Decision(intervene)
    }

    @Synchronized
    fun launchFailed(pkg: String) {
        lastLaunchAt = 0L
        pending.remove(pkg)
    }

    @Synchronized
    fun setShowing(value: Boolean) {
        showing = value
        if (!value) {
            // Obrazovka Pause zmizela: návrat do cíle se bere jako nový vstup.
            lastForegroundPkg = null
            lastLaunchAt = 0L
        }
    }

    // "Pokračovat": teprve teď začíná sezení (klid mezi dotazy).
    @Synchronized
    fun continued(pkg: String, now: Long) {
        pending.remove(pkg)
        lastSeen[pkg] = now
        lastForegroundPkg = pkg
    }

    // "Rozmyslel jsem si to": žádný cooldown, při dalším otevření zastav znovu.
    @Synchronized
    fun abandoned(pkg: String) {
        pending.remove(pkg)
        lastSeen.remove(pkg)
        lastForegroundPkg = null
    }
}
