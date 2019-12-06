package zalo.taitd.calendar.databases

import androidx.room.*
import zalo.taitd.calendar.models.EventReminders

@Dao
interface DatabaseDAO {
    @Query("select * from EventReminders where eventId in (:eventsId)")
    fun getEventsReminders(eventsId:List<Long>):List<EventReminders>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertEventsReminders(eventsReminders: List<EventReminders>)

    @Delete
    fun deleteEventsReminders(eventsReminders: List<EventReminders>)
}