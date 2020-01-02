package zalo.taitd.calendar.models

import android.provider.CalendarContract

data class Calendar(
    val id:Long,
    val displayName:String,
    val accountName:String,
    val isPrimary:Boolean,
    val accessLevel:Int
){
    fun isEditable():Boolean{
        return accessLevel == CalendarContract.Calendars.CAL_ACCESS_OWNER
    }
}