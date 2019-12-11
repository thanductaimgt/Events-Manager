package zalo.taitd.calendar

import android.content.Context
import android.content.SharedPreferences

object SharePreferenceManager {
    private const val SHARE_PREFS_NAME = "Events Manager Share Preferences"
    private const val CUR_ACCOUNT_KEY = "CUR_ACCOUNT_KEY"

    private fun getPrefs(context: Context):SharedPreferences{
        return context.getSharedPreferences(SHARE_PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getCurAccount(context: Context): String? {
        val prefs = getPrefs(context)
        return prefs.getString(CUR_ACCOUNT_KEY, null)
    }

    fun setCurAccount(context: Context, accountName: String) {
        val edit =
            getPrefs(context).edit()
        edit.putString(CUR_ACCOUNT_KEY, accountName)
        edit.apply()
    }
}