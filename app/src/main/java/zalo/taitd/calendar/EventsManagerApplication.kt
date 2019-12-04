package zalo.taitd.calendar

import android.app.Application
import androidx.room.Room
import zalo.taitd.calendar.database.EventsManagerDatabase

class EventsManagerApplication :Application(){
    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(this, EventsManagerDatabase::class.java, EventsManagerDatabase::class.java.simpleName).build()
    }

    companion object{
        lateinit var database : EventsManagerDatabase
    }
}