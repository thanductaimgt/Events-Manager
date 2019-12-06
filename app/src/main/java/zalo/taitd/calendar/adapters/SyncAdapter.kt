package zalo.taitd.calendar.adapters

import android.accounts.Account
import android.content.*
import android.os.Bundle
import android.util.Log
import zalo.taitd.calendar.utils.Constants
import zalo.taitd.calendar.utils.TAG
import zalo.taitd.calendar.services.RescheduleRemindersService

class SyncAdapter @JvmOverloads constructor(
    context: Context,
    autoInitialize: Boolean,
    /**
     * Using a default argument along with @JvmOverloads
     * generates constructor for both method signatures to maintain compatibility
     * with Android 3.0 and later platform versions
     */
    allowParallelSyncs: Boolean = true
) : AbstractThreadedSyncAdapter(context, autoInitialize, allowParallelSyncs) {
    override fun onPerformSync(
        account: Account?,
        extras: Bundle?,
        authority: String?,
        provider: ContentProviderClient?,
        syncResult: SyncResult?
    ) {
        Log.d(TAG, "onPerformSync")
        context.startService(Intent(context, RescheduleRemindersService::class.java).apply {
            action = Constants.ACTION_RESCHEDULE_REMINDERS
        })
    }
}