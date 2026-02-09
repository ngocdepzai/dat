package com.hc.dat.view

import android.content.Context
import android.graphics.*
import android.hardware.usb.UsbDevice
import android.os.Bundle
import android.os.Environment
import android.text.SpannableString
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import androidx.lifecycle.ViewModelProviders
import com.hc.dat.KeyboardManager
import com.hc.dat.model.UserInfo
import com.hc.dat.model.database.entity.UserEntity
import com.hc.dat.model.database.entity.convertToModelEntity
import com.hc.dat.view.adapter.DialogButtonClickListener
import com.hc.dat.viewmodel.AppAction
import com.hc.dat.viewmodel.FaceRecognitionAction
import com.hc.dat.viewmodel.FaceRecognitionViewModel
import com.hc.dat.viewmodel.RiderSessionViewModel
import com.lws.device.camerapreview.*
import com.lws.type.LogRecorder
import com.lws.type.Logger
import hc.manager.datapp.R
import hc.manager.datapp.databinding.ScreenRegisterFaceRecognizeBinding
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ArrayBlockingQueue

class RegisterFaceRecognizeScreen : DatBaseScreen() {
    private lateinit var viewBinding: ScreenRegisterFaceRecognizeBinding
    private lateinit var faceRecognitionViewModel: FaceRecognitionViewModel
    private lateinit var riderSessionViewModel: RiderSessionViewModel

    private var hcImageFolder: File =
        File(Environment.getExternalStorageDirectory().toString() + "/HC_DAT_IMAGES")
    private lateinit var cameraPreviewDevice: CameraPreviewDevice
    private var lastImageCapture: Nv21ImageData? = null

    //    private var cameraManager: CameraManager? = null
//    private var cameraView: CameraPreview? = null
    private var listImageView: MutableList<ImageView> = mutableListOf()
    private var listGuideMessage: MutableList<String> = mutableListOf()
    private var cameraRotation = 0

    //    private var cameraPreviewDataQueue: ArrayBlockingQueue<CameraPreviewData> = ArrayBlockingQueue(10)
    private var cameraPreviewDataQueue: ArrayBlockingQueue<Nv21ImageData> = ArrayBlockingQueue(10)

    private var detectFaceJob: Job? = null
    private var addFaceRecogJob: Job? = null

    private var userEntity: UserEntity? = null
    private var countPass = 0
    private var progressCounter: Int = 0
    private var guideCounter = 0
    private var listImageDataDetected: MutableList<File> = mutableListOf()

    companion object {
        const val IMAGE_RECOGNIZE_NEED = 6
    }

