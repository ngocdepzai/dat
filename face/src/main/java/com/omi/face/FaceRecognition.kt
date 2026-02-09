package com.omi.face

import android.graphics.Bitmap
import android.graphics.Rect
import com.omi.face.model.database.entity.SampleFaceRecognition


interface FaceRecognition {
    fun addSampleFaces(
        id: String,
        groupName: String,
        bitmap: Bitmap,
        resultCallback: (state: AddFaceResult, message: String?, successCounter: Int) -> Unit
    )

    fun startRecognition(
            faceGroupName: String?,
            resultCallback: (
            searchScore: Int,
            faceBitmap: Bitmap?,
            rect: Rect?,
            notFace: Boolean,
            notMask: Boolean,
            ) -> Unit
    )
    suspend fun getAllFaceSample(): List<SampleFaceRecognition>
    suspend fun deleteFaceSample(name: String)
    fun stopRecognition()
    fun resetDetectResult()
}