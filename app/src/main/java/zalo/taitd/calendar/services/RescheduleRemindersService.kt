package zalo.taitd.calendar.services

import android.app.IntentService
import android.content.Intent
import android.util.Log
import zalo.taitd.calendar.utils.Constants
import zalo.taitd.calendar.ReminderManager
import zalo.taitd.calendar.databases.GoogleCalendarDAO
import zalo.taitd.calendar.utils.TAG

class RescheduleRemindersService : IntentService("RescheduleRemindersService") {

    override fun onHandleIntent(intent: Intent?) {
        when (intent?.action) {
            Constants.ACTION_RESCHEDULE_REMINDERS -> {
                Log.d(TAG, "onHandleIntent: ACTION_RESCHEDULE_REMINDERS")
                val events = GoogleCalendarDAO.getEventsSync(this)

                Log.d(TAG, "get events, last event: ${events.last()}")

                ReminderManager.createRemindersForChangedEventsSync(
                    this,
                    ArrayList(),
                    events
                )
            }
        }
    }
}
