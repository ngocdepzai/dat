package com.hc.dat.view.dialog

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues
import android.graphics.*
import android.hardware.usb.UsbDevice
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.SpannableString
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.graphics.drawable.toBitmapOrNull
import coil.request.ImageRequest
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.hc.dat.KeyboardManager
import com.hc.dat.model.UserInfo
import com.hc.dat.model.database.entity.UserEntity
import com.hc.dat.model.database.entity.convertToModelEntity
import com.hc.dat.service.ServiceDefinition
import com.hc.dat.utils.ImageLoader
import com.hc.dat.utils.Utils
import com.hc.dat.view.BaseDialog.dismissProgress
import com.hc.dat.view.BaseDialog.showProgressDialog
import com.hc.dat.viewmodel.AppAction
import com.hc.dat.viewmodel.ApplicationViewModel
import com.hc.dat.viewmodel.FaceRecognitionViewModel
import com.lws.device.camerapreview.*
import com.lws.type.LogRecorder
import com.lws.type.Logger
import hc.manager.datapp.R
import hc.manager.datapp.databinding.FaceRecognizeDialogBinding
import hc.manager.datapp.utils.UserTypeContant
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.concurrent.ArrayBlockingQueue

@SuppressLint("StaticFieldLeak")
internal object FaceRecognizeDialog {
    private var dialog: AlertDialog? = null
    private lateinit var viewBinding: FaceRecognizeDialogBinding

    private lateinit var cameraPreviewDevice: CameraPreviewDevice
    private lateinit var dataCallback: ((action: FaceRecognizeLoginAction, data: Any?, imageFile: File?) -> Any?)
//    private var hcImageFolder: String = "HC_DAT_IMAGES"
    private var hcImageFolder: File =
        File(Environment.getExternalStorageDirectory().toString() + "/HC_DAT_IMAGES")
    private lateinit var faceRecognitionViewModel: FaceRecognitionViewModel
    private lateinit var applicationViewModel: ApplicationViewModel

