package zalo.taitd.calendar

import android.app.IntentService
import android.content.Intent
import android.content.Context
import android.util.Log
import io.reactivex.Completable
import io.reactivex.schedulers.Schedulers

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
