package zalo.taitd.calendar.models

data class Calendar(
    val id:Long,
    val displayName:String,
    val accountName:String,
    val isPrimary:Boolean
)