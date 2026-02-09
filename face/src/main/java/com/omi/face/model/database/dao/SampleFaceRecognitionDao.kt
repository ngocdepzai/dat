package com.omi.face.model.database.dao

import com.omi.face.model.database.entity.SampleFaceRecognition
import androidx.room.*

@Dao
interface SampleFaceRecognitionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(sampleFaceRecognition: SampleFaceRecognition)

    @Query("SELECT * FROM sample_face_recognition")
    fun getAll(): List<SampleFaceRecognition>

    @Query("SELECT * FROM ${"sample_face_recognition"} WHERE ${"name"} = :groupName")
    fun findSampleFaceByGroupName(
            groupName: String
    ): SampleFaceRecognition?

    @Update
    fun updateSampleFaceRecognition(sample: SampleFaceRecognition)

    @Query("DELETE FROM ${"sample_face_recognition"} WHERE ${"name"} = :groupName")
    fun deleteSampleFaceByGroupName(groupName: String)
}