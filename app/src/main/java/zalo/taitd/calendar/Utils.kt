package zalo.taitd.calendar

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import io.reactivex.Completable
import io.reactivex.CompletableObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

object Utils {
    fun createRemindersAsync(context: Context, event: Event, observer: CompletableObserver) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationService::class.java).apply {
            putExtra(Constants.EVENT_ID, event.id)
        }

        val currentTimeMillis = System.currentTimeMillis()

        Completable.fromCallable {
            val reminders = CalendarProviderDAO.getRemindersSync(context, event.id)

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
                    Log.d(TAG, "alarm created: timeToRemind: ${Date(timeToRemind)}, event: $event")
                }

                hasReminderAtEvent = hasReminderAtEvent or (reminder.minutes == 0)
            }

            if (!hasReminderAtEvent && currentTimeMillis < event.startTime.time) {
                alarmManager
                    .appCompatSet(
                        AlarmManager.RTC_WAKEUP,
                        event.startTime.time,
                        PendingIntent.getService(
                            context,
                            1998 + event.id.toInt(), intent, PendingIntent.FLAG_UPDATE_CURRENT
                        )
                    )
                Log.d(TAG, "alarm created: timeToRemind: ${event.startTime}, event: $event")
            }
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(observer)
    }

    fun getTimeFormat(date: Date): String {
        return SimpleDateFormat.getTimeInstance(DateFormat.SHORT).format(date)
    }

    fun getDateFormat(date: Date): String {
        return SimpleDateFormat.getDateInstance(DateFormat.SHORT).format(date)
    }

    fun getDateTimeDiffFormat(context: Context, date: Date, now: Date = Date()): String {
        val diffByMillisecond = date.time - now.time
        return if (diffByMillisecond > 0) getFutureDateTimeDiffFormat(
            context,
            diffByMillisecond
        ) else getPastDateTimeDiffFormat(context, -diffByMillisecond)
    }

    private fun getFutureDateTimeDiffFormat(context: Context, diffByMillisecond: Long): String {
        return when {
            diffByMillisecond < Constants.ONE_MIN_IN_MILLISECOND -> context.getString(R.string.less_than_one_min)
            diffByMillisecond < Constants.ONE_DAY_IN_MILLISECOND -> getFutureTimeDiffFormat(
                context,
                diffByMillisecond
            )
            else -> {
                val date = diffByMillisecond / Constants.ONE_DAY_IN_MILLISECOND
                val hour =
                    (diffByMillisecond % Constants.ONE_DAY_IN_MILLISECOND) / Constants.ONE_HOUR_IN_MILLISECOND
                String.format(context.getString(R.string.remaining_date_time), date, hour)
            }
        }
    }

    private fun getPastDateTimeDiffFormat(context: Context, diffByMillisecond: Long): String {
        return when {
            diffByMillisecond < Constants.ONE_MIN_IN_MILLISECOND -> context.getString(R.string.just_happened)
            diffByMillisecond < Constants.ONE_DAY_IN_MILLISECOND -> getPastTimeDiffFormat(
                context,
                diffByMillisecond
            )
            else -> {
                val date = diffByMillisecond / Constants.ONE_DAY_IN_MILLISECOND
                val hour =
                    (diffByMillisecond % Constants.ONE_DAY_IN_MILLISECOND) / Constants.ONE_HOUR_IN_MILLISECOND
                String.format(context.getString(R.string.passed_date_time), date, hour)
            }
        }
    }

    private fun getFutureTimeDiffFormat(context: Context, diffByMillisecond: Long): String {
        val count: Long
        val unitResId: Int
        when {
            diffByMillisecond < Constants.ONE_HOUR_IN_MILLISECOND -> {
                count = diffByMillisecond / Constants.ONE_MIN_IN_MILLISECOND
                unitResId = R.string.label_minute
            }
            else -> {
                count = diffByMillisecond / Constants.ONE_HOUR_IN_MILLISECOND
                unitResId = R.string.label_hour
            }
        }
        return String.format(
            context.getString(R.string.remaining_time),
            count,
            context.getString(unitResId)
        )
    }

    private fun getPastTimeDiffFormat(context: Context, diffByMillisecond: Long): String {
        val count: Long
        val unitResId: Int
        when {
            diffByMillisecond < Constants.ONE_HOUR_IN_MILLISECOND -> {
                count = diffByMillisecond / Constants.ONE_MIN_IN_MILLISECOND
                unitResId = R.string.label_minute
            }
            else -> {
                count = diffByMillisecond / Constants.ONE_HOUR_IN_MILLISECOND
                unitResId = R.string.label_hour
            }
        }
        return String.format(
            context.getString(R.string.passed_time),
            count,
            context.getString(unitResId)
        )
    }

    fun getCurAccount(context: Context):String?{
        val prefs = context.getSharedPreferences(Constants.SHARE_PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(Constants.CUR_ACCOUNT_KEY, null)
    }

    fun setCurAccount(context: Context, accountName:String){
        val edit = context.getSharedPreferences(Constants.SHARE_PREFS_NAME, Context.MODE_PRIVATE).edit()
        edit.putString(Constants.CUR_ACCOUNT_KEY, accountName)
        edit.apply()
    }
}

val Any.TAG: String
    get() = this::class.java.simpleName

fun AlarmManager.appCompatSet(type: Int, triggerAtMillis: Long, operation: PendingIntent) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
        this.setExact(type, triggerAtMillis, operation)
    } else {
        this.set(type, triggerAtMillis, operation)
    }
}