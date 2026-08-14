package com.hc.dat.view.dialog

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.*
import android.hardware.usb.UsbDevice
import android.os.Build
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.hc.dat.model.InProgressSession
import com.hc.dat.model.database.entity.UserEntity
import com.hc.dat.utils.Utils
import com.hc.dat.view.BaseNotification
import com.hc.dat.view.TrainingSessionScreen
import com.hc.dat.viewmodel.ApplicationViewModel
import com.hc.dat.viewmodel.FaceRecognitionViewModel
import com.hc.dat.viewmodel.RiderSessionViewModel
import com.lws.device.camerapreview.*
import com.lws.type.Logger
import hc.manager.datapp.R
import hc.manager.datapp.databinding.DatFinishSessionLoginDialogBinding
import hc.manager.datapp.utils.DateUtil
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ArrayBlockingQueue
import kotlin.math.floor
import kotlin.math.roundToInt
import java.util.Calendar

internal object FinishSessionDialog {
    private var dialog: AlertDialog? = null
    private lateinit var viewBinding: DatFinishSessionLoginDialogBinding

    private lateinit var cameraPreviewDevice: CameraPreviewDevice
    private var lastPreviewData: Nv21ImageData? = null
    private var dataCallback: ((action: ActionFinishSession, isNotSendTC: Boolean, imageFile: File?) -> Unit?)? =
        null
    private var hcImageFolder: File =
        File(Environment.getExternalStorageDirectory().toString() + "/HC_DAT_IMAGES")
    private var cameraPreviewDataQueue: ArrayBlockingQueue<Nv21ImageData> = ArrayBlockingQueue(10)
    private var cameraRotation = 0
    private lateinit var userEntity: UserEntity
    private lateinit var faceRecognitionViewModel: FaceRecognitionViewModel
    private lateinit var riderSessionViewModel: RiderSessionViewModel
    private var autoLogoutJob: Job? = null
    private const val autoLogoutTime = 1 * 60
    private var currentSearchScore: Int = 0
    private var searchThreshold: Float = 65F // Ngưỡng pass
    private var pendingFrame: Nv21ImageData? = null
    private var pendingCaptureTime = 0L
    private var temporalConfirmed = false
    private const val temporalConfirmDelay = 1000L
    @Volatile private var lastFaceInsideGuide: Boolean = false
    private var lastOutsideGuideNotifyTime: Long = 0L
    private lateinit var applicationViewModel: ApplicationViewModel

