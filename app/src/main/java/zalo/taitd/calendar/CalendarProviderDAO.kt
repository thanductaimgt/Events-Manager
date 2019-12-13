package zalo.taitd.calendar

import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.CalendarContract
import android.util.Log
import io.reactivex.Completable
import io.reactivex.CompletableObserver
import io.reactivex.Single
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import zalo.taitd.calendar.models.Account
import zalo.taitd.calendar.models.Calendar
import zalo.taitd.calendar.models.Event
import zalo.taitd.calendar.utils.TAG
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

object CalendarProviderDAO {
    private val eventProjection: Array<String> = arrayOf(
        CalendarContract.Events._ID,
        CalendarContract.Events.TITLE,
        CalendarContract.Events.EVENT_LOCATION,
        CalendarContract.Events.DESCRIPTION,
        CalendarContract.Events.DTSTART,
        CalendarContract.Events.DTEND,
        CalendarContract.Events.ACCOUNT_NAME,
        CalendarContract.Events.CALENDAR_ID
    )

    private val calendarProjection: Array<String> = arrayOf(
        CalendarContract.Calendars._ID,
        CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
        CalendarContract.Calendars.ACCOUNT_NAME,
        CalendarContract.Calendars.IS_PRIMARY
    )

    fun getAccountsAsync(context: Context, observer: SingleObserver<HashMap<String, Account>>) {
        Single.fromCallable {
            getAccountsSync(context)
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(observer)
    }

    @SuppressLint("MissingPermission")
    fun getAccountsSync(context: Context): HashMap<String, Account> {
        // Run query
        val uri: Uri = CalendarContract.Calendars.CONTENT_URI

        val res = HashMap<String, Account>()

        val cursor = context.contentResolver.query(
            uri,
            calendarProjection, null, null, null
        )
        // Use the cursor to step through the returned records

        while (cursor != null && cursor.moveToNext()) {
            val id = cursor.getLong(cursor.getColumnIndex(CalendarContract.Calendars._ID))
            val displayName =
                cursor.getString(cursor.getColumnIndex(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME))
            val accountName: String =
                cursor.getString(cursor.getColumnIndex(CalendarContract.Calendars.ACCOUNT_NAME))
                    ?: ""
            val isPrimaryCalendar =
                cursor.getInt(cursor.getColumnIndex(CalendarContract.Calendars.IS_PRIMARY)) == 1

            var account = res[accountName]

            if (account == null) {
                account = Account(accountName)
                res[accountName] = account
            }
            account.calendars.add(
                Calendar(
                    id, displayName, accountName, isPrimaryCalendar
                )
            )
        }
        cursor?.close()
        Log.d(TAG, "accounts: $res")

        return res
    }

    fun getEventsAsync(
        context: Context,
        observer: SingleObserver<List<Event>>,
        accountName: String? = null,
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
        accountName: String? = null,
        doGetOthers: Boolean = false
    ): List<Event> {
        // Run query
        val uri: Uri = CalendarContract.Events.CONTENT_URI
        var selection = "(${CalendarContract.Events.DELETED} = 0)"
        var selectionArgs: Array<String>? = null
        if (accountName != null) {
            selection =
                "((${CalendarContract.Events.ACCOUNT_NAME} ${if (doGetOthers) "<>" else "="} ?) AND $selection)"
            selectionArgs = arrayOf(accountName)
        }

        val res = ArrayList<Event>()
        with(context.contentResolver.query(
                uri,
                eventProjection, selection, selectionArgs, null
            )){
            this?.let{cursor->
                while (cursor.moveToNext()) {
                    res.add(parseEventFromCursor(cursor))
                }
            }
        }

        return res
    }

    @SuppressLint("MissingPermission")
    fun getEventSync(context: Context, eventId: Long): Event {
        // Run query
        val uri: Uri = CalendarContract.Events.CONTENT_URI
        val selection = "(${CalendarContract.Events._ID} = ?)"
        val selectionArgs: Array<String?> = arrayOf(eventId.toString())

        val res: Event
        with(
            context.contentResolver.query(
                uri,
                eventProjection, selection, selectionArgs, null
            )
        ) {
            this?.let {cursor->
                moveToNext()

                res = parseEventFromCursor(cursor)

                Log.d(TAG, "event: $res")

                return res
            }
        }

        throw Throwable("cursor null")
    }

    fun deleteEventAsync(
        context: Context,
        eventId: Long,
        observer: CompletableObserver? = null
    ) {
        Completable.fromCallable {
            deleteEventSync(context, eventId)
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .let { completable -> observer?.let { completable.subscribe(observer) } }
    }

    private fun deleteEventSync(context: Context, eventId: Long) {
        val deleteUri: Uri =
            ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
        context.contentResolver.delete(deleteUri, null, null)
    }

    private fun parseEventFromCursor(cursor: Cursor): Event {
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
        val description =
            cursor.getString(cursor.getColumnIndex(CalendarContract.Events.DESCRIPTION)) ?: ""
        val calendarId =
            cursor.getLong(cursor.getColumnIndex(CalendarContract.Events.CALENDAR_ID))

        return Event(
            id,
            title,
            location,
            description,
            startTime,
            endTime,
            accountName,
            calendarId
        )
    }
}