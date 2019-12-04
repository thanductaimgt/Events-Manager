package zalo.taitd.calendar

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.provider.CalendarContract
import android.util.Log
import io.reactivex.Single
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import zalo.taitd.calendar.models.Event
import zalo.taitd.calendar.models.Reminder
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashSet

object GoogleCalendarDAO {
    private val eventProjection: Array<String> = arrayOf(
        CalendarContract.Events._ID,
        CalendarContract.Events.TITLE,
        CalendarContract.Events.EVENT_LOCATION,
        CalendarContract.Events.DTSTART,
        CalendarContract.Events.DTEND,
        CalendarContract.Events.ACCOUNT_NAME
    )

    private val calendarProjection: Array<String> = arrayOf(
        CalendarContract.Calendars.ACCOUNT_NAME
    )

    private val reminderProjection: Array<String> = arrayOf(
        CalendarContract.Reminders._ID,
        CalendarContract.Reminders.EVENT_ID,
        CalendarContract.Reminders.MINUTES,
        CalendarContract.Reminders.METHOD
    )

    fun getAccountsAsync(context: Context, observer: SingleObserver<ArrayList<String>>) {
        Single.fromCallable {
            getAccountsSync(context)
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(observer)
    }

    @SuppressLint("MissingPermission")
    fun getAccountsSync(context: Context): ArrayList<String> {
        // Run query
        val uri: Uri = CalendarContract.Calendars.CONTENT_URI

        val res = HashSet<String>()
        val cursor = context.contentResolver.query(uri, calendarProjection, null, null, null)
        // Use the cursor to step through the returned records

        while (cursor != null && cursor.moveToNext()) {
            // Get the field values
            val accountName: String =
                cursor.getString(cursor.getColumnIndex(CalendarContract.Calendars.ACCOUNT_NAME))
                    ?: ""

            res.add(accountName)
        }
        cursor?.close()
        Log.d(TAG, "accounts: $res")

        return ArrayList(res).apply { sort() }
    }

    fun getEventsAsync(
        context: Context,
        observer: SingleObserver<List<Event>>,
        accountName: String?=null,
        doGetOthers: Boolean = false
    ) {
        Single.fromCallable {
            getEventsSync(context, accountName, doGetOthers)
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(observer)
    }

    @SuppressLint("MissingPermission")
    fun getEventsSync(
        context: Context,
        accountName: String?=null,
        doGetOthers: Boolean = false
    ): List<Event> {
        // Run query
        val uri: Uri = CalendarContract.Events.CONTENT_URI
        var selection = "(${CalendarContract.Events.DELETED} = 0)"
        var selectionArgs:Array<String>? = null
        if(accountName!=null){
            selection = "((${CalendarContract.Events.ACCOUNT_NAME} ${if (doGetOthers) "<>" else "="} ?) AND $selection)"
            selectionArgs = arrayOf(accountName)
        }

        val res = ArrayList<Event>()
        val cursor =
            context.contentResolver.query(uri, eventProjection, selection, selectionArgs, null)
        // Use the cursor to step through the returned records

        while (cursor != null && cursor.moveToNext()) {
            // Get the field values
            val id = cursor.getLong(cursor.getColumnIndex(CalendarContract.Events._ID))
            val title =
                cursor.getString(cursor.getColumnIndex(CalendarContract.Events.TITLE)) ?: ""
            val location =
                cursor.getString(cursor.getColumnIndex(CalendarContract.Events.EVENT_LOCATION))
                    ?: ""
            val startTime =
                Date(cursor.getLong(cursor.getColumnIndex(CalendarContract.Events.DTSTART)))
            val endTime = Date(cursor.getLong(cursor.getColumnIndex(CalendarContract.Events.DTEND)))
            val accountName2 = cursor.getString(cursor.getColumnIndex(CalendarContract.Events.ACCOUNT_NAME))

            res.add(
                Event(
                    id, title, location, startTime, endTime, accountName2
                )
            )
        }
        cursor?.close()
        Log.d(TAG, "events: ${res.takeLast(10)}")

        return res
    }

    @SuppressLint("MissingPermission")
    fun getEventSync(context: Context, eventId: Long): Event {
        // Run query
        val uri: Uri = CalendarContract.Events.CONTENT_URI
        val selection = "(${CalendarContract.Events._ID} = ?)"
        val selectionArgs: Array<String?> = arrayOf(eventId.toString())

        val res: Event
        val cursor =
            context.contentResolver.query(uri, eventProjection, selection, selectionArgs, null)
        // Use the cursor to step through the returned records
        cursor?.apply {
            moveToNext()

            val id: Long = cursor.getLong(cursor.getColumnIndex(CalendarContract.Events._ID))
            val title: String =
                cursor.getString(cursor.getColumnIndex(CalendarContract.Events.TITLE)) ?: ""
            val location: String =
                cursor.getString(cursor.getColumnIndex(CalendarContract.Events.EVENT_LOCATION))
                    ?: ""
            val startTime =
                Date(cursor.getLong(cursor.getColumnIndex(CalendarContract.Events.DTSTART)))
            val endTime = Date(cursor.getLong(cursor.getColumnIndex(CalendarContract.Events.DTEND)))
            val accountName =
                cursor.getString(cursor.getColumnIndex(CalendarContract.Events.ACCOUNT_NAME))

            close()

            res = Event(
                id,
                title,
                location,
                startTime,
                endTime,
                accountName
            )

            Log.d(TAG, "event: $res")

            return res
        }

        throw Throwable("cursor null")
    }

    @SuppressLint("MissingPermission")
    fun getRemindersSync(context: Context, eventId: Long): ArrayList<Reminder> {
        // Run query
        val uri: Uri = CalendarContract.Reminders.CONTENT_URI
        val selection = "(${CalendarContract.Reminders.EVENT_ID} = ?)"
        val selectionArgs: Array<String?> = arrayOf(eventId.toString())

        val res = ArrayList<Reminder>()
        val cursor =
            context.contentResolver.query(uri, reminderProjection, selection, selectionArgs, null)

        while (cursor != null && cursor.moveToNext()) {
            // Get the field values
            val id = cursor.getLong(cursor.getColumnIndex(CalendarContract.Reminders._ID))
            val minutes =
                cursor.getInt(cursor.getColumnIndex(CalendarContract.Reminders.MINUTES))
            val method =
                cursor.getInt(cursor.getColumnIndex(CalendarContract.Reminders.METHOD))

            res.add(
                Reminder(
                    id, eventId, minutes, method
                )
            )
        }
        cursor?.close()
        Log.d(TAG, "reminders: $res")

        return res
    }
}