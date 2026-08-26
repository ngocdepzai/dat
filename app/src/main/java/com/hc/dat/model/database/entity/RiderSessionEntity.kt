package com.hc.dat.model.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.hc.dat.model.InProgressSession
import com.hc.dat.model.database.entity.RiderSessionEntity.Companion.TABLE_NAME

@Entity(
    tableName = TABLE_NAME
)
data class RiderSessionEntity
constructor(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = ID)
    var id: Long = 0,
    @ColumnInfo(name = IMEI)
    var imei: String,
    @ColumnInfo(name = STUDENT_CODE)
    var studentCode: String,
    @ColumnInfo(name = STUDENT_NAME)
    var studentName: String,
    @ColumnInfo(name = PLATE_SLUG)
    var plateSlug: String,
    @ColumnInfo(name = GPS_LAT_START)
    var gpsLatStart: Double,
    @ColumnInfo(name = GPS_LONG_START)
    var gpsLongStart: Double,
    @ColumnInfo(name = LOGIN_TYPE)
    var loginType: Int,
    @ColumnInfo(name = LOGIN_TIME)
    var loginTime: Double,
    @ColumnInfo(name = LOGIN_IMAGE_PATH)
    var loginImagePath: String,
    @ColumnInfo(name = TEACHER_CODE)
    var teacherCode: String,
    @ColumnInfo(name = SESSION_ID)
    var sessionId: String? = null,
    @ColumnInfo(name = GPS_LAT_END)
    var gpsLatEnd: Double? = null,
    @ColumnInfo(name = GPS_LONG_END)
    var gpsLongEnd: Double? = null,
    @ColumnInfo(name = LOGOUT_TIME)
    var logoutTime: Double? = null,
    @ColumnInfo(name = LOGOUT_IMAGE_PATH)
    var logoutImagePath: String? = null,
    @ColumnInfo(name = TOTAL_TIME)
    var totalTime: Double = 0.0,
    @ColumnInfo(name = TOTAL_DISTANCE)
    var totalDistance: Float = 0f,
    @ColumnInfo(name = IS_SEND_TC)
    var isSendTC: Boolean? = null,
    @ColumnInfo(name = STATE)
    var state: Int,
    @ColumnInfo(name = CHECK_INTERRUPT_TIME)
    var checkInterruptTime: Long? = null,
    @ColumnInfo(name = IS_SENT_LOG_STATE)
    var isSentLogState: Boolean? = false,
    @ColumnInfo(name = SESSION_DISRUPTION_COUNT)
    var sessionDisruptionCount: Int = 0,
    @ColumnInfo(name = TIME_IN_24H)
    var timeIn24H: Double? = null,
    @ColumnInfo(name = TIME_24H_TEACHER)
    var time24hTeacher: Double? = null,
    @ColumnInfo(name = LOGIN_STATUS)
    var loginStatus: String? = null

) : CommonEntity() {
    companion object {
        const val TABLE_NAME = "rider_session"
        const val ID = "id"
        const val IMEI = "imei"
        const val STUDENT_CODE = "student_code"
        const val STUDENT_NAME = "student_name"
        const val PLATE_SLUG = "plate_slug"
        const val GPS_LAT_START = "gps_lat_start"
        const val GPS_LONG_START = "gps_long_start"
        const val GPS_LAT_END = "gps_lat_end"
        const val GPS_LONG_END = "gps_long_end"
        const val LOGIN_TYPE = "login_type"
        const val LOGIN_TIME = "login_time"
        const val LOGIN_IMAGE_PATH = "login_image_path"
        const val TEACHER_CODE = "teacher-code"
        const val SESSION_ID = "session_id"
        const val LOGOUT_TIME = "logout_time"
        const val LOGOUT_IMAGE_PATH = "logout_image_path"
        const val TOTAL_TIME = "total_time"
        const val TOTAL_DISTANCE = "total_distance"
        const val IS_SEND_TC = "is_send_tc"
        const val STATE = "state"
        const val CHECK_INTERRUPT_TIME = "check_interrupt_time"
        const val SESSION_DISRUPTION_COUNT = "session_disruption_count"
        const val IS_SENT_LOG_STATE = "sent_log_state"
        const val TIME_IN_24H = "time_in_24h"
        const val TIME_24H_TEACHER = "time_24h_teacher"
        const val LOGIN_STATUS = "login_status"
    }

    fun updateEndSessionInfo(
        gpsLatEnd: Double,
        gpsLongEnd: Double,
        logoutTime: Double,
        logoutImagePath: String,
        totalTime: Double,
        totalDistance: Float,
        isSendTC: Boolean,
        state: Int
    ) {
        this.gpsLatEnd = gpsLatEnd
        this.gpsLongEnd = gpsLongEnd
        this.logoutTime = logoutTime
        this.logoutImagePath = logoutImagePath
        this.totalTime = totalTime
        this.totalDistance = totalDistance
        this.isSendTC = isSendTC
        this.state = state
    }
}

enum class SessionState(val code: Int) {
    UNKNOWN(-1),
    START_OFFLINE(0),
    START_ONLINE(1),
    START_FINISH_OFFLINE(2),
    START_ONLINE_FINISH_OFFLINE(3),
    START_ONLINE_FINISH_ONLINE(4);

    companion object {
        fun findByCode(code: Int): SessionState {
            return values().find {
                it.code == code
            } ?: UNKNOWN
        }
    }
}

fun RiderSessionEntity.convertToModel(
    teacherName: String,
    totalVerifyCounter: Int,
    successVerifyCounter: Int
): InProgressSession {
    return InProgressSession(
        id = sessionId ?: "Offline-or-Unknown",
        localRiderSession = this,
        studentCode = this.studentCode,
        plateSlug = this.plateSlug,
        seri = imei,
        teacherCode = this.teacherCode,
        teacherName = teacherName,
        totalTime = totalTime,
        totalDis = totalDistance,
        totalVerifyCounter = totalVerifyCounter,
        successVerifyCounter = successVerifyCounter,
        startTime = loginTime.toLong(),
        timeIn24H = timeIn24H,
        time24hTeacher = time24hTeacher
    )
}

fun InProgressSession.convertToModelEntity(
    imei: String,
    studentName: String,
    gpsLatStart: Double,
    gpsLongStart: Double,
    loginType: Int,
    loginImagePath: String,
    state: SessionState
): RiderSessionEntity {
    return RiderSessionEntity(
        id = this.localRiderSession?.id ?: 0,
        imei = imei,
        studentCode = this.studentCode ?: "Unknown",
        studentName = studentName,
        gpsLatStart = gpsLatStart,
        gpsLongStart = gpsLongStart,
        plateSlug = this.plateSlug ?: "Unknown",
        loginType = loginType,
        loginTime = this.startTime.toDouble(),
        loginImagePath = loginImagePath,
        teacherCode = this.teacherCode ?: "Unknown",
        sessionId = this.id,
        totalTime = this.totalTime,
        totalDistance = this.totalDis,
        state = state.code,
        timeIn24H = timeIn24H,
        time24hTeacher = time24hTeacher
    )
}
