package cz.honestlead.mezera

import android.app.Application
import cz.honestlead.mezera.data.Store

class MezeraApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Nahřejeme singleton úložiště.
        Store.get(this)
    }
}
