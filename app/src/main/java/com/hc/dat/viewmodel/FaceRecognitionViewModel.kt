package com.hc.dat.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import android.os.Build
import androidx.core.graphics.drawable.toBitmapOrNull
import androidx.lifecycle.ViewModel
import coil.request.ImageRequest
import com.hc.dat.di.ApplicationContext
import com.hc.dat.model.UserInfo
import com.hc.dat.model.database.entity.FaceRecognitionImageEntity
import com.hc.dat.model.database.entity.UserEntity
import com.hc.dat.model.database.entity.convertToModelEntity
import com.hc.dat.model.repository.Repository
import com.hc.dat.model.result.ErrorCode
import com.hc.dat.model.result.ResponseResult
import com.hc.dat.service.ServiceDefinition
import com.hc.dat.utils.ImageLoader
import com.hc.dat.utils.LicenseLoader.hcLicenseFolder
import com.hc.dat.utils.SingleLiveEvent
import com.hc.dat.utils.Utils
import com.lws.device.Device
import com.lws.device.camerapreview.CameraPreviewDevice
import com.lws.device.camerapreview.Nv21ImageData
import com.lws.type.LogRecorder
import com.lws.type.Logger
import com.omi.face.FaceRecognitionProcessor
import hc.manager.datapp.R
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject

