package com.hc.dat.model.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.hc.dat.model.database.entity.FaceRecognitionImageEntity.Companion.TABLE_NAME

@Entity(
    tableName = TABLE_NAME
)
data class FaceRecognitionImageEntity
constructor(
    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = IMAGE_ID)
    var imageId: String,
    @ColumnInfo(name = USER_ID)
    var userId: String,
    @ColumnInfo(name = FACE_SAMPLE_ADDED)
    var faceSampleAdded: Boolean = false
) : CommonEntity() {
    companion object {
        const val TABLE_NAME = "face_recognition_image"
        const val USER_ID = "user_id"
        const val IMAGE_ID = "image_id"
        const val FACE_SAMPLE_ADDED = "face_sample_added"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FaceRecognitionImageEntity

        if (userId != other.userId) return false
        if (imageId != other.imageId) return false
        return true
    }

    override fun hashCode(): Int {
        var result = userId.hashCode()
        result = 31 * result + imageId.hashCode()
        return result
    }
}
