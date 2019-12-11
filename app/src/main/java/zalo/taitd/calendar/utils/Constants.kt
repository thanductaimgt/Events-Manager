package zalo.taitd.calendar.utils

import android.provider.CalendarContract

object Constants {
    const val CALENDAR_PERMISSIONS_REQUEST = 0

    const val ALERT_TIME = "ALERT_TIME"

    const val ONE_MIN_IN_MILLISECOND = 60 * 1000
    const val ONE_HOUR_IN_MILLISECOND = 60 * ONE_MIN_IN_MILLISECOND
    const val ONE_DAY_IN_MILLISECOND = 24 * ONE_HOUR_IN_MILLISECOND

    const val AUTHORITY = CalendarContract.AUTHORITY
}