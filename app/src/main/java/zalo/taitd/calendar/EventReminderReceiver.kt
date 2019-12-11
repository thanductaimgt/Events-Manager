package zalo.taitd.calendar

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import zalo.taitd.calendar.utils.TAG
import zalo.taitd.calendar.services.RemindService
import zalo.taitd.calendar.utils.Constants


class EventReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // This method is called when the BroadcastReceiver is receiving an Intent broadcast.
        Log.d(TAG, "onReceive")

        val uri = intent.data!!
        val alertTime = uri.lastPathSegment

        context.startService(Intent(context, RemindService::class.java).apply {
            putExtra(Constants.ALERT_TIME, alertTime)
        })
    }
}
