package com.lws.device.camera

import android.graphics.Bitmap


interface CameraEvent {
    fun onTakenPicture(resultStatus: ResultStatus, bitmap: Bitmap?)
}

enum class ResultStatus {
    SUCCESS,
    ERROR,
    CANCELED
}