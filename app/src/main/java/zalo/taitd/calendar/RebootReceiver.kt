package zalo.taitd.calendar

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import zalo.taitd.calendar.services.RescheduleRemindersService
import zalo.taitd.calendar.utils.Constants
import zalo.taitd.calendar.utils.TAG

class RebootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive")
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                Log.d(TAG, "ACTION_BOOT_COMPLETED")
                context.startService(Intent(context, RescheduleRemindersService::class.java).apply {
                    action = Constants.ACTION_RESCHEDULE_REMINDERS
                })
            }
        }
    }
}
