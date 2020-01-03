package zalo.taitd.calendar.services

import android.app.*
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.CalendarContract
import android.provider.Settings
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import zalo.taitd.calendar.R
import zalo.taitd.calendar.CalendarProviderDAO
import zalo.taitd.calendar.MainActivity
import zalo.taitd.calendar.models.Event
import zalo.taitd.calendar.utils.Constants
import zalo.taitd.calendar.utils.TAG
import zalo.taitd.calendar.utils.Utils


class RemindService : IntentService(RemindService::class.java.simpleName) {
    override fun onHandleIntent(intent: Intent?) {
        if (intent != null) {
//            val eventId = intent.getLongExtra(Constants.EVENT_ID, -1)
//
//            if (eventId != -1L) {
//                remindEvent(this, eventId)
//            } else {
//                Log.e(TAG, "eventId = -1")
//            }
            val alertTime = intent.getStringExtra(Constants.ALERT_TIME)
            remindEventsWithAlertTime(alertTime)
        } else {
            Log.e(TAG, "intent null")
        }
    }

    private fun remindEventsWithAlertTime(alertTime: String) {
        Log.d(TAG, "remindEventsWithAlertTime: $alertTime")
        val selection = CalendarContract.CalendarAlerts.ALARM_TIME + "=?"
        with(
            contentResolver.query(
                CalendarContract.CalendarAlerts.CONTENT_URI_BY_INSTANCE,
                arrayOf(
                    CalendarContract.CalendarAlerts.EVENT_ID
                ),
                selection,
                arrayOf(alertTime),
                null
            )
        ) {
            if (this == null) {
                Log.d(TAG, "cursor is null")
                return
            }

            while (moveToNext()) {
                val eventId = getLong(getColumnIndex(CalendarContract.CalendarAlerts.EVENT_ID))
                remindEvent(eventId)
            }
        }
    }

    private fun remindEvent(eventId: Long) {
        Log.d(TAG, "remindEvent: $eventId")

        val event = CalendarProviderDAO.getEventSync(this, eventId)

        val notificationIntent = Intent(this, MainActivity::class.java).apply {
            action = Constants.ACTION_VIEW_EVENT
            putExtra(Constants.EXTRA_EVENT_ID, event.id)
        }

        notificationIntent.flags =
            Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        val requestID = System.currentTimeMillis().toInt()
        val contentIntent = PendingIntent
            .getActivity(
                this,
                requestID,
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !isNotificationChannelCreated) {
            createNotificationChannel()
            isNotificationChannelCreated = true
        }

        val notificationTextFormat = getNotificationContent(event)
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.app_icon_transparent)
            .setTicker(this.getString(R.string.app_name))
            .setContentTitle(getNotificationTitle())
            .setContentText(if (event.title != "") event.title else "(${getString(R.string.no_title)})")
            .setStyle(NotificationCompat.BigTextStyle().bigText(notificationTextFormat))
            .setSound(Settings.System.DEFAULT_NOTIFICATION_URI)
            .setDefaults(Notification.DEFAULT_SOUND)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVibrate(
                longArrayOf(
                    TIME_VIBRATE.toLong(),
                    TIME_VIBRATE.toLong(),
                    TIME_VIBRATE.toLong(),
                    TIME_VIBRATE.toLong(),
                    TIME_VIBRATE.toLong()
                )
            )
            .setContentIntent(contentIntent)
        val notificationManager =
            this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(event.id!!.toInt(), builder.build())
    }

    private fun getNotificationContent(event: Event): String {
        val startDateFormat = Utils.getDateFormat(event.startTime)
        val startTimeFormat = Utils.getTimeFormat(event.startTime)

        val endDateFormat = Utils.getDateFormat(event.startTime)
        val endTimeFormat = Utils.getTimeFormat(event.startTime)

        return "${Utils.getDateTimeDiffFormat(this, event.startTime)}\n" +
                "${(if (event.title != "") "${this.getString(R.string.title)}: ${event.title}" else this.getString(
                    R.string.no_title
                ))}\n" +
                "${this.getString(R.string.start)}: $startTimeFormat - $startDateFormat\n" +
                "${this.getString(R.string.end)}: $endTimeFormat - $endDateFormat" +
                (if (event.location != "") "\n${this.getString(R.string.location)}: ${event.location}" else "")
    }

    private fun getNotificationTitle(): String {
        return this.getString(R.string.incoming_event)
    }

    //helper function to create notification channel, use when start service in foreground
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val chan = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT
        )
        chan.lightColor = this.getColor(R.color.lightPrimary)
        chan.lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
        val service = this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(chan)
    }

    companion object {
        private const val TIME_VIBRATE = 1000
        const val CHANNEL_ID = "1447"
        const val CHANNEL_NAME = "Notification Service"
        var isNotificationChannelCreated = false
    }
}