package zalo.taitd.calendar

import android.content.Context
import android.content.SharedPreferences

object SharePrefsManager {
    private const val SHARE_PREFS_NAME = "Events Manager Share Preferences"
    private const val CUR_ACCOUNT_KEY = "CUR_ACCOUNT_KEY"
    private const val ACC_EXCLUDED_CALENDARS_ID_PREFIX = "ACCOUNT_CALENDARS_ID_PREFIX_"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(SHARE_PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getCurAccountName(context: Context): String? {
        val prefs = getPrefs(context)
        return prefs.getString(CUR_ACCOUNT_KEY, null)
    }

    fun setCurAccount(context: Context, accountName: String) {
        val edit =
            getPrefs(context).edit()
        edit.putString(CUR_ACCOUNT_KEY, accountName)
        edit.apply()
    }

    fun getAccExcludedCalendarsId(context: Context, accountName: String):Set<String>{
        val prefs = getPrefs(context)
        return prefs.getStringSet("$ACC_EXCLUDED_CALENDARS_ID_PREFIX${accountName}", HashSet())!!
    }

    fun removeCalendarFromAccExcludedCalendars(context: Context, calendarId: Long, accountName: String) {
        val prefs = getPrefs(context)
        val exCalendarsId:HashSet<String> =
            prefs.getStringSet("$ACC_EXCLUDED_CALENDARS_ID_PREFIX${accountName}", HashSet())!!.toHashSet()
        val edit = prefs.edit()
        edit.putStringSet(
            "$ACC_EXCLUDED_CALENDARS_ID_PREFIX${accountName}",
            exCalendarsId.apply { remove(calendarId.toString()) })
        edit.apply()
    }

    fun addCalendarToAccExcludedCalendars(context: Context, calendarId: Long, accountName: String) {
        val prefs = getPrefs(context)
        val calendarsId:HashSet<String> =
            prefs.getStringSet("$ACC_EXCLUDED_CALENDARS_ID_PREFIX${accountName}", HashSet())!!.toHashSet()
        val edit = prefs.edit()
        edit.putStringSet(
            "$ACC_EXCLUDED_CALENDARS_ID_PREFIX${accountName}",
            calendarsId.apply { add(calendarId.toString()) })
        edit.apply()
    }
}