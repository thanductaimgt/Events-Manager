package zalo.taitd.calendar.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class EventReminders(
    @PrimaryKey val eventId:Long,
    val remindersId:ArrayList<Long>
)