package zalo.taitd.calendar

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
import zalo.taitd.calendar.models.Event


class NotificationService : IntentService(NotificationService::class.java.simpleName) {
    override fun onHandleIntent(intent: Intent?) {
        if (intent != null) {
            val eventId = intent.getLongExtra(Constants.EVENT_ID, -1)

            if (eventId != -1L) {
                val event = GoogleCalendarDAO.getEventSync(this, eventId)

                val uri: Uri =
                    ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
                val notificationIntent = Intent(Intent.ACTION_VIEW).setData(uri)

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
                    .setTicker(getString(R.string.app_name))
                    .setContentTitle(getNotificationTitle(event))
                    .setContentText(notificationTextFormat)
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
                    getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(event.id.toInt(), builder.build())
            } else {
                Log.e(TAG, "eventId = -1")
            }
        } else {
            Log.e(TAG, "intent null")
        }
    }

    private fun getNotificationContent(event: Event): String {
        val startDateFormat = Utils.getDateFormat(event.startTime)
        val startTimeFormat = Utils.getTimeFormat(event.startTime)

        val endDateFormat = Utils.getDateFormat(event.startTime)
        val endTimeFormat = Utils.getTimeFormat(event.startTime)

        return "${Utils.getDateTimeDiffFormat(this, event.startTime)}\n" +
                "${(if (event.title != "") "${getString(R.string.title)}: ${event.title}" else getString(
                    R.string.no_title
                ))}\n" +
                "${getString(R.string.start)}: $startTimeFormat - $startDateFormat\n" +
                "${getString(R.string.end)}: $endTimeFormat - $endDateFormat" +
                (if (event.location != "") "\n${getString(R.string.location)}: ${event.location}" else "")
    }

    private fun getNotificationTitle(event: Event): String {
        return getString(R.string.incoming_event)
    }

    //helper function to create notification channel, use when start service in foreground
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val chan = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT
        )
        chan.lightColor = getColor(R.color.lightPrimary)
        chan.lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(chan)
    }

    companion object {
        private const val TIME_VIBRATE = 1000
        const val CHANNEL_ID = "1447"
        const val CHANNEL_NAME = "Notification Service"
        var isNotificationChannelCreated = false
    }
}