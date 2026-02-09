package com.hc.dat.view.dialog

import android.app.Activity
import android.graphics.*
import android.hardware.usb.UsbDevice
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.hc.dat.utils.Utils
import com.hc.dat.viewmodel.FaceRecognitionViewModel
import com.lws.device.camerapreview.*
import com.lws.type.Logger
import hc.manager.datapp.R
import hc.manager.datapp.databinding.DatNfcLoginDialogBinding
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ArrayBlockingQueue

internal object NFCLoginDialog {
    private var dialog: AlertDialog? = null
    private lateinit var viewBinding: DatNfcLoginDialogBinding

    //    private var cameraManager: CameraManager? = null
//    private var cameraView: CameraPreview? = null
    private lateinit var cameraPreviewDevice: CameraPreviewDevice
    private var lastPreviewData: Nv21ImageData? = null
    private var dataCallback: ((imageFile: File?) -> Unit?)? = null
    private var hcImageFolder: File =
        File(Environment.getExternalStorageDirectory().toString() + "/HC_DAT_IMAGES")

    //    private var cameraPreviewDataQueue: ArrayBlockingQueue<CameraPreviewData> = ArrayBlockingQueue(10)
    private var cameraPreviewDataQueue: ArrayBlockingQueue<Nv21ImageData> = ArrayBlockingQueue(1)
    private var cameraRotation = 0
    private var notFace = true
    private lateinit var faceRecognitionViewModel: FaceRecognitionViewModel

    init {
        if (!hcImageFolder.exists()) {
            hcImageFolder.mkdirs()
        }
    }