    //    private var cameraPreviewDataQueue: ArrayBlockingQueue<CameraPreviewData> = ArrayBlockingQueue(10)
    private var cameraPreviewDataQueue: ArrayBlockingQueue<Nv21ImageData> =
        ArrayBlockingQueue(3) // set capacity to small for make sure image recognized is same face image
    private var faceDetectedMessageQueue: ArrayBlockingQueue<Pair<Nv21ImageData, ByteArray>> =
        ArrayBlockingQueue(3) // set capacity to small for make sure image recognized is same face image
    private var userEntity: UserEntity? = null
    private var cameraRotation = 0
    private var isTeacherLogin = true
    private lateinit var activity: Activity
    private var isResearchInProgress = false
    private var currentSearchScore: Int = 0
    private var searchThreshold: Int = 40 // Ngưỡng chấp nhận (40 điểm)
    init {
        if (!hcImageFolder.exists()) {
            hcImageFolder.mkdirs()
        }
    }
    fun showDialog(
        activity: Activity,
        isTeacherLogin: Boolean,
        cameraRotation: Int,
        applicationViewModel: ApplicationViewModel,
        cameraPreviewDevice: CameraPreviewDevice,
        faceRecognitionViewModel: FaceRecognitionViewModel,
        callback: ((action: FaceRecognizeLoginAction, data: Any?, imageFile: File?) -> Any?)
    ) {
        Logger.d("showDialog")
        // reset old data
        userEntity = null
        isResearchInProgress = false
        faceDetectedMessageQueue.clear()

        this.activity = activity
        dataCallback = callback
        this.cameraPreviewDevice = cameraPreviewDevice
        this.isTeacherLogin = isTeacherLogin
        this.cameraRotation = cameraRotation
        this.applicationViewModel = applicationViewModel
        this.faceRecognitionViewModel = faceRecognitionViewModel
        clearData()
        val view = LayoutInflater.from(activity)
            .inflate(R.layout.face_recognize_dialog, null, false)
        viewBinding = FaceRecognizeDialogBinding.bind(view)

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
//            dataCallback?.let { callback -> callback(fileTemp) }
            releaseResource()
        }
        dialog?.setOnCancelListener {
            Logger.d("setOnCancelListener")
            releaseResource()
        }
        dialog?.show()
        initView()
    }

    private fun clearData() {
        faceDetectedMessageQueue.clear()
        cameraPreviewDataQueue.clear()
    }

    private val cameraPreviewEvent = object : CameraPreviewEvent {
        override fun onTakenPicture(
            handleStatus: CameraHandlerStatus,
            imageData: Nv21ImageData?
        ) {
//            Logger.i("onTakenPicture")
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
                    viewBinding.extraCamera = true
                    cameraPreviewDevice.startExtraCamera(
                        aspectRatioSurfaceView = viewBinding.surfaceView,
                        event = this
                    )
                }
                ExtraCameraState.CAMERA_DEVICE_DETACH -> {
                    viewBinding.extraCamera = false
                    cameraPreviewDevice.startInternalCamera(
                        internalCameraPreview = viewBinding.previewCamera,
                        windowManager = activity.windowManager,
                        event = this
                    )
                }
                else -> {
                    Logger.w("Not handle state: $state")
                }
            }
        }
    }
    private fun getImageFileFromUri(uri: Uri): File? {
        val cursor = activity.contentResolver.query(
            uri,
            arrayOf(MediaStore.Images.Media.DISPLAY_NAME, MediaStore.Images.Media.DATA),
            null,
            null,
            null
        )

        cursor?.use {
            if (it.moveToFirst()) {
                val filePathColumn = it.getColumnIndex(MediaStore.Images.Media.DATA)
                val filePath = it.getString(filePathColumn)
                return File(filePath)
            }
        }
        return null
    }
    private fun saveBitmap(image: Bitmap, fileName: String, userCode: String) {
        val mimeType = "image/png"
        var fileTemp: File? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "$fileName")
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(
                        MediaStore.MediaColumns.RELATIVE_PATH,
                        Environment.DIRECTORY_PICTURES + "/$hcImageFolder/$userCode"
                    )
                }
            }
            val resolver = activity.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

            uri?.let {
                var outputStream: OutputStream? = null
                try {
                    outputStream = resolver.openOutputStream(it)
                    image.compress(Bitmap.CompressFormat.PNG, 100, outputStream!!)
                    fileTemp = getImageFileFromUri(uri)
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    outputStream?.close()
                }
            }
        } else {
            val file = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "/$hcImageFolder/$userCode"
            )
            if (!file.exists()) {
                file.mkdirs()
            }

            val imageFile = File(file, fileName)
            var outputStream: FileOutputStream? = null
            try {
                outputStream = FileOutputStream(imageFile)
                image.compress(Bitmap.CompressFormat.PNG, 100, outputStream!!)
                outputStream!!.flush()

                // Add to gallery
                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                    put(MediaStore.Images.Media.DATA, imageFile.absolutePath)
                }

                val resolver = activity.contentResolver
                resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                outputStream?.close()
            }
            fileTemp = imageFile
        }
        dataCallback(FaceRecognizeLoginAction.FACE_LOGIN_SUCCESS, userEntity, fileTemp)

    }
    private fun handleSaveImageFile(userCode: String, imageData: Nv21ImageData) {
        CoroutineScope(Dispatchers.Default).launch(
            CoroutineExceptionHandler { _, e ->
                Logger.w("Error in progress -> handleSaveImageFile${e.message}")
                dataCallback(FaceRecognizeLoginAction.FACE_LOGIN_SUCCESS, userEntity, null)
            }
        ) {
            // Convert preview camera to image file
            // make folder save images recognition student face
            val studentFolder = File(hcImageFolder, userCode)
            if (!studentFolder.exists()) {
                studentFolder.mkdirs()
            }
            val prefix = if (isTeacherLogin) "t" else "s"
            val fileTemp = File(studentFolder, "${prefix}_face_login_${Utils.getRealTimeStamp()}.png")
            fileTemp.apply {
                if (exists()) delete()
                createNewFile()
                val fos = FileOutputStream(this)
                val yuvImage = YuvImage(
                    imageData.nv21Data,
                    ImageFormat.NV21,
                    imageData.width,
                    imageData.height,
                    null
                )
                val imageRatio: Float = 800.0F / imageData.width.toFloat()
                val quality = (100F * imageRatio).toInt().coerceIn(0, 100)
                yuvImage.compressToJpeg(Rect(0, 0, imageData.width, imageData.height), quality, fos)
                fos.flush()
                fos.close()
                Logger.d("Save image temporary success")
            }

            dataCallback(FaceRecognizeLoginAction.FACE_LOGIN_SUCCESS, userEntity, fileTemp)
        }
    }

    private fun startResearchFaceRecognition() {
        // start research face recognition
        if (!isResearchInProgress) {
            isResearchInProgress = true
            CoroutineScope(Dispatchers.Default).launch {
                delay(1000)

                // Khởi động nhận diện:
                // Nếu là Teacher login 1:N thì truyền null, nếu Student 1:1 thì truyền userEntity.userCode
                val targetUserId = if (isTeacherLogin) null else userEntity?.userCode

                faceRecognitionViewModel.startRecognition(targetUserId) { score, _, _, _, _ ->
                    currentSearchScore = score

                    val passStatus = if (score >= 40) "PASS" else "FAIL"
                    // Lưu ý: Nếu thư viện trả về thang điểm 100 thì sửa 0.4 thành 40
                    android.util.Log.d("FaceRecog", "Điểm nhận diện: $score | Trạng thái (40%): $passStatus")
                }

                handleCollectFaceDetected()
                handleRecognizeFaceDetected2()

            }
        }
    }

    private fun initView() {
        viewBinding.tvAddUserLabel.text = if (isTeacherLogin) {
            activity.getString(R.string.add_new_teacher_recognition_label)
        } else activity.getString(R.string.add_new_student_recognition_label)

        viewBinding.btAutoDetect.setOnClickListener {
//            viewBinding.btAutoDetect.isEnabled = false
            if (!isResearchInProgress) {
                viewBinding.btAutoDetect.setBackgroundColor(Color.GRAY)
                viewBinding.btAutoDetect.alpha = 0.8f
                startResearchFaceRecognition()
            }
        }

        viewBinding.btCheck.setOnClickListener {
            KeyboardManager.setKeyboardVisible(false)
            val userValue: String = viewBinding.edtUserCode.editText?.text.toString()
            Logger.i("userValue: $userValue")
            if (userValue.isNotBlank()) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        faceRecognitionViewModel.deleteSampleFace(userValue)
                        Logger.d("Đã xóa dữ liệu khuôn mặt cũ cho ID: $userValue")
                    } catch (e: Exception) {
                        Logger.e("Lỗi khi xóa khuôn mặt: ${e.message}")
                    }
                }
                showProgressDialog(activity)
                applicationViewModel.getUserInfoByUserCode(userValue, false, appCallback)
            }
        }
        viewBinding.btCancel.setOnClickListener {
            dismiss()
        }
    }

    private val appCallback: (action: AppAction, data: Any?)
    -> Unit = { action: AppAction, data: Any? ->
        Logger.d("loginCallback action: $action | data: $data")
        dismissProgress()
        when (action) {
            AppAction.GET_USER_INFO_SUCCESS -> {
                val userInfo = data as UserInfo
                userEntity = userInfo.convertToModelEntity()
                if (isTeacherLogin && userInfo.userType != UserTypeContant.TEACHER) {
                    dataCallback(FaceRecognizeLoginAction.REQUIRED_LOGIN_TEACHER, null, null)
                } else if (!isTeacherLogin && userInfo.userType != UserTypeContant.STUDENT) {
                    dataCallback(FaceRecognizeLoginAction.REQUIRED_LOGIN_STUDENT, null, null)
                } else {
                    if (userInfo.avatarId.isNullOrEmpty()) {
                        dataCallback(
                            FaceRecognizeLoginAction.FACE_IMAGE_SAMPLE_NOT_EXIST,
                            userEntity,
                            null
                        )
                    } else {
                        CoroutineScope(Dispatchers.Default).launch {
                            faceRecognitionViewModel.addNewUser(userInfo)
                        }
                        userEntity?.also {
                            initFaceRecognizeView(it)
                        }
                    }
                }
            }
            AppAction.GET_USER_INFO_FAIL -> {
                dataCallback(FaceRecognizeLoginAction.USER_INFO_NOT_EXIST, null, null)
            }
            else -> {
                Logger.w("Action $action not handle!")
            }
        }
    }

    private fun initFaceRecognizeView(userEntity: UserEntity) {
        android.util.Log.d("FaceRecog", "1. Đã vào initFaceRecognizeView cho user: ${userEntity.userCode}")
        viewBinding.userEntity = userEntity
        userEntity.avatarId?.also {
            val request = ImageRequest.Builder(activity)
                .data("${ServiceDefinition.IMAGE_FULL_SIZE_URL}$it")
                .setHeader("User-Agent", "Mozilla/5.0")
                .crossfade(true)
                .placeholder(R.drawable.ic_loading)
                .allowHardware(false)
                .target(
                    onStart = { placeholder ->
                        Logger.d("FaceRecog onStart: ${ServiceDefinition.IMAGE_FULL_SIZE_URL}$it")
                    },
                    onSuccess = { result ->
                        Logger.d("FaceRecog onSuccess: ${ServiceDefinition.IMAGE_FULL_SIZE_URL}$it")
                        val bitmap: Bitmap? = result.toBitmapOrNull()
                        if (bitmap != null) {
                            viewBinding.ivFaceSampleRecog.setImageBitmap(bitmap)
//                            viewBinding.btAutoDetect.isEnabled = false
                            viewBinding.btAutoDetect.setBackgroundColor(Color.GRAY)
                            viewBinding.btAutoDetect.alpha = 0.8f
                            startResearchFaceRecognition()
                        }
                    },
                    onError = { error ->
                        Logger.e("FaceRecog onError studentAuthInfo?.authenImages ${ServiceDefinition.IMAGE_FULL_SIZE_URL}$it Error!!")
//                        dataCallback(FaceRecognizeLoginAction.FACE_LOGIN_FAIL, null, null)
                    }
                )
                .build()
            ImageLoader.imageLoader?.enqueue(request)
        }
    }

    fun dismiss() {
        Logger.d("dismiss")
        dialog?.dismiss()
    }

    private fun releaseResource() {
//        cameraManager?.release()
        cameraPreviewDevice.stopCameraPreview()
    }

    private fun handleRecognizeFaceDetected2() {
        Logger.d("handleRecognizeFaceDetected2 - Bắt đầu vòng lặp so sánh")
        CoroutineScope(Dispatchers.IO).launch(
            CoroutineExceptionHandler { _, _ ->
                Logger.w("Error in progress -> handleRecognizeFaceDetected")
                if (dialog?.isShowing == true) {
                    handleRecognizeFaceDetected2()
                }
            }
        ) {
            while (dialog?.isShowing == true) {
                if (faceDetectedMessageQueue.isEmpty()) delay(500)
                else{
                    val faceDetectedData: Pair<Nv21ImageData, ByteArray> =
                        withContext(Dispatchers.IO) {
                            faceDetectedMessageQueue.take()
                        }

                    if (!isTeacherLogin){
                        val previewData = faceDetectedData.first
                        // 1. Chuyển frame thành Bitmap và phân tích
                        val frameBitmap = faceRecognitionViewModel.cameraPreviewDataToBitmap(previewData)
                        val targetUserId = if (isTeacherLogin) null else userEntity?.userCode
                        // Gọi AI phân tích
                        faceRecognitionViewModel.facialAnalysis(frameBitmap, targetUserId)
                        // 2. CHẶN Ở ĐÂY: Kiểm tra điểm số
                        Logger.i("Đang phân tích... Score hiện tại: $currentSearchScore")
                        if (currentSearchScore >= searchThreshold) {
                            // CHỈ KHI ĐIỂM >= 40 MỚI CHO PASS
                            Logger.i("NHẬN DIỆN THÀNH CÔNG! Score: $currentSearchScore")

                            withContext(Dispatchers.Main) {
                                faceRecognitionViewModel.stopRecognition()
                                handleSaveImageFile(userEntity?.userCode ?: "unknown", imageData = previewData)
                            }
                            break // Thoát vòng lặp khi thành công
                        } else {
                            // Nếu không phải người này hoặc điểm thấp, tiếp tục quét frame tiếp theo
                            Logger.w("Không khớp hoặc người lạ (Score: $currentSearchScore). Tiếp tục quét...")
                        }
                    } else{
                        handleSaveImageFile(userEntity!!.userCode, imageData = faceDetectedData.first)
                        LogRecorder.i("Phát hiện khuôn mặt", userEntity?.fullName)
                        break // Thoát vòng lặp khi thành công
                    }
                }
            }
        }
    }

    private fun handleCollectFaceDetected() {
        Logger.d("handleCollectFaceDetected")
        CoroutineScope(Dispatchers.Default).launch(
            CoroutineExceptionHandler { _, ex ->
                Logger.w("Error in progress -> handleCollectFaceDetected ex: ${ex.message}")
                if (dialog?.isShowing == true) {
                    handleCollectFaceDetected()
                }
            }
        ) {
            while (dialog?.isShowing == true) {
                if (cameraPreviewDataQueue.isEmpty()) delay(500)
                val previewData = cameraPreviewDataQueue.take()
                // Convert preview camera to image file
                previewData?.also { previewData ->
                    faceRecognitionViewModel.faceDetect(previewData) { rect ->
                        CoroutineScope(Dispatchers.Default).launch() {

                            showFacePassFace(rect = rect)
                            if (rect != null) {
                                faceDetectedMessageQueue.offer(
                                    Pair(
                                        previewData,
                                        previewData.nv21Data
                                    )
                                )
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
            faceRecognitionRate.append(currentSearchScore).append("/").append(searchThreshold)
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
            if(!isTeacherLogin){
                viewBinding.faceView.addRate(faceRecognitionRate.toString())
            }
            withContext(Dispatchers.Main){
                viewBinding.faceView.invalidate()
            }
        }
    }

//    override fun onPictureTaken(cameraPreviewData: CameraPreviewData?) {
//        // start check face when got user info
//        if (userInfo != null) {
// //            cameraPreviewDataQueue.offer(cameraPreviewData)
//        }
//    }
}

enum class FaceRecognizeLoginAction {
    REQUIRED_LOGIN_TEACHER,
    REQUIRED_LOGIN_STUDENT,
    FACE_LOGIN_SUCCESS,
    FACE_LOGIN_FAIL,
    CANCEL_LOGIN,
    USER_INFO_NOT_EXIST,
    FACE_IMAGE_SAMPLE_NOT_QUALITY,
    FACE_IMAGE_SAMPLE_NOT_EXIST,
}
