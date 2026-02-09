package com.lws.device.camerapreview

import DeviceLogger
import android.hardware.usb.UsbDevice
import android.view.SurfaceHolder
import android.view.WindowManager
import com.herohan.uvcapp.CameraHelper
import com.herohan.uvcapp.ICameraHelper
import com.lws.device.camerapreview.sample.InternalCameraManager
import com.lws.device.camerapreview.sample.InternalCameraPreview
import com.lws.type.Logger
import com.serenegiant.usb.UVCCamera
import com.serenegiant.widget.AspectRatioSurfaceView
import kotlinx.coroutines.*


class CameraPreviewHandler: CameraPreviewDevice {
    private var cameraPreviewType: CameraPreviewType? = null
    private var cameraPreviewEvent: CameraPreviewEvent? = null
    private var internalCameraManager: InternalCameraManager = InternalCameraManager()
    private lateinit var internalCameraPreview: InternalCameraPreview
//    private var cameraPreview: Any? = null
    // extra camera
    private lateinit var cameraHelper: CameraHelper
    private lateinit var aspectRatioSurfaceView: AspectRatioSurfaceView
//    private var cameraDevice: UsbDevice? = null
//    private var surfaceWidth = 0
//    private var surfaceHeight = 0
    private var extraCameraState: ExtraCameraState = ExtraCameraState.CAMERA_NOT_CONNECT
    private var surfaceCaptureJob: Job? = null
    private var isOpenCamera: Boolean = false

    override fun getExtraCameraState(): ExtraCameraState = extraCameraState
    override fun getExtraCamera(): CameraHelper = cameraHelper
    override fun getInternalCamera(): InternalCameraManager = internalCameraManager

    override fun startExtraCameraEventListener() {
        DeviceLogger.d("startExtraCameraEventListener")
        cameraHelper = CameraHelper()
        cameraHelper.setStateCallback(object: ICameraHelper.StateCallback {
            override fun onAttach(device: UsbDevice?) {
                DeviceLogger.d("ExtraCamera onAttach device: $device")
                extraCameraState = ExtraCameraState.CAMERA_DEVICE_ATTACH
//                cameraDevice = device
                cameraHelper.selectDevice(device)
                cameraPreviewEvent?.extraCameraChangeState(state = extraCameraState, device = device)
            }

            override fun onDeviceOpen(device: UsbDevice?, isFirstOpen: Boolean) {
                DeviceLogger.d("ExtraCamera onDeviceOpen device: $device | isFirstOpen: $isFirstOpen")
                cameraHelper.openCamera()
            }

            override fun onCameraOpen(device: UsbDevice?) {
                DeviceLogger.d("ExtraCamera onCameraOpen device: $device")
                extraCameraState = ExtraCameraState.CAMERA_OPEN
                cameraPreviewEvent?.extraCameraChangeState(state = extraCameraState, device = device)
            }

            override fun onCameraClose(device: UsbDevice?) {
                DeviceLogger.d("ExtraCamera onCameraClose device: $device")

                // change to CAMERA_DEVICE_ATTACH after camera close
//                extraCameraState = ExtraCameraState.CAMERA_DEVICE_ATTACH
//                cameraPreviewEvent?.extraCameraChangeState(state = extraCameraState, device = device)
            }

            override fun onDeviceClose(device: UsbDevice?) {
                extraCameraState = ExtraCameraState.CAMERA_DEVICE_DETACH
                cameraPreviewEvent?.extraCameraChangeState(state = extraCameraState, device = device)
                DeviceLogger.d("ExtraCamera onDeviceClose device: $device")
            }

            override fun onDetach(device: UsbDevice?) {
                DeviceLogger.d("ExtraCamera onDetach device: $device")
                extraCameraState = ExtraCameraState.CAMERA_DEVICE_DETACH
                cameraPreviewEvent?.extraCameraChangeState(state = extraCameraState, device = device)
            }

            override fun onCancel(device: UsbDevice?) {
                DeviceLogger.d("ExtraCamera onCancel device: $device")
//                extraCameraState = ExtraCameraState.CAMERA_DEVICE_DETACH
//                cameraPreviewEvent?.extraCameraChangeState(state = extraCameraState, device = device)
            }
        })
    }

    override fun startInternalCamera(
        cameraView: InternalCameraPreview,
        windowManager: WindowManager,
        event: CameraPreviewEvent
    ) {
        this.cameraPreviewType = CameraPreviewType.INTERNAL_CAMERA
        this.cameraPreviewEvent = event
        this.internalCameraPreview = cameraView

        internalCameraManager.setPreviewDisplay(internalCameraPreview)
        internalCameraManager.open(windowManager, true)
        isOpenCamera = true
        internalCameraManager.setListener { cameraPreviewData ->
            cameraPreviewEvent?.onTakenPicture(
                handleStatus = CameraHandlerStatus.TAKE_IMAGE_SUCCESS,
                imageData = cameraPreviewData.convertToNv21ImageData()
            )
        }
    }