    fun showDialog(
        activity: Activity,
        isTeacher: Boolean,
        cameraRotation: Int,
        faceRecognitionViewModel: FaceRecognitionViewModel,
        callback: ((imageFile: File?) -> Unit?)? = null
    ) {
        Logger.d("showDialog isTeacher: $isTeacher")
        this.cameraPreviewDevice = faceRecognitionViewModel.getCameraPreviewDevice()!!
        this.cameraRotation = cameraRotation
        this.faceRecognitionViewModel = faceRecognitionViewModel
        dataCallback = callback
        val view = LayoutInflater.from(activity)
            .inflate(R.layout.dat_nfc_login_dialog, null, false)
        viewBinding = DatNfcLoginDialogBinding.bind(view)
        viewBinding.rlCameraPreview.visibility = if (isTeacher) View.GONE else View.VISIBLE
        if (!isTeacher) {
            Logger.i("ExtraCameraState: ${cameraPreviewDevice.getExtraCameraState()}")
            if (cameraPreviewDevice.getExtraCameraState() == ExtraCameraState.CAMERA_OPEN) {
                viewBinding.extraCamera = true
                cameraPreviewDevice.startExtraCamera(
                    aspectRatioSurfaceView = viewBinding.surfaceView,
                    event = cameraPreviewEvent
                )
            } else {
                viewBinding.extraCamera = false
                cameraPreviewDevice.startInternalCamera(
                    internalCameraPreview = viewBinding.previewCamera,
                    windowManager = activity.windowManager,
                    event = cameraPreviewEvent
                )
            }

            CoroutineScope(Dispatchers.Default).launch {
                delay(1000)
                handleCollectFaceDetected()
            }
        }

        dialog?.run {
            if (isShowing) dismiss()
        }

        dialog = MaterialAlertDialogBuilder(activity)
            .setCancelable(true)
            .setView(viewBinding.root).create()
        dialog?.setOnShowListener {
            dialog?.window?.setLayout(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        dialog?.setOnDismissListener {
            Logger.d("setOnDismissListener")
            handleSaveImageFile()
            releaseResource()
        }
        dialog?.setOnCancelListener {
            Logger.d("setOnCancelListener")
            releaseResource()
        }

        dialog?.show()
    }

    private val cameraPreviewEvent = object : CameraPreviewEvent {
        override fun onTakenPicture(
            handleStatus: CameraHandlerStatus,
            imageData: Nv21ImageData?
        ) {
//                    Logger.i("onTakenPicture imageData: $imageData")
            imageData?.also {
                cameraPreviewDataQueue.offer(imageData)
            }
        }

        override fun extraCameraChangeState(state: ExtraCameraState, device: UsbDevice?) {
            Logger.d("extraCameraChangeState state: $state | device: $device")
            when (state) {
                ExtraCameraState.CAMERA_OPEN -> {
                    // must call start preview for extra camera
                }
                ExtraCameraState.CAMERA_DEVICE_ATTACH -> {
//                            viewBinding.extraCamera = true
//                            cameraPreviewDevice.setCameraPreview(
//                                cameraPreviewType = CameraPreviewType.EXTRA_CAMERA,
//                                preview = viewBinding.surfaceView
//                            )
//                            cameraPreviewDevice.openCamera(this, null)
                }
                ExtraCameraState.CAMERA_DEVICE_DETACH -> {
//                            viewBinding.extraCamera = false
//                            cameraPreviewDevice.setCameraPreview(
//                                cameraPreviewType = CameraPreviewType.INTERNAL_CAMERA,
//                                preview = viewBinding.previewCamera
//                            )
//                            cameraPreviewDevice.openCamera(this, requireActivity().windowManager)
                }
                else -> {
                    Logger.w("Not handle state: $state")
                }
            }
        }
    }

    private fun releaseResource() {
//        cameraManager?.release()
        cameraPreviewDevice.stopCameraPreview()
        cameraPreviewDataQueue.clear()
//        lastPreviewData = null
    }

    fun dismiss() {
        Logger.d("dismiss")
        dialog?.dismiss()
    }

    private fun handleSaveImageFile() {
        CoroutineScope(Dispatchers.Default).launch(
            CoroutineExceptionHandler { _, _ ->
                Logger.w("Error in progress -> handleSaveImageFile")
                dataCallback?.let { callback -> callback(null) }
            }
        ) {
            // Convert preview camera to image file
//            Logger.i("lastPreviewData: $lastPreviewData")
            lastPreviewData?.also { previewData ->
                val fileTemp = File(hcImageFolder, "nfc_login_${Utils.getRealTimeStamp()}.png")
                fileTemp.apply {
                    if (exists()) delete()
                    createNewFile()
                    val fos = FileOutputStream(this)
                    val yuvImage = YuvImage(
                        previewData.nv21Data,
                        ImageFormat.NV21,
                        previewData.width,
                        previewData.height,
                        null
                    )
//                    yuvImage.compressToJpeg(Rect(0, 0, previewData.width, previewData.height), 100, fos)
                    //            Logger.i("handlePushAuthData image width: ${faceImageData.nv21ImageData.width} | height: ${faceImageData.nv21ImageData.height}")
                    val imageRatio: Float = 800.0F / previewData.width.toFloat()
                    val quality = (100F * imageRatio).toInt().coerceIn(0, 100)
//            Logger.i("handlePushAuthData imageRatio: $imageRatio | quality: $quality")
                    yuvImage.compressToJpeg(
                        Rect(0, 0, previewData.width, previewData.height),
                        quality,
                        fos
                    )
                    fos.flush()
                    fos.close()
                    Logger.d("Save image temporary success")
                }
                dataCallback?.let { callback -> callback(fileTemp) }
                // reset
                lastPreviewData = null
            } ?: run {
                dataCallback?.let { callback -> callback(null) }
            }
        }
    }

    private fun handleCollectFaceDetected() {
        CoroutineScope(Dispatchers.Default).launch(
            CoroutineExceptionHandler { _, ex ->
                Logger.w("Error in progress -> handleCollectFaceDetected ex: ${ex.message}")
                if (dialog?.isShowing == true) {
                    handleCollectFaceDetected()
                }
            }
        ) {
            while (dialog?.isShowing == true) {
//                if (cameraPreviewDataQueue.isEmpty())  delay(1000)
                val previewData = cameraPreviewDataQueue.take()
                // Convert preview camera to image file
                previewData?.also { previewData ->
                    faceRecognitionViewModel.faceDetect(previewData) { rect ->
                        CoroutineScope(Dispatchers.Default).launch {
                            if (rect != null) {
                                lastPreviewData = previewData.clone()
                                showFacePassFace(rect = rect)
                            } else {
                                withContext(Dispatchers.Main) {
                                    viewBinding.faceView.clear()
                                    viewBinding.faceView.invalidate()
                                }
                            }

                        }
                    }
                }
            }
        }
    }
    private suspend fun showFacePassFace(rect: Rect?) {

        if(rect != null){
            viewBinding.faceView.clear()

            val mirror = viewBinding.extraCamera == false
            val faceIdString = StringBuilder()
            val faceRollString = StringBuilder()
            val facePitchString = StringBuilder()
            val faceYawString = StringBuilder()
            val faceBlurString = StringBuilder()
            val smileString = StringBuilder()
            val faceRecognitionRate = StringBuilder()
            val mat = Matrix()
            val w = cameraPreviewDevice.getPreviewSize().first
            val h = cameraPreviewDevice.getPreviewSize().second
            val cameraWidth: Int = cameraPreviewDevice.getResolutionSize().first
            val cameraHeight: Int = cameraPreviewDevice.getResolutionSize().second
            var left = 0f
            var top = 0f
            var right = 0f
            var bottom = 0f
            when (cameraRotation) {
                0 -> {
                    left = rect.right.toFloat()
                    top = rect.top.toFloat()
                    right = rect.left.toFloat()
                    bottom = rect.bottom.toFloat()
                    mat.setScale(if (mirror) -1f else 1f, 1f)
                    mat.postTranslate(if (mirror) cameraWidth.toFloat() else 0f, 0f)
                    mat.postScale(
                        w.toFloat() / cameraWidth.toFloat(),
                        h.toFloat() / cameraHeight.toFloat()
                    )
                }
                90 -> {
                    mat.setScale((if (mirror) -1.0f else 1.0f), 1f)
                    mat.postTranslate(if (mirror) cameraHeight.toFloat() else 0f, 0f)
                    mat.postScale(
                        w.toFloat() / cameraHeight.toFloat(),
                        h.toFloat() / cameraWidth.toFloat()
                    )
                    left = rect.top.toFloat()
                    top = (cameraWidth - rect.right).toFloat()
                    right = rect.bottom.toFloat()
                    bottom = (cameraWidth - rect.left).toFloat()
                }
                180 -> {
                    mat.setScale(1f, 1f)
                    mat.postTranslate(0f, 0f)
                    mat.postScale(
                        w.toFloat() / cameraWidth.toFloat(),
                        h.toFloat() / cameraHeight.toFloat()
                    )
                    left = rect.right.toFloat()
                    top = rect.bottom.toFloat()
                    right = rect.left.toFloat()
                    bottom = rect.top.toFloat()
                }
                270 -> {
                    mat.setScale((if (mirror) -1.0f else 1.0f), 1f)
                    mat.postTranslate(if (mirror) cameraHeight.toFloat() else 0f, 0f)
                    mat.postScale(
                        w.toFloat() / cameraHeight.toFloat(),
                        h.toFloat() / cameraWidth.toFloat()
                    )
                    left = (cameraHeight - rect.bottom).toFloat()
                    top = rect.left.toFloat()
                    right = (cameraHeight - rect.top).toFloat()
                    bottom = rect.right.toFloat()
                }
            }
            val drect = RectF()
            val srect = RectF(left, top, right, bottom)
            mat.mapRect(drect, srect)
            viewBinding.faceView.addRect(drect)
            viewBinding.faceView.addId(faceIdString.toString())
            viewBinding.faceView.addRoll(faceRollString.toString())
            viewBinding.faceView.addPitch(facePitchString.toString())
            viewBinding.faceView.addYaw(faceYawString.toString())
            viewBinding.faceView.addBlur(faceBlurString.toString())
            viewBinding.faceView.addSmile(smileString.toString())
            viewBinding.faceView.addRate(faceRecognitionRate.toString())
            withContext(Dispatchers.Main){
                viewBinding.faceView.invalidate()
            }
        }
    }


}
