package com.lws.device.camerapreview

import android.hardware.usb.UsbDevice

interface CameraPreviewEvent {
    fun onTakenPicture(handleStatus: CameraHandlerStatus, imageData: Nv21ImageData?)
    fun extraCameraChangeState(state: ExtraCameraState, device: UsbDevice?)
}

enum class CameraHandlerStatus {
    TAKE_IMAGE_SUCCESS,
}