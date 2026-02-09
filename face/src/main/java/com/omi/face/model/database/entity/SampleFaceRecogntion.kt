package com.omi.face.model.database.entity

import com.omi.face.Converters
import androidx.room.*

@Entity(tableName = "sample_face_recognition")
data class SampleFaceRecognition(
        @PrimaryKey(autoGenerate = true)
        val id: Int = 0,
        val name: String,
        @TypeConverters(Converters::class) val listVectorEmbedding: MutableList<FloatArray>
)