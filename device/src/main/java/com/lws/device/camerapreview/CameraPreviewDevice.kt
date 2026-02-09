package com.lws.device.camerapreview

import android.view.WindowManager
import com.herohan.uvcapp.CameraHelper
import com.lws.device.camerapreview.sample.InternalCameraManager
import com.lws.device.camerapreview.sample.InternalCameraPreview
import com.serenegiant.widget.AspectRatioSurfaceView

interface CameraPreviewDevice {
    fun startExtraCameraEventListener()
    fun getExtraCameraState(): ExtraCameraState
    fun getExtraCamera(): CameraHelper
    fun getInternalCamera(): InternalCameraManager

    fun startInternalCamera(internalCameraPreview: InternalCameraPreview, windowManager: WindowManager, event: CameraPreviewEvent)
    fun rotateCamera(windowManager: WindowManager, event: CameraPreviewEvent)
    fun startExtraCamera(aspectRatioSurfaceView: AspectRatioSurfaceView, event: CameraPreviewEvent)

//    fun setCameraPreview(cameraPreviewType: CameraPreviewType, preview: Any)
//    fun openCamera(
//        cameraPreviewEvent: CameraPreviewEvent,
//        windowManager: WindowManager? = null)
//    fun startCameraPreviewAndCaptureImage()
    fun stopCameraPreview()
    fun getPreviewSize(): Pair<Int, Int>
    fun getResolutionSize(): Pair<Int, Int>
    fun isOpenCamera(): Boolean
}

enum class CameraPreviewType {
    INTERNAL_CAMERA,
    EXTRA_CAMERA,
}

enum class ExtraCameraState {
    CAMERA_NOT_CONNECT,
    CAMERA_DEVICE_ATTACH,
    CAMERA_OPEN,
//    CAMERA_CLOSE,
    CAMERA_DEVICE_DETACH,
}