    override fun rotateCamera(
        windowManager: WindowManager,
        event: CameraPreviewEvent
    ) {
        internalCameraManager.release()
        internalCameraManager.rotateCamera(windowManager)
    }

    override fun startExtraCamera(surfaceView: AspectRatioSurfaceView, event: CameraPreviewEvent) {
        this.cameraPreviewType = CameraPreviewType.EXTRA_CAMERA
        this.cameraPreviewEvent = event
        this.aspectRatioSurfaceView = surfaceView

        // if surface view created -> re call initExtraCameraPreview
        Logger.i("startExtraCamera surface view created: ${aspectRatioSurfaceView.isSurfaceHolderCreated}")
        if (aspectRatioSurfaceView.isSurfaceHolderCreated) {
            initExtraCameraPreview(aspectRatioSurfaceView.holder)
        }

        isOpenCamera = true

        aspectRatioSurfaceView.holder.addCallback(object: SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                DeviceLogger.d("surfaceCreated")
//                cameraHelper.addSurface(holder.surface, false)
                initExtraCameraPreview(holder)
                aspectRatioSurfaceView.setSurfaceHolderCreatedState(true)
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                DeviceLogger.d("surfaceChanged width: $width | height: $height")
                aspectRatioSurfaceView.setSize(width, height)
                aspectRatioSurfaceView.setAspectRatio(aspectRatioSurfaceView.surfaceWidth, aspectRatioSurfaceView.surfaceHeight)
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                DeviceLogger.d("surfaceDestroyed")
                cameraHelper.removeSurface(holder.surface)
                aspectRatioSurfaceView.setSurfaceHolderCreatedState(false)
            }

        })
    }

    private fun initExtraCameraPreview(holder: SurfaceHolder) {
        if (extraCameraState == ExtraCameraState.CAMERA_OPEN) {
            DeviceLogger.d("surfaceCreated surfaceWidth: ${aspectRatioSurfaceView.surfaceWidth} | surfaceHeight: ${aspectRatioSurfaceView.surfaceHeight}")
            cameraHelper.addSurface(holder.surface, false)
            aspectRatioSurfaceView.setAspectRatio(aspectRatioSurfaceView.surfaceWidth, aspectRatioSurfaceView.surfaceHeight)
            cameraHelper.addSurface(holder.surface, false)
            cameraHelper.startPreview()
            cameraHelper.setFrameCallback({ frame ->
//                        DeviceLogger.d("setFrameCallback")
                val nv21 = ByteArray(frame.remaining())
                frame[nv21, 0, nv21.size]
                this@CameraPreviewHandler.cameraPreviewEvent?.onTakenPicture(
                    handleStatus = CameraHandlerStatus.TAKE_IMAGE_SUCCESS,
                    imageData = Nv21ImageData(
                        nv21Data = nv21,
                        width = cameraHelper.previewSize.width,
                        height = cameraHelper.previewSize.height,
                        rotation = 0,
                        mirror = true
                    )
                ) },
                UVCCamera.PIXEL_FORMAT_NV21
            )
        } else {
            DeviceLogger.e("Extra Camera haven't open!")
        }
    }

    override fun stopCameraPreview() {
        if (cameraPreviewType == CameraPreviewType.INTERNAL_CAMERA) {
            internalCameraManager.setListener(null)
            internalCameraManager.release()
        } else if (cameraPreviewType == CameraPreviewType.EXTRA_CAMERA) {
//            surfaceCaptureJob?.cancel()
            // remove callback
            cameraHelper.setFrameCallback(null, UVCCamera.PIXEL_FORMAT_NV21)
            cameraHelper.removeSurface(aspectRatioSurfaceView.holder.surface)
            cameraHelper.stopPreview()
        }
        cameraPreviewEvent = null

        isOpenCamera = false
    }

    override fun getPreviewSize(): Pair<Int, Int> {
        return if (cameraPreviewType == CameraPreviewType.INTERNAL_CAMERA) {
            Pair(internalCameraPreview.measuredWidth, internalCameraPreview.measuredHeight)
        } else if (cameraPreviewType == CameraPreviewType.EXTRA_CAMERA) {
            Pair(aspectRatioSurfaceView.surfaceWidth, aspectRatioSurfaceView.surfaceHeight)
        } else Pair(0,0)
    }

    override fun getResolutionSize(): Pair<Int, Int> {
        return if (cameraPreviewType == CameraPreviewType.INTERNAL_CAMERA) {
            Pair(internalCameraManager.cameraWidth, internalCameraManager.cameraheight)
        } else if (cameraPreviewType == CameraPreviewType.EXTRA_CAMERA) {
            Pair(cameraHelper.previewSize.width,cameraHelper.previewSize.height)
        } else Pair(0,0)
    }

    override fun isOpenCamera(): Boolean = isOpenCamera
}