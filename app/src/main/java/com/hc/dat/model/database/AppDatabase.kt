package com.hc.dat.model.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.hc.dat.model.database.dao.*
import com.hc.dat.model.database.entity.*

@Database(
    entities = [
        UserEntity::class,
        FaceRecognitionImageEntity::class,
        RiderSessionEntity::class,
        GPSSignalEntity::class,
        StudentAuthenticationEntity::class
    ],
    version = 5
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userEntityDao(): UserEntityDao
    abstract fun faceRecogImageEntityDao(): FaceRecogImageEntityDao
    abstract fun riderSessionEntityDao(): RiderSessionEntityDao
    abstract fun gpsSignalEntityDao(): GPSSignalEntityDao
    abstract fun studentAuthenticationEntityDao(): StudentAuthenticationEntityDao

    companion object {
        private const val DATABASE_NAME = "application_db"

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
