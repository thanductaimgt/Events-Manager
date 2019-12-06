package zalo.taitd.calendar.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import zalo.taitd.calendar.R
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

object Utils {
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

    fun <T> getNoneIntersectElements(list1: List<T>, list2: List<T>): List<T> {
        val remain1 = list1.toMutableList().apply { removeAll(list2) }
        val remain2 = list2.toMutableList().apply { removeAll(list1) }

        return remain1.apply { addAll(remain2) }
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