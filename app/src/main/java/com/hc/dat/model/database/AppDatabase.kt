package com.hc.dat.model.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 6
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userEntityDao(): UserEntityDao
    abstract fun faceRecogImageEntityDao(): FaceRecogImageEntityDao
    abstract fun riderSessionEntityDao(): RiderSessionEntityDao
    abstract fun gpsSignalEntityDao(): GPSSignalEntityDao
    abstract fun studentAuthenticationEntityDao(): StudentAuthenticationEntityDao

    companion object {
        private const val DATABASE_NAME = "application_db"

        // ALTER TABLE thay vì để fallbackToDestructiveMigration xoá bảng: rider_session
        // chứa các phiên offline chưa upload lên server, mất là mất luôn dữ liệu học viên.
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE ${RiderSessionEntity.TABLE_NAME}" +
                        " ADD COLUMN ${RiderSessionEntity.TIME_24H_TEACHER} REAL"
                )
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            if (INSTANCE == null) {
                INSTANCE = Room.databaseBuilder(
                    context,
                    AppDatabase::class.java,
                    DATABASE_NAME
                ).addMigrations(MIGRATION_5_6)
                    .fallbackToDestructiveMigration()
                    .build()
            }
            return INSTANCE!!
        }
    }
}