    init {
        if (!hcImageFolder.exists()) {
            hcImageFolder.mkdirs()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        viewBinding = ScreenRegisterFaceRecognizeBinding.inflate(inflater, container, false)
        faceRecognitionViewModel =
            ViewModelProviders.of(
                requireActivity(),
                viewModelFactory
            )[FaceRecognitionViewModel::class.java]
        riderSessionViewModel =
            ViewModelProviders.of(
                requireActivity(),
                viewModelFactory
            )[RiderSessionViewModel::class.java]

        cameraPreviewDevice = faceRecognitionViewModel.getCameraPreviewDevice()!!
        listImageView.addAll(
            listOf(
                viewBinding.ivRecog1,
                viewBinding.ivRecog2,
                viewBinding.ivRecog3,
                viewBinding.ivRecog4,
                viewBinding.ivRecog5,
                viewBinding.ivRecog6
            )
        )
        listGuideMessage.addAll(
            listOf(
                "Ảnh 1: Mặt thẳng và song song với camera",
                "Ảnh 2: Mặt thẳng và song song với camera",
                "Ảnh 3: Nghiêng mặt về phía tay trái của bạn",
                "Ảnh 4: Nghiêng mặt về phía tay phải của bạn",
                "Ảnh 5: Hơi cúi mặt xuống dưới",
                "Ảnh 6: Hơi ngẩng mặt lên trên"
            )
        )

        userEntity = arguments?.getSerializable("user_info") as UserEntity?
        Logger.i("userEntity: $userEntity")

        initUserView()

        viewBinding.edtUserCode.requestFocus()
        viewBinding.btCheck.setOnClickListener {
            KeyboardManager.setKeyboardVisible(false)
            val userValue: String = viewBinding.edtUserCode.editText?.text.toString()
            Logger.i("userValue: $userValue")
            if (userValue.isNotBlank()) {
                showProgressDialog()
                appViewModel.getUserInfoByUserCode(userValue, false, appCallback)
            }
        }

        viewBinding.btUploadFaceRecognize.setOnClickListener {
            // handle upload image recognition
            handleUploadImageRecognition()
        }

        viewBinding.btCancel.setOnClickListener {
            requireActivity().onBackPressed()
        }

        viewBinding.btReset.setOnClickListener {
            progressCounter = 0
            guideCounter = 0
            countPass = 0
            listImageDataDetected.clear()
            viewBinding.tvProgress.text = "$progressCounter%"
            viewBinding.progressBar.progress = progressCounter
            listImageView.forEach {
                it.setImageDrawable(requireContext().getDrawable(R.drawable.ic_face_recog))
            }
            handleCollectFaceDetected()
        }

        viewBinding.btCapture.setOnClickListener {
            lastImageCapture?.also {
                CoroutineScope(Dispatchers.IO).launch(
                    CoroutineExceptionHandler { _, _ ->
                        Logger.w("Error in progress -> handleManualCaptureImage")
                    }
                ) {
                    convertBitmapAndSave(it)
                }
            }
        }
        if(cameraPreviewDevice.getExtraCameraState() == ExtraCameraState.CAMERA_OPEN) {
            viewBinding.btRotateCamera.visibility = View.GONE
        }
        viewBinding.btRotateCamera.setOnClickListener{
            cameraPreviewDevice.rotateCamera(
                windowManager = requireActivity().windowManager,
                event = cameraPreviewEvent)
        }

        return viewBinding.root
    }

    private val cameraPreviewEvent = object : CameraPreviewEvent {
        override fun onTakenPicture(
            handleStatus: CameraHandlerStatus,
            imageData: Nv21ImageData?
        ) {
//                    Logger.i("onTakenPicture imageData: $imageData")
            lastImageCapture = imageData
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

    private fun initUserView() {
        if (userEntity != null) {
            viewBinding.userInfo = userEntity
            viewBinding.tvName.text = userEntity?.fullName ?: "-/-"
            viewBinding.tvUserCode.text = userEntity?.userCode ?: "-/-"
            viewBinding.tvPhoneNumber.text = userEntity?.phoneNumber ?: "-/-"
            viewBinding.tvIdentifyNumber.text = userEntity?.citizenId ?: "-/-"
            progressCounter =
                (listImageDataDetected.size.toFloat() / IMAGE_RECOGNIZE_NEED.toFloat() * 100f).toInt()
            viewBinding.tvProgress.text = "$progressCounter %"

            BaseNotification.showMessage(
                "Khuôn mặt học viên phải vừa trong khung hình",
                showToast = false
            )
            handleCollectFaceDetected()
        } else {
            viewBinding.userInfo = null
        }
    }

    private val appCallback: (action: AppAction, data: Any?)
    -> Unit = { action: AppAction, data: Any? ->
        Logger.d("loginCallback action: $action | data: $data")
        dismissProgress()
        when (action) {
            AppAction.GET_USER_INFO_SUCCESS -> {
                this.userEntity = (data as UserInfo).convertToModelEntity()
                initUserView()
            }
            AppAction.GET_USER_INFO_FAIL -> {
                showDialog(
                    title = getString(R.string.title_notification),
                    message = getString(R.string.rfr_get_user_info_fail),
                    buttonList = listOf(getString(R.string.ok)),
                    listener = object : DialogButtonClickListener {
                        override fun onDialogButtonClick(position: Int) {
                            dismissDialog()
                        }
                    }
                )
            }
            else -> {
                Logger.w("Action $action not handle!")
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (riderSessionViewModel.inProgressSession != null) {
            showDialog(
                title = getString(R.string.title_notification),
                message = getString(R.string.rfr_can_register_when_session_doing),
                buttonList = listOf(getString(R.string.back_bt)),
                cancelable = false,
                listener = object : DialogButtonClickListener {
                    override fun onDialogButtonClick(position: Int) {
                        dismissDialog()
                        requireActivity().onBackPressed()
                    }
                }
            )
        }
    }

    private fun handleUploadImageRecognition() {
        if (listImageDataDetected.size < 2) {
            showDialog(
                title = getString(R.string.title_notification),
                message = getString(
                    R.string.rfr_missing_image_recognize,
                    IMAGE_RECOGNIZE_NEED.toString()
                ),
                buttonList = listOf(getString(R.string.rfr_add_more_bt)),
                listener = object : DialogButtonClickListener {
                    override fun onDialogButtonClick(position: Int) {
                        dismissDialog()
                    }
                }
            )
        } else if (listImageDataDetected.size < IMAGE_RECOGNIZE_NEED) {
            showDialog(
                title = getString(R.string.title_notification),
                message = getString(
                    R.string.rfr_missing_image_recognize,
                    IMAGE_RECOGNIZE_NEED.toString()
                ),
                buttonList = listOf(
                    getString(R.string.rfr_upload_bt),
                    getString(R.string.rfr_add_more_bt)
                ),
                listener = object : DialogButtonClickListener {
                    override fun onDialogButtonClick(position: Int) {
                        dismissDialog()
                        if (position == 0) {
                            showProgressDialog()
                            faceRecognitionViewModel.uploadImageInRecognition(
                                listImageDataDetected,
                                userCode = userEntity!!.userCode,
                                callback = faceRecognitionCallback
                            )
                        }
                    }
                }
            )
        } else {
            showProgressDialog()
            faceRecognitionViewModel.uploadImageInRecognition(
                listImageDataDetected,
                userCode = userEntity!!.userCode,
                callback = faceRecognitionCallback
            )
        }
    }

    private fun handleAddImageRecognize(nv21ImageData: Nv21ImageData) {
        CoroutineScope(Dispatchers.IO).launch(
            CoroutineExceptionHandler { _, _ ->
                Logger.w("Error in progress -> handleSaveAndUploadImage")
            }
        ) {
            val fileTemp = File(hcImageFolder, "face_reg_img_${listImageDataDetected.size}.png")
            fileTemp.apply {
                if (exists()) delete()
                createNewFile()
                val fos = FileOutputStream(this)
                val yuvImage = YuvImage(
                    nv21ImageData.nv21Data,
                    ImageFormat.NV21,
                    nv21ImageData.width,
                    nv21ImageData.height,
                    null
                )
                yuvImage.compressToJpeg(
                    Rect(0, 0, nv21ImageData.width, nv21ImageData.height),
                    100,
                    fos
                )
                fos.flush()
                fos.close()
                Logger.d("Save image temporary success")
            }
            listImageDataDetected.add(fileTemp)
        }
    }
    private val faceRecognitionCallback: (action: FaceRecognitionAction, data: Any?)
    -> Unit = { action: FaceRecognitionAction, data: Any? ->
        Logger.d("faceRecognitionCallback action: $action | data: $data")
        dismissProgress()
        when (action) {
            FaceRecognitionAction.UPLOAD_IMAGES_RECOGNITION_SUCCESS -> {
                showDialog(
                    title = getString(R.string.title_notification),
                    message = getString(R.string.rfr_finish_update_face_recoginition),
                    buttonList = listOf(getString(R.string.ok)),
                    listener = object : DialogButtonClickListener {
                        override fun onDialogButtonClick(position: Int) {
                            dismissDialog()
                            requireActivity().onBackPressed()
                        }
                    }
                )
            }
            FaceRecognitionAction.UPLOAD_IMAGES_RECOGNITION_FAIL -> {
                showDialog(
                    title = getString(R.string.title_notification),
                    message = getString(R.string.rfr_upload_images_recognition_fail),
                    buttonList = listOf(getString(R.string.retry_button)),
                    listener = object : DialogButtonClickListener {
                        override fun onDialogButtonClick(position: Int) {
                            dismissDialog()
                        }
                    }
                )
            }
            else -> {
                Logger.w("Action $action not handle!")
            }
        }
    }

    private suspend fun handleCheckAddFaceCondition(roll: Float, pitch: Float): Boolean {
//        Logger.d("handleCheckAddFaceCondition guideCounter: $guideCounter | listImageDataDetected.size: ${listImageDataDetected.size}")
//        Logger.d("handleCheckAddFaceCondition roll: $roll | pitch: $pitch")
        if (guideCounter == listImageDataDetected.size) {
            withContext(Dispatchers.Main) {
                BaseNotification.showMessage(listGuideMessage[guideCounter], showToast = false)
                viewBinding.tvGuide.text = listGuideMessage[guideCounter]
                guideCounter++
                // clear for get new image face
                cameraPreviewDataQueue.clear()
            }
        }
        return when (listImageDataDetected.size) {
            // ảnh thẳng mặt
            0 -> {
                roll in -2.0..2.0 && pitch in -1.0..3.0
            }
            // ảnh thẳng mặt
            1 -> {
                roll in -2.0..2.0 && pitch in -1.0..3.0
            }
            // ảnh nghiêng trái
            2 -> {
                roll in 2.0..7.0 && pitch in -4.0..4.0
            }
            // ảnh nghiêng phải
            3 -> {
                roll in -12.0..-2.0 && pitch in -4.0..4.0
            }
            // ảnh cúi mặt
            4 -> {
                pitch in 5.0..12.0 && roll in -2.0..2.0
            }
            // ảnh ngẩng mặt
            5 -> {
                pitch in -12.0..-5.0 && roll in -2.0..2.0
            }
            else -> false
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val windowRotation =
            (requireActivity().getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.rotation * 90
//        cameraRotation = if (windowRotation == 0) {
//            FacePassImageRotation.DEG90
//        } else if (windowRotation == 90) {
//            FacePassImageRotation.DEG0
//        } else if (windowRotation == 270) {
//            FacePassImageRotation.DEG180
//        } else {
//            FacePassImageRotation.DEG270
//        }
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
                windowManager = requireActivity().windowManager,
                event = cameraPreviewEvent
            )
        }
        CoroutineScope(Dispatchers.Default).launch {
            delay(1000)
            handleCollectFaceDetected()
        }
    }

    override fun onDestroy() {
//        cameraManager?.release()

        cameraPreviewDevice.stopCameraPreview()
        addFaceRecogJob?.cancel()
        detectFaceJob?.cancel()
        super.onDestroy()
    }

    private fun handleCollectFaceDetected() {
        if (userEntity != null) {
            detectFaceJob?.cancel()
            detectFaceJob = CoroutineScope(Dispatchers.Default).launch(
                CoroutineExceptionHandler { _, _ ->
                    Logger.w("Error in progress -> handleCaptureImageVerifyStudentFace")
                    handleCollectFaceDetected()
                }
            ) {
                while (isActive) {
                    if (cameraPreviewDataQueue.isEmpty()) delay(1000)
                    val previewData = cameraPreviewDataQueue.take()
                    // Convert preview camera to image file
                    previewData?.also { previewData ->
                        faceRecognitionViewModel.faceDetect(previewData) { rect ->

                            CoroutineScope(Dispatchers.Default).launch {
                                showFacePassFace(rect = rect)

                                if (rect != null) {
//                                result.faceList.firstOrNull {
//                                    handleCheckAddFaceCondition(it.pose.roll, it.pose.pitch)
//                                }?.also {
//                                    convertBitmapAndSave(previewData)
//                                    // delay 2s for wait convert image and clear queue for get new image capture
//                                    cameraPreviewDataQueue.clear()
//                                    delay(2000)
//                                }
                                    withContext(Dispatchers.Main) {
//                                    Logger.i("guideCounter: $guideCounter | listImageDataDetected.size: ${listImageDataDetected.size}")
                                        if (guideCounter == listImageDataDetected.size && guideCounter < listGuideMessage.size) {
                                            withContext(Dispatchers.Main) {
                                                BaseNotification.showMessage(
                                                    listGuideMessage[guideCounter],
                                                    showToast = false
                                                )
                                                LogRecorder.i(
                                                    "Thông báo: ",
                                                    listGuideMessage[guideCounter]
                                                )
                                                viewBinding.tvGuide.text =
                                                    listGuideMessage[guideCounter]
                                                guideCounter++
                                                // clear for get new image face
                                                cameraPreviewDataQueue.clear()
                                            }
                                        }
                                    }

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
    }

    private suspend fun convertBitmapAndSave(nv21ImageData: Nv21ImageData) {
        CoroutineScope(Dispatchers.Default).launch(
            CoroutineExceptionHandler { _, _ ->
                Logger.w("Error in progress -> convertBitmapAndSave")
                handleCollectFaceDetected()
            }
        ) {
            handleAddImageRecognize(nv21ImageData)
            val bitmap: Bitmap? =
                convertToBitmap(nv21ImageData.nv21Data, nv21ImageData.width, nv21ImageData.height)
            Logger.i("bitmap: $bitmap")
            if (bitmap != null) {
                withContext(Dispatchers.Main) {
                    listImageView[countPass].setImageBitmap(bitmap)
                    Logger.i("listImageDataDetected.siz: ${listImageDataDetected.size}")
                    progressCounter =
                        (listImageDataDetected.size.toFloat() / IMAGE_RECOGNIZE_NEED.toFloat() * 100f).toInt()
                    Logger.i("progressCounter: $progressCounter")
                    viewBinding.progressBar.progress = progressCounter
                    viewBinding.tvProgress.text = "$progressCounter%"
                    if (progressCounter == 100) {
                        BaseNotification.showMessage("Hoàn thành cài đặt nhận dạng.")
                        LogRecorder.i("Thông báo: ","Hoàn thành cài đặt nhận dạng.")
                        detectFaceJob?.cancel()
                    }
                }
                ++countPass
//                faceDetectedBitmapQueue.offer(bitmap)
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

    private fun convertToBitmap(nv21bytearray: ByteArray, width: Int, height: Int): Bitmap? {
        return try {
            val yuvImage = YuvImage(nv21bytearray, ImageFormat.NV21, width, height, null)
            val os = ByteArrayOutputStream()
            yuvImage.compressToJpeg(Rect(0, 0, width, height), 100, os)
            val jpegByteArray: ByteArray = os.toByteArray()
            val bitmap = BitmapFactory.decodeByteArray(jpegByteArray, 0, jpegByteArray.size)
            val fos = FileOutputStream("${Environment.getExternalStorageDirectory()}/imagename.png")
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
            fos.close()
            bitmap
        } catch (ex: Exception) {
            Logger.e("Convert Raw data to image error: ${ex.message}")
            null
        }
    }

//    override fun onPictureTaken(cameraPreviewData: CameraPreviewData?) {
// //        Logger.d("onPictureTaken")
// //        if (previewData == null) {
// //            previewData = cameraPreviewData
// //            handleGetImageFromPreview()
// //        }
// //        previewData = cameraPreviewData
//        cameraPreviewDataQueue.offer(cameraPreviewData)
//    }
}
