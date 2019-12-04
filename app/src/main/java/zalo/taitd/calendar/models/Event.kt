package zalo.taitd.calendar.models

import java.util.*

data class Event(
    val id:Long,
    val title:String,
    val location:String,
    val startTime:Date,
    val endTime:Date,
    val accountName:String
){
    companion object{
        const val PAYLOAD_TITLE = 0
        const val PAYLOAD_LOCATION = 1
        const val PAYLOAD_TIME = 2
    }
}