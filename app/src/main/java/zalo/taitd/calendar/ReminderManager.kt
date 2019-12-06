package zalo.taitd.calendar

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import android.util.LongSparseArray
import androidx.core.util.set
import io.reactivex.Completable
import io.reactivex.CompletableObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import zalo.taitd.calendar.databases.GoogleCalendarDAO
import zalo.taitd.calendar.models.Event
import zalo.taitd.calendar.models.EventReminders
import zalo.taitd.calendar.services.NotificationService
import zalo.taitd.calendar.utils.Constants
import zalo.taitd.calendar.utils.TAG
import zalo.taitd.calendar.utils.Utils
import zalo.taitd.calendar.utils.appCompatSet
import java.util.*
import kotlin.collections.ArrayList

object ReminderManager {
    fun createRemindersForChangedEventsAsync(
        context: Context,
        oldEvents: List<Event>,
        newEvents: List<Event>,
        observer: CompletableObserver
    ) {
        Completable.fromCallable {
            createRemindersForChangedEventsSync(context, oldEvents, newEvents)
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(observer)
    }

    fun createRemindersForChangedEventsSync(
        context: Context,
        oldEvents: List<Event>,
        newEvents: List<Event>
    ) {
        val changedEvents = Utils.getNoneIntersectElements(oldEvents, newEvents)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val currentTimeMillis = System.currentTimeMillis()

        val eventsReminders = ArrayList<EventReminders>()

        changedEvents.forEach { event ->
            val intent = Intent(context, NotificationService::class.java).apply {
                putExtra(Constants.EVENT_ID, event.id)
            }

            val reminders = GoogleCalendarDAO.getRemindersSync(context, event.id)

            val eventReminders = EventReminders(event.id, ArrayList())

            var hasReminderAtEvent = false
            reminders.forEach { reminder ->
                val timeToRemind =
                    event.startTime.time - reminder.minutes * Constants.ONE_MIN_IN_MILLISECOND
                if (currentTimeMillis < timeToRemind) {
                    alarmManager
                        .appCompatSet(
                            AlarmManager.RTC_WAKEUP,
                            timeToRemind,
                            PendingIntent.getService(
                                context,
                                reminder.id.toInt(), intent, PendingIntent.FLAG_UPDATE_CURRENT
                            )
                        )
                    eventReminders.remindersId.add(reminder.id)
                    Log.d(
                        TAG,
                        "alarm created: timeToRemind: ${Date(timeToRemind)}, event: $event"
                    )
                }

                hasReminderAtEvent = hasReminderAtEvent or (reminder.minutes == 0)
            }

            if (!hasReminderAtEvent && currentTimeMillis < event.startTime.time) {
                val reminderAtEventId = 1998 + event.id
                alarmManager
                    .appCompatSet(
                        AlarmManager.RTC_WAKEUP,
                        event.startTime.time,
                        PendingIntent.getService(
                            context,
                            reminderAtEventId.toInt(), intent, PendingIntent.FLAG_UPDATE_CURRENT
                        )
                    )
                eventReminders.remindersId.add(reminderAtEventId)
                Log.d(TAG, "alarm created: timeToRemind: ${event.startTime}, event: $event")
            }

            eventsReminders.add(eventReminders)
        }

        EventsManagerApplication.database.getDatabaseDAO()
            .insertEventsReminders(eventsReminders)
    }

    fun cancelRemindersForDeletedEventsAsync(
        context: Context,
        oldEvents: List<Event>,
        newEvents: List<Event>,
        observer: CompletableObserver
    ) {
        Completable.fromCallable {
            val deletedEventsMap = LongSparseArray<Event>()

            val deletedEvents = oldEvents.toMutableList().apply { removeAll(newEvents) }
                .also {
                    it.forEach { event ->
                        deletedEventsMap[event.id] = event
                    }
                }

            val intent = Intent(context, NotificationService::class.java)

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            val eventsReminders = EventsManagerApplication.database.getDatabaseDAO()
                .getEventsReminders(deletedEvents.map { it.id })

            eventsReminders.forEach { eventReminders ->
                eventReminders.remindersId.forEach { reminderId ->
                    alarmManager.cancel(
                        PendingIntent.getService(
                            context,
                            reminderId.toInt(), intent, PendingIntent.FLAG_UPDATE_CURRENT
                        )
                    )
                }

                Log.d(TAG, "deleted alarms of event: ${deletedEventsMap[eventReminders.eventId]}")
            }

            EventsManagerApplication.database.getDatabaseDAO()
                .deleteEventsReminders(eventsReminders)
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(observer)
    }
}