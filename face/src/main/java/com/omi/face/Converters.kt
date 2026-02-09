package com.omi.face

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.InstanceCreator
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

class Converters {
    private val gson: Gson = GsonBuilder()
            .registerTypeAdapter(FloatArray::class.java, FloatArrayInstanceCreator())
            .create()

    @TypeConverter
    fun fromFloatArrayList(value: List<FloatArray>?): String? {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toFloatArrayList(value: String?): List<FloatArray>? {
        val listType = object : TypeToken<MutableList<FloatArray>>() {}.type
        return gson.fromJson(value, listType)
    }
}
class FloatArrayInstanceCreator : InstanceCreator<FloatArray> {
    override fun createInstance(type: Type?): FloatArray {
        return FloatArray(0)
    }
}