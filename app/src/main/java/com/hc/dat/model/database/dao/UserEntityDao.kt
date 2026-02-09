package com.hc.dat.model.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hc.dat.model.database.entity.UserEntity

@Dao
interface UserEntityDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(userEntity: UserEntity)

    @Query("SELECT * FROM ${UserEntity.TABLE_NAME} ORDER BY ${UserEntity.ID} DESC")
    fun getListUserEntity(): List<UserEntity>

    @Query("SELECT * FROM ${UserEntity.TABLE_NAME} WHERE ${UserEntity.NFC_ID} = :nfcId LIMIT 1")
    fun getUserByRfidCode(nfcId: String): UserEntity?

    @Query("SELECT * FROM ${UserEntity.TABLE_NAME} WHERE ${UserEntity.USER_CODE} = :userCode LIMIT 1")
    fun getUserByUserCode(userCode: String): UserEntity?

    @Query(
        "UPDATE ${UserEntity.TABLE_NAME} SET " +
            "${UserEntity.USER_NAME} = :userName, " +
            "${UserEntity.GENDER} = :gender, " +
            "${UserEntity.FULL_NAME} = :fullName, " +
            "${UserEntity.PHONE_NUMBER} = :phoneNumber, " +
            "${UserEntity.USER_CODE} = :userCode, " +
            "${UserEntity.ADDRESS} = :address, " +
            "${UserEntity.AVATAR_ID} = :avatarId, " +
            "${UserEntity.TRAINING_CENTER_ID} = :trainingCenterId, " +
            "${UserEntity.BIRTHDAY} = :birthday, " +
            "${UserEntity.COURSE_ID} = :courseId, " +
            "${UserEntity.COURSE_CODE} = :courseCode, " +
            "${UserEntity.COURSE_LICENSE} = :courseLicense, " +
            "${UserEntity.NFC_ID} = :nfcId, " +
            "${UserEntity.TOTAL_TIME_STUDIED} = :totalTimeStudied, " +
            "${UserEntity.TOTAL_DISTANCE_RODE} = :totalDistanceRode, " +
            "${UserEntity.TOTAL_COURSE_TIME} = :totalCourseTime, " +
            "${UserEntity.TOTAL_COURSE_DISTANCE} = :totalCourseDistance " +
            "WHERE ${UserEntity.USER_ID} = :userId"
    )
    fun updateUserBasicInfo(
        userId: String,
        userName: String,
        gender: Int,
        fullName: String,
        phoneNumber: String,
        userCode: String,
        address: String,
        avatarId: String?,
        trainingCenterId: String,
        birthday: String,
        courseId: String?,
        courseCode: String?,
        courseLicense: String?,
        nfcId: String?,
        totalTimeStudied: Double?,
        totalDistanceRode: Float?,
        totalCourseTime: Double?,
        totalCourseDistance: Float?
    )

    @Query(
        "UPDATE ${UserEntity.TABLE_NAME} SET " +
            "${UserEntity.LAST_LOGIN_TYPE} = :lastLoginType " +
            "WHERE ${UserEntity.USER_ID} = :userId"
    )
    fun updateLastLoginType(
        userId: String,
        lastLoginType: Int
    )

    @Query(
        "UPDATE ${UserEntity.TABLE_NAME} SET " +
            "${UserEntity.FACE_GROUP_READY} = :faceGroupReady " +
            "WHERE ${UserEntity.USER_ID} = :userId"
    )
    fun updateFaceGroupReady(
        userId: String,
        faceGroupReady: Boolean
    )

    @Query(
        "UPDATE ${UserEntity.TABLE_NAME} SET " +
            "${UserEntity.TOTAL_TIME_STUDIED} = ${UserEntity.TOTAL_TIME_STUDIED} +  :sessionTimeStudied, " +
            "${UserEntity.TOTAL_DISTANCE_RODE} = ${UserEntity.TOTAL_DISTANCE_RODE} + :sessionDistanceRode " +
            "WHERE ${UserEntity.USER_ID} = :userId"
    )
    fun updateCurrentStudyResult(
        userId: String,
        sessionTimeStudied: Double,
        sessionDistanceRode: Float
    )

//    @Query("SELECT * FROM ${UserEntity.TABLE_NAME} WHERE ${UserEntity.USER_NAME} = :userName")
//    fun getByUserName(userName: String?): UserEntity?

//    @Query("DELETE FROM ${UserEntity.TABLE_NAME} WHERE ${UserEntity.USER_NAME} = :userName")
//    fun delete(userName: String)
}
