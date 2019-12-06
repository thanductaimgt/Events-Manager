package zalo.taitd.calendar.services

import android.app.Service
import android.content.ContentResolver
import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.provider.CalendarContract
import android.util.Log
import zalo.taitd.calendar.utils.Constants
import zalo.taitd.calendar.utils.TAG

class CalendarProviderObserverService : Service() {
    override fun onCreate() {
        super.onCreate()

        observeCalendarProvider()
    }

    private fun observeCalendarProvider(){
        Log.d(TAG, "observeCalendarProvider")
        contentResolver.registerContentObserver(CalendarContract.CONTENT_URI, true, observer)
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    private val observer = object: ContentObserver(Handler()) {
        override fun onChange(selfChange: Boolean) {
            onChange(selfChange, null)
        }

        override fun onChange(selfChange: Boolean, changeUri: Uri?) {
            Log.d(TAG, "onChange")
            ContentResolver.requestSync(Constants.ACCOUNT, Constants.AUTHORITY, Bundle())
        }
    }
}