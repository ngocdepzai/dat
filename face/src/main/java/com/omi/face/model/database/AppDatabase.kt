package com.omi.face.model.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.omi.face.Converters
import com.omi.face.model.database.dao.*
import com.omi.face.model.database.entity.*

@Database(
        entities = [
            SampleFaceRecognition::class
        ],
        version = 1,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sampleFaceRecognitionDao(): SampleFaceRecognitionDao

    companion object {
        private const val DATABASE_NAME = "application_db1"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            if (INSTANCE == null) {
                INSTANCE = Room.databaseBuilder(
                        context,
                        AppDatabase::class.java,
                        DATABASE_NAME
                ).fallbackToDestructiveMigration().build()
            }
            return INSTANCE!!
        }
    }
}
