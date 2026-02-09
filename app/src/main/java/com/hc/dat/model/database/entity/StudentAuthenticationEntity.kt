package com.hc.dat.model.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.hc.dat.model.database.entity.StudentAuthenticationEntity.Companion.TABLE_NAME
import java.io.Serializable

@Entity(
    tableName = TABLE_NAME
)
data class StudentAuthenticationEntity
constructor(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = ID)
    var id: Long = 0,
    @ColumnInfo(name = LOCAL_SESSION_ID)
    var localSessionId: Long,
    @ColumnInfo(name = GPS_LAT)
    var gpsLat: Double,
    @ColumnInfo(name = GPS_LONG)
    var gpsLong: Double,
    @ColumnInfo(name = GPS_DISTANCE)
    var gpsDistance: Float,
    @ColumnInfo(name = GPS_SPEED)
    var gpsSpeed: Double,
    @ColumnInfo(name = AUTHEN_IMAGE_PATH)
    var authenImagePath: String,
    @ColumnInfo(name = TIME)
    var time: Long,
    @ColumnInfo(name = RECOGNITION_RESULT)
    var recognitionResult: Int,
    @ColumnInfo(name = STATE)
    var state: Int,
    @ColumnInfo(name = SEARCH_SCORE)
    var searchScore: Float
) : CommonEntity(), Serializable {
    companion object {
        const val TABLE_NAME = "student_authentication"
        const val ID = "id"
        const val LOCAL_SESSION_ID = "local_session_id"
        const val GPS_LAT = "gps_lat"
        const val GPS_LONG = "gps_long"
        const val GPS_DISTANCE = "gps_distance"
        const val AUTHEN_IMAGE_PATH = "authen_image_path"
        const val GPS_SPEED = "gps_speed"
        const val TIME = "time"
        const val RECOGNITION_RESULT = "recognition_result"
        const val STATE = "state"
        const val SEARCH_SCORE = "search_score"
    }
}

enum class AuthenUploadState(val code: Int) {
    UNKNOWN(-1),
    SAVE_LOCAL(0),
    SENT_ONLINE(1);

    companion object {
        fun findByCode(code: Int): AuthenUploadState {
            return values().find {
                it.code == code
            } ?: UNKNOWN
        }
    }
}
