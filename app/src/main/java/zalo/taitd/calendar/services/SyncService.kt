package zalo.taitd.calendar.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import zalo.taitd.calendar.utils.TAG
import zalo.taitd.calendar.adapters.SyncAdapter

class SyncService : Service() {

    override fun onCreate() {
        Log.d(TAG, "onCreate SyncService")
        /*
         * Create the sync adapter as a singleton.
         * Set the sync adapter as syncable
         * Disallow parallel syncs
         */
        synchronized(sSyncAdapterLock) {
            sSyncAdapter = sSyncAdapter ?: SyncAdapter(applicationContext, true)
        }
    }

    /**
     * Return an object that allows the system to invoke
     * the sync adapter.
     *
     */
    override fun onBind(intent: Intent): IBinder {
        /*
         * Get the object that allows external processes
         * to call onPerformSync(). The object is created
         * in the base class code when the SyncAdapter
         * constructors call super()
         *
         * We should never be in a position where this is called before
         * onCreate() so the exception should never be thrown
         */
        Log.d(TAG, "onBind SyncService")

        return sSyncAdapter?.syncAdapterBinder ?: throw IllegalStateException()
    }

    companion object {
        // Storage for an instance of the sync adapter
        private var sSyncAdapter: SyncAdapter? = null
        // Object to use as a thread-safe lock
        private val sSyncAdapterLock = Any()
    }
}