data class FaceRecognitionViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: Repository,
    private val device: Device
) : ViewModel() {
    var showDialogCertificationVerifyTimeOut: Boolean = false
    private var faceRecognitionProcessor: FaceRecognitionProcessor
    val triggerFaceRecognitionEvent = SingleLiveEvent<TriggerFaceRecognitionEvent>()
    private var listCertFileId = mutableListOf<File>()
    private var listUserEntity = mutableListOf<UserEntity>()
    private var listFaceRecogImageEntity = mutableListOf<FaceRecognitionImageEntity>()


    //    flag
    var isDownloadLicense: Boolean = false

    companion object {
        const val PRESSURE_MIN = 0.01F
        const val PRESSURE_MAX = 1.99F
        const val TEMPERATURE_MIN = -20
        const val TEMPERATURE_MAX = 50
        const val TANK_MIN = 0
        const val TANK_MAX = 100

//        const val HC_FACE_PASS_GROUP_NAME = "hc-face-pass"
    }

    init {
        faceRecognitionProcessor = FaceRecognitionProcessor(activity = context)
//        activeLicense()
//        if (device.getCurrentPrinter() == null) throw NullPointerException("Please init Printer device first!")
        if (device.getCurrentCamera() == null) throw NullPointerException("Please init Camera device first!")
        refreshDataUpdated()
    }

    private fun getListFilesInDirectory() {
        val directory = hcLicenseFolder
        if (directory.exists() && directory.isDirectory) {
            val files = directory.listFiles()
            if (files != null) {
                for (file in files) {
                    if (file.isFile) {
                        listCertFileId.add(file)
                    }
                }
            }
        } else {
            Logger.e("Directory does not exist or is not a directory")
        }
    }

    private fun refreshDataUpdated() {
        CoroutineScope(Dispatchers.Default).launch(
            CoroutineExceptionHandler { _, _ ->
                LogRecorder.e("Đồng bộ dữ liệu ảnh mẫu lỗi", "")
                Logger.e("refreshDataUpdated Error in get data listUserEntity and listFaceRecogImageEntity in local")
            }
        ) {
            LogRecorder.d("Đồng bộ dữ liệu ảnh mẫu", "")
            listUserEntity.addAll(repository.getListUserLocal())
            listFaceRecogImageEntity.addAll(repository.getAllFaceRecogImage())
        }
    }

    fun getCameraPreviewDevice(): CameraPreviewDevice? = device.getCameraPreview()

    fun getListUserAssignInDevice(
        imei: String,
        callback: (action: FaceRecognitionAction, data: Any?) -> Unit
    ) {
        Logger.d("getListUserAssignInDevice imei: $imei")
        CoroutineScope(Dispatchers.Default).launch(
            CoroutineExceptionHandler { _, ex ->
                Logger.e("getListUserAssignInDevice: Found an exception exception: ${ex.message}")
                CoroutineScope(Dispatchers.Main).launch {
                    callback(
                        FaceRecognitionAction.GET_LIST_USER_ASSIGN_IN_DEVICE_FAIL,
                        "Lỗi xử lý!!\n\nXin hãy thử lại"
                    )
                }
            }
        ) {
            delay(3000)
            val resResult: ResponseResult<List<UserInfo>> = repository.getListUserAssignInDevice(imei)
            if (resResult.isError) {
                Logger.e("Error: ${resResult.errorMessage}")
                CoroutineScope(Dispatchers.Main).launch {
                    callback(
                        FaceRecognitionAction.GET_LIST_USER_ASSIGN_IN_DEVICE_FAIL,
                        resResult.errorMessage
                    )
                }
            } else {
                val listUserAssignInDevice = resResult.data
                Logger.i("listUserAssignInDevice: $listUserAssignInDevice")

                listUserAssignInDevice?.filter {
                    // only accept users hase authenImages, if it hasn't -> required add new user authentication for setup recognition images
                    it.authenImages?.isNotEmpty() == true
                }?.forEach { userInfo ->
                    listUserEntity.firstOrNull { userEntity ->
                        userEntity.userId == userInfo.id
                    }?.also { userEntity ->
                        val updateUserEntity = userInfo.convertToModelEntity()
                        // update basic user info
                        userEntity.updateUserBasicInfo(
                            updateUserEntity = updateUserEntity
                        )
                        repository.updateUserBasicInfo(
                            userId = updateUserEntity.userId,
                            userName = updateUserEntity.userName,
                            gender = updateUserEntity.gender,
                            fullName = updateUserEntity.fullName,
                            phoneNumber = updateUserEntity.phoneNumber,
                            userCode = updateUserEntity.userCode,
                            address = updateUserEntity.address,
                            avatarId = updateUserEntity.avatarId,
                            trainingCenterId = updateUserEntity.trainingCenterId,
                            birthday = updateUserEntity.birthday,
                            courseId = updateUserEntity.courseId,
                            courseCode = updateUserEntity.courseCode,
                            courseLicense = updateUserEntity.courseLicense,
                            nfcId = updateUserEntity.nfcId,
                            totalTimeStudied = updateUserEntity.totalTimeStudied,
                            totalDistanceRode = updateUserEntity.totalDistanceRode,
                            totalCourseTime = updateUserEntity.totalCourseTime,
                            totalCourseDistance = updateUserEntity.totalCourseDistance
                        )
                        // check new list image id of exist user have changed
                        userInfo.authenImages?.forEach { authenImageId ->
                            if (listFaceRecogImageEntity.firstOrNull { faceRecognitionImageEntity ->
                                faceRecognitionImageEntity.imageId == authenImageId
                            } == null
                            ) {
                                // Add new face recognition image
                                listFaceRecogImageEntity.add(
                                    FaceRecognitionImageEntity(
                                        userId = userInfo.userCode!!,
                                        imageId = authenImageId
                                    )
                                )
                            }
                        }
                    } ?: also {
                        val newUserEntity = userInfo.convertToModelEntity()
                        // add new user to list at first position
                        listUserEntity.add(0, newUserEntity)
                        // insert new user to local db
                        repository.insertNewUser(newUserEntity)
                        // insert all image recognition of new user to list image recognition
                        listFaceRecogImageEntity.addAll(
                            userInfo.authenImages?.map {
                                FaceRecognitionImageEntity(
                                    userId = userInfo.userCode!!,
                                    imageId = it
                                )
                            } ?: emptyList()
                        )
                    }
                }
                Logger.i("listUserEntity: $listUserEntity")
                Logger.i("listFaceRecogImageEntity:: new $listFaceRecogImageEntity")

                handleLoadFaceRecognitionImage(callback)
            }
        }
    }

    suspend fun addNewUser(userInfo: UserInfo) {
        if (listUserEntity.firstOrNull { userEntity ->
            userEntity.userId == userInfo.id
        } == null
        ) {
            val newUserEntity = userInfo.convertToModelEntity()
            // add new user to list at first position
            listUserEntity.add(0, newUserEntity)
            CoroutineScope(Dispatchers.Default).launch {
                // insert new user to local db
                repository.insertNewUser(newUserEntity)
            }
            // insert all image recognition of new user to list image recognition
            listFaceRecogImageEntity.addAll(
                userInfo.authenImages?.map {
                    FaceRecognitionImageEntity(
                        userId = userInfo.userCode!!,
                        imageId = it
                    )
                } ?: emptyList()
            )
        } else {
                userInfo.userCode?.let { deleteSampleFace(it) }
            // check new list image id of exist user have changed
            userInfo.authenImages?.forEach { authenImageId ->
                if (listFaceRecogImageEntity.firstOrNull { faceRecognitionImageEntity ->
                    faceRecognitionImageEntity.imageId == authenImageId
                } == null
                ) {
                    // Add new face recognition image
                    listFaceRecogImageEntity.add(
                        FaceRecognitionImageEntity(
                            userId = userInfo.userCode!!,
                            imageId = authenImageId
                        )
                    )
                }
            }
        }
        Logger.i("listUserEntity: $listUserEntity")
        Logger.i("listFaceRecogImageEntity:: new $listFaceRecogImageEntity")

        handleLoadFaceRecognitionImage(null)
    }

    private suspend fun deleteSampleFace(userId: String) {
        val listFaceRecogImageFilter = listFaceRecogImageEntity.filter { it.userId == userId }

        listFaceRecogImageFilter.forEach {
            repository.deleteFaceRecogImage(it.imageId)
        }
        faceRecognitionProcessor.deleteFaceSample(userId)
        listFaceRecogImageEntity.removeAll { it.userId == userId }
    }

    private fun handleLoadFaceRecognitionImage(callback: ((action: FaceRecognitionAction, data: Any?) -> Unit)?) {
        CoroutineScope(Dispatchers.Default).launch {
            val addFaceChannel = Channel<AddFaceAction>()
            listFaceRecogImageEntity.filter {
                !it.faceSampleAdded
            }.forEach {
                it.faceSampleAdded = true
                repository.insertNewFaceRecogImage(it)
            }
            val listFaceSample = faceRecognitionProcessor.getAllFaceSample().map {
                it.name
            }
            val listFaceSampleAdd = listFaceRecogImageEntity.filter {
                it.userId !in listFaceSample
            }
            listFaceSampleAdd.forEach { faceRecognitionImageEntity ->
                    loadImagesRecognizeQueue(
                            faceRecogImage = faceRecognitionImageEntity,
                            channel = addFaceChannel,
                    )
                    addFaceChannel.receive()
            }
            CoroutineScope(Dispatchers.Main).launch {
                callback?.let {
                    it(
                        FaceRecognitionAction.GET_LIST_USER_ASSIGN_IN_DEVICE_SUCCESS,
                        "Hoàn thành đồng bộ dữ liệu ảnh nhận dạng."
                    )
                }
            }
        }
    }

    private fun loadImagesRecognizeQueue(
        faceRecogImage: FaceRecognitionImageEntity,
        channel: Channel<AddFaceAction>,
    ) {
        Logger.i("loadImagesRecognizeQueue faceRecogImage: $faceRecogImage")
        val request = ImageRequest.Builder(context)
            .data("${ServiceDefinition.IMAGE_FULL_SIZE_URL}${faceRecogImage.imageId}")
            .setHeader("User-Agent", "Mozilla/5.0")
            .crossfade(true)
            .placeholder(R.drawable.ic_loading)
            .allowHardware(false)
            .target(
                onStart = { _ ->
                    Logger.d("loadImagesRecognizeQueue onStart: ${ServiceDefinition.IMAGE_FULL_SIZE_URL}${faceRecogImage.imageId}")
                },
                onSuccess = { result ->
                    Logger.d("loadImagesRecognizeQueue onSuccess: ${ServiceDefinition.IMAGE_FULL_SIZE_URL}${faceRecogImage.imageId}\"")
                    val bitmap: Bitmap? = result.toBitmapOrNull()
                    if (bitmap != null) {
                        CoroutineScope(Dispatchers.Default).launch {
                            faceRecognitionProcessor.addSampleFaces(
                                    id = faceRecogImage.userId,
                                    groupName = faceRecogImage.userId,
                                    bitmap = bitmap
                            ) { state, message, successCounter ->
                                // Handle the result here
                                Logger.i("Result: $state, Message: $message, Success: $successCounter")
                            }
                            channel.send(AddFaceAction.ADD_FACE_SUCCESS)
                        }
                    }
                },
                onError = { error ->
                    Logger.e("loadImagesRecognizeQueue onError studentAuthInfo?.authenImages ${ServiceDefinition.IMAGE_FULL_SIZE_URL}${faceRecogImage.imageId} Error!!")
                    CoroutineScope(Dispatchers.Default).launch {
                        channel.send(AddFaceAction.LOAD_IMAGE_FAIL)
                    }
                }
            )
            .build()
        ImageLoader.imageLoader?.enqueue(request)
    }

    private fun getFacePassGroupByUserId(userId: String): String {
        var validGroupName = userId
        // max length of group's name is 12
        if (userId.length > 12) validGroupName = userId.takeLast(12)
        return validGroupName
    }


    suspend fun facialAnalysis(frameBitmap: Bitmap, userId: String?) {
            faceRecognitionProcessor.analyze(image = frameBitmap, userId = userId)
    }

    suspend fun faceDetect(frameBitmap: Nv21ImageData,onFaceDetected: (Rect?) -> Unit){
        faceRecognitionProcessor.faceDetect(image = frameBitmap,onFaceDetected = onFaceDetected)
    }

    fun startRecognition(
        faceGroupName: String?,
        resultCallback: (
        searchScore: Int,
        faceBitmap: Bitmap?,
        rect: Rect?,
        notFace: Boolean,
        notMask: Boolean
    ) -> Unit){
        faceRecognitionProcessor.startRecognition(faceGroupName = faceGroupName, resultCallback = resultCallback)
    }
    fun stopRecognition(){
        faceRecognitionProcessor.stopRecognition()
    }

    fun cameraPreviewDataToBitmap(previewData: Nv21ImageData): Bitmap {
        val nv21Data = previewData.nv21Data
        val width = previewData.width
        val height = previewData.height
        val rotation = previewData.rotation
        val mirror = previewData.mirror

        // Convert NV21 data to YuvImage
        val yuvImage = YuvImage(nv21Data, ImageFormat.NV21, width, height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, width, height), 100, out)
        val imageBytes = out.toByteArray()
        var bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        if (rotation != 0 || mirror) {
            val matrix = Matrix()
            if (rotation != 0) {
                matrix.postRotate(rotation.toFloat())
            }
            if (mirror) {
                matrix.postScale(-1f, 1f)
            }
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        }
        return bitmap
    }

    fun uploadImageInRecognition(
        files: List<File>,
        userCode: String,
        callback: (action: FaceRecognitionAction, data: Any?) -> Unit
    ) {
        Logger.d("uploadImageInRecognition files: ${files.size}")
        CoroutineScope(Dispatchers.Default).launch(
            CoroutineExceptionHandler { _, ex ->
                Logger.e("uploadImageInRecognition: Found an exception exception: ${ex.message}")
                CoroutineScope(Dispatchers.Main).launch {
                    callback(
                        FaceRecognitionAction.UPLOAD_IMAGES_RECOGNITION_FAIL,
                        "Lỗi xử lý!!\n\nXin hãy thử lại"
                    )
                }
            }
        ) {
            val resResult: ResponseResult<Any?> = repository.uploadImageInRecognition(
                files = files,
                userCode = userCode,
                getImeiDevice(context)
            )
            if (resResult.isError) {
                Logger.e("uploadImageInRecognition Response Error: ${resResult.errorMessage}")
                CoroutineScope(Dispatchers.Main).launch {
                    callback(
                        FaceRecognitionAction.UPLOAD_IMAGES_RECOGNITION_FAIL,
                        resResult.errorMessage
                    )
                }
            } else {
                CoroutineScope(Dispatchers.Main).launch {
                    callback(FaceRecognitionAction.UPLOAD_IMAGES_RECOGNITION_SUCCESS, null)
                }
            }
        }
    }

    private val finishLicenseCallBack: (action: DownloadStatus)
    -> Unit = { action: DownloadStatus ->
        Logger.d("zipCallback action: $action ")
        when (action) {
            DownloadStatus.LICENSE_READY -> {
                getListFilesInDirectory()
//                checkFaceRecognitionCerVerify(0)
            }
        }
    }

    fun getImeiDevice(context: Context): String{
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if(repository.getSerialNumber() != null){
                repository.getSerialNumber()!!
            }else{
                Utils.readDataConfig()[0] ?: ""
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Utils.getImeiDevice(context = context)
        }else throw RuntimeException("Version Android not supported!")
    }
    private fun handleErrorFromServer(errorCode: Int) {
        when (errorCode) {
            ErrorCode.SERVER_DECLINE_AUTHENTICATE -> {
                Logger.i("handleErrorFromServer REQUEST_SERVER_HAS_PROBLEM")
                endLoginSession()
            }
        }
    }

    private fun endLoginSession() {
        Logger.d("endLoginSession")
//        triggerFaceRecognitionEvent.postValue(TriggerLogout.SERVER_DECLINE)
    }
}

enum class FaceRecognitionAction {
    UPLOAD_IMAGES_RECOGNITION_SUCCESS,
    UPLOAD_IMAGES_RECOGNITION_FAIL,
    GET_LIST_USER_ASSIGN_IN_DEVICE_SUCCESS,
    GET_LIST_USER_ASSIGN_IN_DEVICE_FAIL,
}
enum class DownloadStatus {
    LICENSE_READY,
}

enum class TriggerFaceRecognitionEvent {
    CERTIFICATION_VERIFY_FAIL,
    CERTIFICATION_VERIFY_SUCCESS,
    VERIFY_CER_NEED_INTERNET_CONNECTION,
    CREATE_FACE_PASS_GROUP_FAIL,
    CERTIFICATION_DATA_NOT_FOUND,
    DOWNLOAD_LICENSE_START,
    VERIFIED_CERTIFICATE,
    CERTIFICATION_VERIFY_TIMEOUT
}

enum class AddFaceAction {
    LOAD_IMAGE_FAIL,
    ADD_FACE_FAIL,
    CREATE_FACE_GROUP_FAIL,
    ADD_FACE_SUCCESS,
}
