package com.hc.dat.model.database.dao

import androidx.room.*
import com.hc.dat.model.database.entity.FaceRecognitionImageEntity

@Dao
interface FaceRecogImageEntityDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(entity: FaceRecognitionImageEntity)

    @Query("SELECT * FROM ${FaceRecognitionImageEntity.TABLE_NAME}")
    fun getAllFaceRecogImage(): List<FaceRecognitionImageEntity>

//    @Query("SELECT * FROM ${UserEntity.TABLE_NAME} WHERE ${UserEntity.USER_NAME} = :userName")
//    fun getByUserName(userName: String?): UserEntity?

    @Query("DELETE FROM ${FaceRecognitionImageEntity.TABLE_NAME} WHERE ${FaceRecognitionImageEntity.IMAGE_ID} = :imageId")
    fun deleteFaceRecogImage(imageId: String)
}
