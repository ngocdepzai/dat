package com.lws.device.camera

import android.app.Activity
import android.content.Intent

interface CameraDevice {
    val activityResultCallback: (activity: Activity, requestCode: Int, resultCode: Int, data: Intent?) -> Unit
    val requestPermissionResultCallback: (
        activity: Activity,
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) -> Unit
    suspend fun takePicture(activity: Activity, cameraEvent: CameraEvent)
}
