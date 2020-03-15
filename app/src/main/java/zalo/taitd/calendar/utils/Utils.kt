package zalo.taitd.calendar.utils

import android.content.Context
import zalo.taitd.calendar.R
import java.text.DateFormat
import java.text.SimpleDateFormat

object Utils {
    fun getTimeFormat(milli:Long): String {
        return SimpleDateFormat.getTimeInstance(DateFormat.SHORT).format(milli)
    }

    fun getDateFormat(milli:Long, isLong:Boolean=true): String {
        return SimpleDateFormat.getDateInstance(if(isLong) DateFormat.FULL else DateFormat.DEFAULT).format(milli)
    }

    fun getDateTimeDiffFormat(context: Context, milli:Long, now: Long = System.currentTimeMillis()): String {
        val diffByMillisecond = milli - now
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
}

val Any.TAG: String
    get() = this::class.java.simpleName