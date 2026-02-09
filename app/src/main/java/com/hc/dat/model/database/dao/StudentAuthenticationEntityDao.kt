package com.hc.dat.model.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hc.dat.model.database.entity.RiderSessionEntity
import com.hc.dat.model.database.entity.StudentAuthenticationEntity

@Dao
interface StudentAuthenticationEntityDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(studentAuthenticationEntity: StudentAuthenticationEntity)
    @Query(
        "SELECT COUNT(*) FROM ${StudentAuthenticationEntity.TABLE_NAME} " +
                "WHERE ${StudentAuthenticationEntity.LOCAL_SESSION_ID} = :localSessionId" +
                " AND ${StudentAuthenticationEntity.STATE} == 1"
    )
    fun getSuccessfulAuthDataUploadCount(
        localSessionId: Long
    ): Int
    @Query(
        "SELECT COUNT(*) FROM ${StudentAuthenticationEntity.TABLE_NAME} " +
                "WHERE ${StudentAuthenticationEntity.LOCAL_SESSION_ID} = :localSessionId" +
                " AND ${StudentAuthenticationEntity.STATE} != 1"
    )
    fun getOfflineAuthDataCount(
        localSessionId: Long
    ): Int
    @Query(
        "SELECT COUNT(*) FROM ${StudentAuthenticationEntity.TABLE_NAME} " +
                "WHERE ${StudentAuthenticationEntity.LOCAL_SESSION_ID} = :localSessionId"
    )
    fun getTotalAuthDataCount(
        localSessionId: Long
    ): Int
    @Query(
        "SELECT * " +
                "FROM ${StudentAuthenticationEntity.TABLE_NAME} " +
                "WHERE ${StudentAuthenticationEntity.LOCAL_SESSION_ID} = :localSessionId " +
                "AND ${StudentAuthenticationEntity.STATE} != 1" // SENT_ONLINE(1);
    )
    fun getListAuthenDataUploadFailed(
        localSessionId: Long
    ): List<StudentAuthenticationEntity>
    @Query(
        "SELECT * " +
                "FROM ${StudentAuthenticationEntity.TABLE_NAME} " +
                "WHERE ${StudentAuthenticationEntity.LOCAL_SESSION_ID} = :localSessionId " +
                "AND ${StudentAuthenticationEntity.RECOGNITION_RESULT} == 1"
    )
    fun getListAuthDataRecognitionSuccess(
        localSessionId: Long
    ): List<StudentAuthenticationEntity>

    @Query(
        "SELECT * " +
            "FROM ${StudentAuthenticationEntity.TABLE_NAME} " +
            "WHERE ${StudentAuthenticationEntity.LOCAL_SESSION_ID} = :localSessionId"
    )
    fun getListAuthenDataByLocalSessionId(
        localSessionId: Long
    ): List<StudentAuthenticationEntity>

    // SAVE_LOCAL (0)
    @Query(
        "SELECT * " +
            "FROM ${StudentAuthenticationEntity.TABLE_NAME} " +
            "WHERE ${StudentAuthenticationEntity.LOCAL_SESSION_ID} = :localSessionId " +
            " AND ${RiderSessionEntity.STATE} = 0"
    )
    fun getListAuthenOfflineBySessionId(
        localSessionId: Long
    ): List<StudentAuthenticationEntity>

    @Query(
        "UPDATE ${StudentAuthenticationEntity.TABLE_NAME} SET " +
            "${StudentAuthenticationEntity.STATE} = :state " +
            "WHERE ${StudentAuthenticationEntity.ID} = :id"
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
