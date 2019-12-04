package zalo.taitd.calendar.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import zalo.taitd.calendar.models.EventReminders

@Database(entities = [EventReminders::class], version = 1, exportSchema = false)
@TypeConverters(ArrayLongTypeConverter::class)
abstract class EventsManagerDatabase :RoomDatabase(){
    abstract fun getDatabaseDAO():DatabaseDAO
}