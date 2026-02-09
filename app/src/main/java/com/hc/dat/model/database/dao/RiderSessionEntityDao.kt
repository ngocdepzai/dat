package com.hc.dat.model.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hc.dat.model.database.entity.RiderSessionEntity

@Dao
interface RiderSessionEntityDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(riderSessionEntity: RiderSessionEntity)

    @Query("SELECT * FROM ${RiderSessionEntity.TABLE_NAME}")
    fun getListRiderSessionEntity(): List<RiderSessionEntity>

    // START_ONLINE_FINISH_ONLINE (4)
    @Query("SELECT * FROM ${RiderSessionEntity.TABLE_NAME} WHERE ${RiderSessionEntity.STATE} != 4")
    fun getListSessionNotFinishOnline(): List<RiderSessionEntity>

    @Query("SELECT * FROM ${RiderSessionEntity.TABLE_NAME} WHERE ${RiderSessionEntity.IS_SENT_LOG_STATE} == :sentLogState")
    fun getListLogFilePushFail(sentLogState: Boolean): List<RiderSessionEntity>

    @Query(
        "UPDATE ${RiderSessionEntity.TABLE_NAME} SET " +
                "${RiderSessionEntity.IS_SENT_LOG_STATE} = :sentLogState " +
                "WHERE ${RiderSessionEntity.ID} = :id"
    )
    fun updateLogState(
        id: Long,
        sentLogState: Boolean,
    )

    @Query("SELECT * FROM ${RiderSessionEntity.TABLE_NAME} WHERE ${RiderSessionEntity.SESSION_ID} = :sessionId")
    fun findSessionBySessionId(
        sessionId: String
    ): RiderSessionEntity?

    @Query("SELECT * FROM ${RiderSessionEntity.TABLE_NAME} ORDER BY ${RiderSessionEntity.ID} DESC LIMIT 1")
    fun getLastRiderSession(): RiderSessionEntity?

    @Query(
        "UPDATE ${RiderSessionEntity.TABLE_NAME} SET " +
            "${RiderSessionEntity.TOTAL_TIME} = :totalTime, " +
            "${RiderSessionEntity.TOTAL_DISTANCE} = :totalDistance " +
            "WHERE ${RiderSessionEntity.ID} = :id"
    )
    fun updateDrivingProgress(
        id: Long,
        totalTime: Double,
        totalDistance: Float
    )

    @Query(
        "UPDATE ${RiderSessionEntity.TABLE_NAME} SET " +
                "${RiderSessionEntity.CHECK_INTERRUPT_TIME} = :checkInterruptTime " +
                "WHERE ${RiderSessionEntity.ID} = :id"
    )
    fun updateCheckInterruptTime(
        id: Long,
        checkInterruptTime: Long,
    )

    @Query(
        "UPDATE ${RiderSessionEntity.TABLE_NAME} SET " +
                "${RiderSessionEntity.LOGIN_STATUS} = :loginStatus " +
                "WHERE ${RiderSessionEntity.ID} = :id"
    )
    fun updateLoginStatus(
        id: Long,
        loginStatus: String,
    )

    @Query(
        "UPDATE ${RiderSessionEntity.TABLE_NAME} SET " +
                "${RiderSessionEntity.SESSION_DISRUPTION_COUNT} = :sessionDisruptionCount " +
                "WHERE ${RiderSessionEntity.ID} = :id"
    )
    fun updateSessionDisruptionCount(
        id: Long,
        sessionDisruptionCount: Int,
    )

    @Query(
        "UPDATE ${RiderSessionEntity.TABLE_NAME} SET " +
            "${RiderSessionEntity.GPS_LAT_END} = :gpsLatEnd, " +
            "${RiderSessionEntity.GPS_LONG_END} = :gpsLongEnd, " +
            "${RiderSessionEntity.LOGOUT_TIME} = :logoutTime, " +
            "${RiderSessionEntity.LOGOUT_IMAGE_PATH} = :logoutImagePath, " +
            "${RiderSessionEntity.TOTAL_TIME} = :totalTime, " +
            "${RiderSessionEntity.TOTAL_DISTANCE} = :totalDistance, " +
            "${RiderSessionEntity.IS_SEND_TC} = :isSendTC, " +
            "${RiderSessionEntity.STATE} = :state " +
            "WHERE ${RiderSessionEntity.ID} = :id"
    )
    fun updateEndSessionInfo(
        id: Long,
        gpsLatEnd: Double,
        gpsLongEnd: Double,
        logoutTime: Double,
        logoutImagePath: String,
        totalTime: Double,
        totalDistance: Float,
        isSendTC: Boolean,
        state: Int
    )

    @Query(
        "UPDATE ${RiderSessionEntity.TABLE_NAME} SET " +
            "${RiderSessionEntity.SESSION_ID} = :sessionId, " +
            "${RiderSessionEntity.STATE} = :state " +
            "WHERE ${RiderSessionEntity.ID} = :id"
    )
    fun updateStartSessionToOnline(
        id: Long,
        sessionId: String,
        state: Int
    )

    @Query(
        "UPDATE ${RiderSessionEntity.TABLE_NAME} SET " +
            "${RiderSessionEntity.STATE} = :state " +
            "WHERE ${RiderSessionEntity.ID} = :id"
    )
    fun updateUploadState(
        id: Long,
        state: Int
    )

//    @Query("SELECT * FROM ${UserEntity.TABLE_NAME} WHERE ${UserEntity.USER_NAME} = :userName")
//    fun getByUserName(userName: String?): UserEntity?

//    @Query("DELETE FROM ${UserEntity.TABLE_NAME} WHERE ${UserEntity.USER_NAME} = :userName")
//    fun delete(userName: String)
}
