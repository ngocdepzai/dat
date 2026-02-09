package com.hc.dat.model.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.hc.dat.model.database.entity.GPSSignalEntity.Companion.TABLE_NAME

@Entity(
    tableName = TABLE_NAME
)
data class GPSSignalEntity
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
    @ColumnInfo(name = GPS_STATUS)
    var gpsStatus: Int,
    @ColumnInfo(name = GSM_STATUS)
    var gsmStatus: Int,
    @ColumnInfo(name = TIME)
    var time: Long,
    @ColumnInfo(name = STATE)
    var state: Int
) : CommonEntity() {
    companion object {
        const val TABLE_NAME = "gps_signal"
        const val ID = "id"
        const val LOCAL_SESSION_ID = "local_session_id"
        const val GPS_LAT = "gps_lat"
        const val GPS_LONG = "gps_long"
        const val GPS_DISTANCE = "gps_distance"
        const val GPS_STATUS = "gps_status"
        const val GPS_SPEED = "gps_speed"
        const val GSM_STATUS = "gsm_status"
        const val TIME = "time"
        const val STATE = "state"
    }
}

enum class GPSUploadState(val code: Int) {
    UNKNOWN(-1),
    SAVE_LOCAL(0),
    SENT_ONLINE(1);

    companion object {
        fun findByCode(code: Int): GPSUploadState {
            return values().find {
                it.code == code
            } ?: UNKNOWN
        }
    }
}
