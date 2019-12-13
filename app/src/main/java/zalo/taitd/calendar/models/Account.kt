package zalo.taitd.calendar.models

data class Account(
    val name:String,
    val calendars:ArrayList<Calendar> = ArrayList()
)