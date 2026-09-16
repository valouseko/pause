package cz.honestlead.mezera

import android.app.Application
import cz.honestlead.mezera.data.Store
import cz.honestlead.mezera.update.Notifier
import cz.honestlead.mezera.update.UpdateWorker

class MezeraApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Nahřejeme singleton úložiště.
        Store.get(this)
        // Notifikační kanál + periodická kontrola aktualizací na pozadí.
        Notifier.ensureChannel(this)
        UpdateWorker.schedule(this)
    }
}