    init {
        if (!hcImageFolder.exists()) {
            hcImageFolder.mkdirs()
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    @SuppressLint("SetTextI18n")
    fun showDialog(
        activity: Activity,
        cameraRotation: Int,
        userEntity: UserEntity,
        inProgressSession: InProgressSession,
        faceRecognitionViewModel: FaceRecognitionViewModel,
        riderSessionViewModel: RiderSessionViewModel,
        applicationViewModel: ApplicationViewModel,
        sessionContinues: Boolean,
        autoLogout: Boolean,
        callback: ((action: ActionFinishSession, isNotSendTC: Boolean, imageFile: File?) -> Unit?)? = null
    ) {
        Logger.d("showDialog")
        var autoLogoutCounter = 0
        this.cameraPreviewDevice = faceRecognitionViewModel.getCameraPreviewDevice()!!
        this.cameraRotation = cameraRotation
        this.faceRecognitionViewModel = faceRecognitionViewModel
        this.riderSessionViewModel = riderSessionViewModel
        this.applicationViewModel = applicationViewModel
        this.userEntity = userEntity
        dataCallback = callback
        clearData()
        updateSearchThresholdBlock() // Cập nhật threshold từ config
        val view = LayoutInflater.from(activity)
            .inflate(R.layout.dat_finish_session_login_dialog, null, false)
        viewBinding = DatFinishSessionLoginDialogBinding.bind(view)

        currentSearchScore = 0 // Reset score
        faceRecognitionViewModel.startRecognition(userEntity.userCode, isFromDialog = false) { score, _, _, _, _ ->
            currentSearchScore = score
        }

        viewBinding.tvName.text = userEntity.fullName
        viewBinding.tvPhoneNumber.text = userEntity.phoneNumber
        viewBinding.tvUserCode.text = userEntity.userCode
        viewBinding.tvIdentifyNumber.text = userEntity.citizenId

        val totalDistance: Float = inProgressSession.totalDis / 1000f
        viewBinding.tvTotalDistance.text =
            activity.getString(R.string.total_distance_value, totalDistance)
        viewBinding.tvTotalTime.text = DateUtil.ConvertHms(inProgressSession.totalTime)
        val totalImageUploadCountsByTime: Int = 1 + Math.floor(inProgressSession.totalTime).toInt()/300
        viewBinding.tvUploadImageResult.text = "${inProgressSession.totalAuthDataUploadSuccess}/${inProgressSession.totalAuthDataUpload}($totalImageUploadCountsByTime)"
        val totalGPSUploadCountsByTime: Int = 1 + Math.floor(inProgressSession.totalTime).toInt()/10
        viewBinding.tvUploadGPSResult.text = "${inProgressSession.totalGPSUploadSuccess}/${inProgressSession.totalGPSUpload}($totalGPSUploadCountsByTime)"

        val authCountByTime: Int = (1 + floor(inProgressSession.totalTime).toInt() / 300)
        val successPercentage: Int =
            ((inProgressSession.successVerifyCounter.toDouble() / authCountByTime) * 100).roundToInt()
        viewBinding.tvPercentagePass.text = "$successPercentage%"
        val color =
            if (successPercentage >= TrainingSessionScreen.GOOD_SUCCESS_PERCENTAGE) Color.GREEN
            else if (successPercentage in TrainingSessionScreen.NORMAL_SUCCESS_PERCENTAGE until TrainingSessionScreen.GOOD_SUCCESS_PERCENTAGE) Color.YELLOW
            else if (successPercentage in TrainingSessionScreen.LOW_SUCCESS_PERCENTAGE until TrainingSessionScreen.NORMAL_SUCCESS_PERCENTAGE) activity.getColor(
                R.color.orange
            )
            else Color.RED
        viewBinding.tvPercentagePass.setTextColor(color)
        
        var warningMessage = ""
        val minuteStudy: Int = (inProgressSession.totalTime / 60).toInt()
        if (minuteStudy >= (4 * 60 - 5)) { // Tiệm cân 5'
            warningMessage += activity.getString(
                R.string.overtime_warning_message,
                DateUtil.ConvertHms(inProgressSession.totalTime)
            )
            warningMessage += "\n"
        }
        if (successPercentage < TrainingSessionScreen.GOOD_SUCCESS_PERCENTAGE) {
            warningMessage += activity.getString(
                R.string.low_percentage_warning_message,
                "$successPercentage%"
            )
        }
        if (warningMessage.isNullOrEmpty()) {
            viewBinding.vWarningView.visibility = View.GONE
        } else {
            viewBinding.tvWarningMessage.text = warningMessage
        }
        if(!sessionContinues){
            viewBinding.btContinues.visibility = View.GONE
//            faceRecognitionViewModel.stopRecognition()
        }
        viewBinding.btContinues.setOnClickListener {
            dismiss()
            dataCallback?.let { callback ->
                callback(
                    ActionFinishSession.CONTINUES_SESSION,
                    false,
                    null
                )
            }
        }
        viewBinding.btSaveInfo.setOnClickListener {
//            handleSaveImageFile()
            if (lastPreviewData == null) {
                BaseNotification.showWarning(activity.getString(R.string.student_not_in_camera))
                return@setOnClickListener
            }

            if (!lastFaceInsideGuide) {
                BaseNotification.showWarning(activity.getString(R.string.student_not_in_recognition_frame))
                return@setOnClickListener
            }

            // Kiểm tra điểm số nhận diện
            if (currentSearchScore >= searchThreshold) {
                Logger.i("Xác nhận kết thúc: Khớp khuôn mặt ($currentSearchScore điểm)")
                handleSaveImageFile()
            } else {
                Logger.w("Không thể kết thúc: Khuôn mặt không khớp ($currentSearchScore điểm)")
                BaseNotification.showWarning("Khuôn mặt không khớp, vui lòng nhìn thẳng vào camera")
            }
        }

        if (autoLogout) {
            autoLogoutJob?.cancel()
            autoLogoutJob = CoroutineScope(Dispatchers.Default).launch(
                CoroutineExceptionHandler { _, ex->
                    Logger.w("Error in auto logout: ${ex.message}")
                }
            ) {
                Logger.d("Auto Logout")
                val lastAuthImage = riderSessionViewModel.getLastSuccessRecognitionImage()
                while (autoLogoutJob?.isActive == true) {
                    ++autoLogoutCounter
                    if (autoLogoutCounter >= autoLogoutTime) {
                        withContext(Dispatchers.Main) {
                            handleSaveImageFile(lastAuthImage)
                            BaseNotification.showMessage(message = activity.getString(R.string.auto_logout_message))
                            dismiss()
                        }
                    }
                    delay(1000)
                }
            }
        }

        viewBinding.faceView.setShowGuide(true)

        CoroutineScope(Dispatchers.Default).launch {
            delay(1000)
            handleCollectFaceDetected()
        }

        dialog?.run {
            if (isShowing) dismiss()
        }

        dialog = MaterialAlertDialogBuilder(activity)
            .setCancelable(false)
            .setView(viewBinding.root).create()
        dialog?.setOnShowListener {
            Logger.d("setOnShowListener")
            dialog?.window?.setLayout(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
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
        }
        dialog?.setOnDismissListener {
            Logger.d("setOnDismissListener")
            releaseResource()
        }
        dialog?.setOnCancelListener {
            Logger.d("setOnCancelListener")
        }

        dialog?.show()
    }

    private fun clearData(){
        cameraPreviewDataQueue.clear()
        lastPreviewData = null
        currentSearchScore = 0
        pendingFrame = null
        pendingCaptureTime = 0L
        temporalConfirmed = false
        lastFaceInsideGuide = false
        lastOutsideGuideNotifyTime = 0L
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
    }

    fun dismiss() {
        Logger.d("dismiss")
        faceRecognitionViewModel.stopRecognition() // Dừng nhận diện
        autoLogoutJob?.cancel()
        dialog?.dismiss()
    }

    private fun handleSaveImageFile(lastAuthenImage: File? = null) {
        Logger.i("handleSaveImageFile lastAuthenImage: $lastAuthenImage")
        CoroutineScope(Dispatchers.Default).launch(
            CoroutineExceptionHandler { _, _ ->
                Logger.w("Error in progress -> handleSaveImageFile")
                dataCallback?.let { callback ->
                    callback(
                        ActionFinishSession.FINISH_SESSION,
                        viewBinding.cbNotSendTC.isChecked,
                        null
                    )
                }
            }
        ) {
            // Convert preview camera to image file
//            Logger.i("lastPreviewData: $lastPreviewData")

            // auto logout when lastAuthenImage != null
            if(lastAuthenImage != null){
                dataCallback?.let { callback ->
                    callback(
                        ActionFinishSession.AUTO_FINISH_SESSION,
                        true,
                        lastAuthenImage
                    )
                }
            } else {
                // Ưu tiên dùng pendingFrame nếu đã temporal confirm, fallback về lastPreviewData
                val frameToSave = if (temporalConfirmed && pendingFrame != null) pendingFrame else lastPreviewData
                frameToSave?.also { previewData ->
                    val studentFolder = File(hcImageFolder, userEntity.userCode)
                    if (!studentFolder.exists()) {
                        studentFolder.mkdirs()
                    }
                    val fileTemp = File(studentFolder, "logout_${Utils.getRealTimeStamp()}.png")
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
                    dataCallback?.let { callback ->
                        callback(
                            ActionFinishSession.FINISH_SESSION,
                            viewBinding.cbNotSendTC.isChecked,
                            fileTemp
                        )
                    }
                } ?: run {
                    dataCallback?.let { callback ->
                        callback(
                            ActionFinishSession.FINISH_SESSION,
                            viewBinding.cbNotSendTC.isChecked,
                            null
                        )

                    }
                }
            }
        }
    }

    private fun updateSearchThresholdBlock() {
        CoroutineScope(Dispatchers.IO).launch(
                CoroutineExceptionHandler { _, _ -> Logger.w("Error in updateSearchThresholdBlock!")}
        ) {
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)

            if (hour == 17 && minute in 4..6) {
                applicationViewModel.updateSearchThreshold {
                    searchThreshold = applicationViewModel.searchThreshold
                }
            } else {
                searchThreshold = applicationViewModel.searchThreshold
            }
            Logger.i("FinishSession Threshold: $searchThreshold")
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

                if (cameraPreviewDataQueue.isEmpty()) delay(1000)
                val previewData = cameraPreviewDataQueue.take()
                // Convert preview camera to image file
                previewData?.also { previewData ->
                     faceRecognitionViewModel.faceDetect(previewData){rect ->
                         CoroutineScope(Dispatchers.Default).launch(){
                             val insideGuide = showFacePassFace(rect = rect)
                             if (rect != null && insideGuide) {
                                 lastPreviewData = previewData

                                 // 2. Chuyển frame thành Bitmap và gọi AI phân tích điểm số
                                 val frameBitmap = faceRecognitionViewModel.cameraPreviewDataToBitmap(previewData)
                                 faceRecognitionViewModel.facialAnalysis(frameBitmap, userEntity.userCode)
                                 // Log để kiểm tra điểm thực tế từ Processor trả về
                                  android.util.Log.d("FinishDialog", "Score: $currentSearchScore")
                                 // Temporal confirmation: chụp giây 2, xác nhận giây 3
                                 if (currentSearchScore >= searchThreshold) {
                                     if (pendingFrame == null) {
                                         pendingFrame = previewData
                                         pendingCaptureTime = Utils.getRealTimeStamp()
                                         temporalConfirmed = false
                                         Logger.i("Logout: pass lần 1, chụp và chờ xác nhận. Score: $currentSearchScore")
                                     } else if (!temporalConfirmed && Utils.getRealTimeStamp() - pendingCaptureTime >= temporalConfirmDelay) {
                                         temporalConfirmed = true
                                         Logger.i("Logout: XÁC NHẬN THÀNH CÔNG (temporal). Score: $currentSearchScore")
                                     }
                                 } else {
                                     if (pendingFrame != null) {
                                         Logger.w("Logout: score fail (${currentSearchScore}). Reset pending.")
                                         pendingFrame = null
                                         temporalConfirmed = false
                                     }
                                 }
                             } else if (rect != null && !insideGuide) {
                                 // Face detected but outside guide → reset temporal + notify
                                 if (pendingFrame != null) {
                                     pendingFrame = null
                                     temporalConfirmed = false
                                 }
                                 val now = Utils.getRealTimeStamp()
                                 if (now - lastOutsideGuideNotifyTime >= 5000) {
                                     lastOutsideGuideNotifyTime = now
                                     withContext(Dispatchers.Main) {
                                         BaseNotification.showWarning(
                                             viewBinding.root.context.getString(R.string.student_not_in_recognition_frame)
                                         )
                                     }
                                 }
                             } else {
                                 if (pendingFrame != null) {
                                     Logger.w("Logout: không phát hiện face, reset pending.")
                                     pendingFrame = null
                                     temporalConfirmed = false
                                 }
                                 withContext(Dispatchers.Main) {
                                     viewBinding.faceView.clear()
                                     viewBinding.faceView.setShowGuide(true)
                                     viewBinding.faceView.invalidate()
                                 }
                             }
                         }
                    }
                }
                delay(500)
            }
        }
    }

    private suspend fun showFacePassFace(rect: Rect?): Boolean {

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
            faceRecognitionRate.append(FinishSessionDialog.currentSearchScore).append("/").append(FinishSessionDialog.searchThreshold.toInt())
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
            val insideGuide = viewBinding.faceView.isInsideGuide(drect)
            lastFaceInsideGuide = insideGuide
            viewBinding.faceView.setShowGuide(!insideGuide)
            withContext(Dispatchers.Main){
                viewBinding.faceView.invalidate()
            }
            return insideGuide
        }
        lastFaceInsideGuide = false
        return false
    }
}

enum class ActionFinishSession {
    CONTINUES_SESSION,
    FINISH_SESSION,
    AUTO_FINISH_SESSION,
}
