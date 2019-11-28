package zalo.taitd.calendar

data class Reminder(
    val id:Long,
    val eventId:Long,
    val minutes:Int,
    val method:Int
)