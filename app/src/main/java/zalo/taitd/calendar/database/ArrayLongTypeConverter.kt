package zalo.taitd.calendar.database

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class ArrayLongTypeConverter {
    @TypeConverter
    fun fromArray(arrayList: ArrayList<Long>):String{
        return Gson().toJson(arrayList)
    }

    @TypeConverter
    fun fromString(string: String):ArrayList<Long>{
        return Gson().fromJson(string, object : TypeToken<ArrayList<Long>>(){}.type)
    }
}