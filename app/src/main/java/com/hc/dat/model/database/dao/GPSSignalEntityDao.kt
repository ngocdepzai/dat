package com.hc.dat.model.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hc.dat.model.database.entity.GPSSignalEntity

@Dao
interface GPSSignalEntityDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(gpsSignalEntity: GPSSignalEntity)

    @Query(
        "SELECT * FROM ${GPSSignalEntity.TABLE_NAME} " +
                "WHERE ${GPSSignalEntity.LOCAL_SESSION_ID} = :localSessionId " +
                "AND ${GPSSignalEntity.STATE} != 1" // SENT_ONLINE(1);
    )
    fun getListGpsDataUploadFailed(
        localSessionId: Long
    ): List<GPSSignalEntity>

    @Query(
        "SELECT * FROM ${GPSSignalEntity.TABLE_NAME} " +
            "WHERE ${GPSSignalEntity.LOCAL_SESSION_ID} = :localSessionId"
    )
    fun getListGPSSignalByLocalSessionId(
        localSessionId: Long
    ): List<GPSSignalEntity>

    @Query(
        "SELECT COUNT(*) FROM ${GPSSignalEntity.TABLE_NAME} " +
                "WHERE ${GPSSignalEntity.LOCAL_SESSION_ID} = :localSessionId" +
                " AND ${GPSSignalEntity.STATE} == 1"
    )
    fun getSuccessfulGPSUploadsCount(
        localSessionId: Long
    ): Int

    @Query(
        "SELECT COUNT(*) FROM ${GPSSignalEntity.TABLE_NAME} " +
                "WHERE ${GPSSignalEntity.LOCAL_SESSION_ID} = :localSessionId" +
                " AND ${GPSSignalEntity.STATE} != 1"
    )
    fun getFailureGPSUploadsCount(
        localSessionId: Long
    ): Int

    @Query(
        "SELECT COUNT(*) FROM ${GPSSignalEntity.TABLE_NAME} " +
                "WHERE ${GPSSignalEntity.LOCAL_SESSION_ID} = :localSessionId"
    )
    fun getTotalGPSCount(
        localSessionId: Long
    ): Int

    // SAVE_LOCAL (0)
    @Query(
        "SELECT * FROM ${GPSSignalEntity.TABLE_NAME} WHERE ${GPSSignalEntity.LOCAL_SESSION_ID} = :localSessionId " +
            " AND ${GPSSignalEntity.STATE} = 0"
    )
    fun getListGpsSignalOfflineBySessionId(
        localSessionId: Long
    ): List<GPSSignalEntity>

    @Query(
        "UPDATE ${GPSSignalEntity.TABLE_NAME} SET " +
            "${GPSSignalEntity.STATE} = :state " +
            "WHERE ${GPSSignalEntity.ID} = :id"
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
