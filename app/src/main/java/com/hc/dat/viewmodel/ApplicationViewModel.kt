package com.hc.dat.viewmodel

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.nfc.NfcAdapter
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import com.hc.dat.di.ApplicationContext
import com.hc.dat.model.*
import com.hc.dat.model.database.entity.UserEntity
import com.hc.dat.model.database.entity.convertToModel
import com.hc.dat.model.repository.Repository
import com.hc.dat.model.result.ErrorCode
import com.hc.dat.model.result.ResponseResult
import com.hc.dat.service.ServiceDefinition
import com.hc.dat.service.model.DeviceConfigResponse
import com.hc.dat.service.model.GetObjectsLinkedDatResponse
import com.hc.dat.service.model.UploadDeviceInfoResponse
import com.hc.dat.utils.ImageLoader
import com.hc.dat.utils.ImageUtil
import com.hc.dat.utils.SingleLiveEvent
import com.hc.dat.utils.Utils
import com.lws.device.Device
import com.lws.device.nfc.NFCAction
import com.lws.device.nfc.NFCEvent
import com.lws.type.Logger
import hc.manager.datapp.BuildConfig
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import kotlin.math.abs

class ApplicationViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: Repository,
    private val device: Device
) : ViewModel() {

    companion object {
        val TIMEOUTS_OPTIONAL = listOf(30, 60, 90, 120)
    }

    val triggerLogout = SingleLiveEvent<TriggerLogout>()
    private val imagesFolderPath = Environment.getExternalStorageDirectory().toString() + "/HC_DAT_IMAGES"
    private val reportFolderPath = Environment.getExternalStorageDirectory().toString() + "/HC_DAT_REPORT"

    //    val configurationLV = SingleLiveEvent<Configuration>()
    var datDevice: DatDevice? = null
    var trainingCenter: TrainingCenter? = null
    var vehicleInfo: VehicleInfo? = null
    var allowOfflineStartSession = false
    var allowOfflineFinishSession = false
    var isAppVersionActive = false
    var searchThreshold = 65F
    init {
        ImageUtil.buildPicasso(context)
        ImageLoader.buildImageLoader(context)

        // delete old files
        deleteOldFilesAndFolders(File(imagesFolderPath),30)
        deleteOldFilesAndFolders(File(reportFolderPath),30)
//        checkRequestRetrieverLog()
    }

//    private val coroutineException = CoroutineExceptionHandler { handler, ex ->
//        Logger.e("ApplicationViewModel: Found an exception handler: $handler | exception: ${ex.message}")
//    }
    private fun isConnectionAvailable(): Boolean {
        return device.getCurrentNetworkConnection()?.checkConnectionAvailable() ?: false
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    fun getDeviceConfig() {
        Logger.d("getConfigDevice")
        val seri = getImeiDevice(context)
        val deviceInfo = Utils.getDeviceInfo(context)
        val imei = deviceInfo.imei1
        val SIMSerialNumber = deviceInfo.simReal
        val versionCode = BuildConfig.VERSION_CODE.toString()

        CoroutineScope(Dispatchers.Default).launch(
            CoroutineExceptionHandler { _, ex ->
                Logger.e("getDeviceConfig: Found an exception exception: ${ex.message}")
            }
        ) {
            if(isConnectionAvailable()){
                val resResult: ResponseResult<DeviceConfigResponse?> =
                    repository.getDeviceConfig(
                        seri = seri,
                        versionCode = versionCode,
                        imei = imei,
                        SIMSerialNumber = SIMSerialNumber
                    )
                val deviceConfigData = resResult.data
                if (!resResult.isError) {
                    if (deviceConfigData != null) {
                        searchThreshold = deviceConfigData.searchThreshold ?: 65F
                        allowOfflineStartSession = deviceConfigData.offlineStartSession ?: false
                        allowOfflineFinishSession = deviceConfigData.offlineFinishSession ?: false
                        isAppVersionActive = deviceConfigData.isAppVersionActive ?: false
                        repository.saveVersionAppActiveState(isAppVersionActive)
                        repository.saveOfflineStartSessionState(allowOfflineStartSession)
                        repository.saveOfflineFinishSessionState(allowOfflineFinishSession)
                        Logger.i("getDeviceConfig internet onl activeVersionApp: $isAppVersionActive")
                    }
                }
            } else {
                isAppVersionActive = repository.getVersionAppActiveState()
                allowOfflineStartSession = repository.getOfflineStartSessionState()
                allowOfflineFinishSession = repository.getOfflineFinishSessionState()
                Logger.i("getDeviceConfig internet off activeVersionApp: $isAppVersionActive")
            }
        }
    }

    fun updateSearchThreshold( callback: () -> Unit) {
        Logger.d("getsearchThreshold")
        val seri = Utils.getImeiDevice(context)
        val deviceInfo = Utils.getDeviceInfo(context)
        val imei = deviceInfo.imei1
        val SIMSerialNumber = deviceInfo.simReal
        val versionCode = BuildConfig.VERSION_CODE.toString()

        CoroutineScope(Dispatchers.Default).launch(
            CoroutineExceptionHandler { _, ex ->
                Logger.e("getsearchThreshold: Found an exception exception: ${ex.message}")
            }
        ) {
            if (isConnectionAvailable()) {
                val resResult: ResponseResult<DeviceConfigResponse?> =
                    repository.getDeviceConfig(
                        seri = seri,
                        versionCode = versionCode,
                        imei = imei,
                        SIMSerialNumber = SIMSerialNumber
                    )
                val responseData = resResult.data
                if (!resResult.isError) {
                    if (responseData != null) {
                        if (responseData.searchThreshold != null) {
                            searchThreshold = responseData.searchThreshold
                            callback()
                        }
                    }
                }
            }
        }
    }

    fun getAPIPathUploadImage( callback: ((action: AppAction, data: Any?) -> Unit?)? = null
    ) {
        Logger.d("getAPIPathUploadImage")
        val seri = getImeiDevice(context)

        CoroutineScope(Dispatchers.Default).launch(
            CoroutineExceptionHandler { _, ex ->
                callback?.let { callback ->
                    callback(
                        AppAction.GET_PATH_API_IMAGE_UPLOAD_FAIL,
                        null,
                    )
                }
                Logger.e("getAPIPathUploadImage: Found an exception exception: ${ex.message}")
            }
        ) {
            if (isConnectionAvailable()) {
                val resResult: ResponseResult<String?> =
                    repository.getAPIPathUploadImage(
                        seri = seri,
                    )
                val urlUploadImage = resResult.data
                Logger.i("getAPIPathUploadImage APIPathUploadImage: $urlUploadImage")
                if (!resResult.isError) {
                    if (urlUploadImage != null) {
                        ServiceDefinition.UPLOAD_IMAGE_AUTHEN_PROGRESS_URL = urlUploadImage
                        callback?.let { callback ->
                            callback(
                                AppAction.GET_PATH_API_IMAGE_UPLOAD_SUCCESS,
                                null,
                            )
                        }
                    } else {
                        callback?.let { callback ->
                            callback(
                                AppAction.GET_PATH_API_IMAGE_UPLOAD_FAIL,
                                null,
                            )
                        }
                    }
                } else {
                    callback?.let { callback ->
                        callback(
                            AppAction.GET_PATH_API_IMAGE_UPLOAD_FAIL,
                            resResult.errorMessage,
                        )
                    }
                }
            } else {
                callback?.let { callback ->
                    callback(
                        AppAction.GET_PATH_API_IMAGE_UPLOAD_FAIL_BY_INTERNET,
                        null,
                    )
                }

            }
        }
    }

    fun getSearchThreshold(): Float?{
        return  device.getSearchThreshold()
    }

    private fun handleErrorFromServer(errorCode: Int) {
        when (errorCode) {
            ErrorCode.SERVER_DECLINE_AUTHENTICATE -> {
                Logger.i("handleErrorFromServer REQUEST_SERVER_HAS_PROBLEM")
//                endLoginSession()
            }
        }
    }

    fun checkNFCAvailable(): Boolean {
        return NfcAdapter.getDefaultAdapter(context) != null
    }

    fun startNFCCard(nfcEvent: NFCEvent, nfcAction: NFCAction? = null, dataWriteToNFCCard: String = "") {
        if (!NfcAdapter.getDefaultAdapter(context).isEnabled) {
            nfcEvent.onNFCDataDetected(nfcAction = NFCAction.NFC_DISABLE)
        } else {
            CoroutineScope(Dispatchers.IO).launch(
                CoroutineExceptionHandler { _, ex ->
                    Logger.e("startReadNFCCard: Found an exception exception: ${ex.message}")
                }
            ) {
                device.getCurrentNFC()?.waitNFCDataDetected(
                    nfcEvent = nfcEvent,
                    nfcAction = nfcAction,
                    dataWriteToNFCCard = dataWriteToNFCCard
                )
            }
        }
    }

    fun stopNFCCard() {
        CoroutineScope(Dispatchers.IO).launch(
            CoroutineExceptionHandler { _, ex ->
                Logger.e("stopReadNFCCard: Found an exception exception: ${ex.message}")
            }
        ) {
            device.getCurrentNFC()?.waitNFCDataDetected(null)
        }
    }

    fun teacherAutoLogin(callback: (action: AppAction, data: Any?) -> Unit): Boolean {
        Logger.d("teacherAutoLogin")
        val teacherCode: String? = repository.getTeacherCode()
        Logger.d("teacherCode : $teacherCode")
        return teacherCode?.let {
            CoroutineScope(Dispatchers.Default).launch(
                CoroutineExceptionHandler { _, ex ->
                    Logger.e("teacherAutoLogin: Found an exception exception: ${ex.message}")
                }
            ) {
                repository.getTeacherCode()?.also {
                    getUserInfoByUserCode(it, true, callback)
                }
            }
            true
        } ?: false
    }

    fun studentAutoLogin(callback: (action: AppAction, data: Any?) -> Unit): Boolean {
        Logger.d("studentAutoLogin")
        val studentCode: String? = repository.getStudentCode()
        Logger.i("studentAutoLogin studentCode: $studentCode")
        return studentCode?.let {
            CoroutineScope(Dispatchers.Default).launch(
                CoroutineExceptionHandler { _, ex ->
                    Logger.e("studentAutoLogin: Found an exception exception: ${ex.message}")
                }
            ) {
                getUserInfoByUserCode(studentCode, true, callback)
            }
            true
        } ?: false
    }

//    fun removeCurrentTeacherInfo() {
//        repository.removeTeacherCode()
//    }

    fun getUserInfoByUserCode(
        code: String,
        isAutoLogin: Boolean,
        callback: (action: AppAction, data: Any?) -> Unit
    ) {
        Logger.d("getUserInfoByUserCode code: $code | isAutoLogin: $isAutoLogin")
        CoroutineScope(Dispatchers.Default).launch(
            CoroutineExceptionHandler { _, ex ->
                Logger.e("getUserInfoByUserCode: Found an exception exception: ${ex.message}")
                CoroutineScope(Dispatchers.Main).launch {
                    callback(AppAction.GET_USER_INFO_FAIL, "Lỗi xử lý!!\n\nXin hãy thử lại")
                }
            }
        ) {
            // [DAT CER]: only use for get DAT certification
            // check internet available
            if (device.getCurrentNetworkConnection()?.checkConnectionAvailable() == false) {
                val userEntity: UserEntity? = repository.getUserLocalByUserCode(userCode = code)
                userEntity?.also {
                    val userItem = userEntity.convertToModel()
                    userItem.loginType = LoginType.RFID
                    CoroutineScope(Dispatchers.Main).launch {
                        callback(AppAction.GET_USER_INFO_SUCCESS, userItem)
                    }
                } ?: also {
                    CoroutineScope(Dispatchers.Main).launch {
                        callback(
                            AppAction.GET_USER_INFO_FAIL,
                            "Mã thẻ NFC này không tồn tại trên thiết bị! Kết nối lại internet và thử lại."
                        )
                    }
                }
            } // [DAT CER]
            else {
                val resResult: ResponseResult<UserInfo?> =
                    repository.getUserInfoByUserCode(code, getImeiDevice(context))
                if (resResult.isError) {
                    Logger.e("Error: ${resResult.errorMessage}")
//                    CoroutineScope(Dispatchers.Main).launch {
//                        callback(AppAction.GET_USER_INFO_FAIL, resResult.errorMessage)
//                    }
                    val userEntity: UserEntity? = repository.getUserLocalByUserCode(userCode = code)
                    userEntity?.also {
                        val userItem = userEntity.convertToModel()
                        userItem.loginType = LoginType.RFID
                        CoroutineScope(Dispatchers.Main).launch {
                            callback(AppAction.GET_USER_INFO_SUCCESS, userItem)
                        }
                    } ?: also {
                        CoroutineScope(Dispatchers.Main).launch {
                            callback(
                                    AppAction.GET_USER_INFO_FAIL,
                                    "Mã thẻ NFC này không tồn tại trên thiết bị! Kết nối lại internet và thử lại."
                            )
                        }
                    }
                } else {
                    val userItem = resResult.data
                    // Todo hard code is LoginType.RFID
                    userItem?.loginType = LoginType.RFID
                    userItem?.also {
                        Logger.i("userItem: $userItem | isAutoLogin: $isAutoLogin")
                        CoroutineScope(Dispatchers.Main).launch {
                            callback(
                                if (isAutoLogin) AppAction.GET_USER_INFO_AUTO_SUCCESS else AppAction.GET_USER_INFO_SUCCESS,
                                userItem
                            )
                        }
                    }
                }
            }
        }
    }

    fun getUserInfoByRfidCode(cardData: String, callback: (action: AppAction, data: Any?) -> Unit) {
        Logger.d("getUserInfoByRfidCode cardData: $cardData")
        CoroutineScope(Dispatchers.Default).launch(
            CoroutineExceptionHandler { _, ex ->
                Logger.e("getUserInfoByRfidCode: Found an exception exception: ${ex.message}")
                CoroutineScope(Dispatchers.Main).launch {
                    callback(AppAction.GET_USER_INFO_FAIL, "Lỗi xử lý!!\n\nXin hãy thử lại")
                }
            }
        ) {
            // [DAT CER]: only use for get DAT certification
            // check internet available
            if (device.getCurrentNetworkConnection()?.checkConnectionAvailable() == false) {
                val userEntity: UserEntity? = repository.getUserLocalByRfidCode(cardData)
                userEntity?.also {
                    val userItem = userEntity.convertToModel()
                    userItem.loginType = LoginType.RFID
                    CoroutineScope(Dispatchers.Main).launch {
                        callback(AppAction.GET_USER_INFO_SUCCESS, userItem)
                    }
                } ?: also {
                    CoroutineScope(Dispatchers.Main).launch {
                        callback(
                            AppAction.GET_USER_INFO_FAIL,
                            "Mã thẻ NFC này không tồn tại trên thiết bị! Kết nối lại internet và thử lại."
                        )
                    }
                }
            } // [DAT CER]
            else {
                val resResult: ResponseResult<UserInfo?> =
                    repository.getUserInfoByRfidCode(cardData, getImeiDevice(context))
                if (resResult.isError) {
                    Logger.e("Error: ${resResult.errorMessage}")
                    CoroutineScope(Dispatchers.Main).launch {
                        callback(AppAction.GET_USER_INFO_FAIL, resResult.errorMessage)
                    }
                } else {
                    val userItem = resResult.data
                    userItem?.loginType = LoginType.RFID
                    userItem?.also {
                        Logger.i("userItem: $userItem")
                        CoroutineScope(Dispatchers.Main).launch {
                            callback(AppAction.GET_USER_INFO_SUCCESS, userItem)
                        }
                    }
                }
            }
        }
    }
    fun getImeiDevice(context: Context): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val serialNumber = repository.getSerialNumber()
            val imei1 = repository.getImei1()
            val imei2 = repository.getImei2()
            if (serialNumber != null && imei1 != null && imei2 != null) {
                Utils.saveDataConfigToExternalStorage(serialNumber = serialNumber, imei1 = imei1, imei2 = imei2)
                serialNumber
            } else {
                val configDevice = Utils.readDataConfig()
                val serialNumber = configDevice?.get(0) ?: ""
                val imei1 = configDevice?.get(1) ?: ""
                val imei2 = configDevice?.get(2) ?: ""
                saveDeviceConfigToSharePre(
                    serialNumber = serialNumber,
                    imei1 = imei1,
                    imei2 = imei2
                )
                serialNumber
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Utils.getImeiDevice(context = context)
        } else throw RuntimeException("Version Android not supported!")
    }
    fun saveDeviceConfigToSharePre(serialNumber: String, imei1: String, imei2: String){
        repository.saveSerialNumber(serialNumber = serialNumber)
        repository.saveImei1(imei1 = imei1)
        repository.saveImei2(imei2 = imei2)
    }

    fun getTrainingCenterName(): String? {
        return trainingCenter?.name ?: repository.getTrainingCenterName()
    }

    fun getPlateSlug(): String? {
        return vehicleInfo?.plateSlug ?: repository.getPlateSlug()
    }

//    private fun checkRequestRetrieverLog() {
//        CoroutineScope(Dispatchers.IO).launch(
//            CoroutineExceptionHandler { _, ex ->
//                Logger.e("checkRequestRetrieverLog: Found an exception exception: ${ex.message}")
//            }
//        ) {
//            if (device.getCurrentNetworkConnection()?.checkConnectionAvailable() == true) {
//                val imei = Utils.getImeiDevice(context)
//                val resResult: ResponseResult<GetRequestRetrieverLogResponse> =
//                    repository.getRequestRetrieverLog(imei = imei)
//                Logger.i("resResult: $resResult")
//                if (!resResult.isError && resResult.data != null) {
//                    val getRequestRetrieverLogResponse = resResult.data
//                    val studentCode = getRequestRetrieverLogResponse.studentCode
//                    val logDate =
//                        Utils.convertServerTimeToDate(getRequestRetrieverLogResponse.dateTimeLog)
//                    val dateString =
//                        SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(logDate)
//                    Logger.i("checkRequestRetrieverLog dateString: $dateString")
//                    var userLogFolder = File(
//                        Environment.getExternalStorageDirectory()
//                            .toString() + "/HC_DAT_LOGS/$studentCode"
//                    )
//                    val fileLog = File(userLogFolder, "${studentCode}_$dateString.log")
//                    if (fileLog.exists()) {
//                        // upload logfile
//                        repository.uploadUserLog(
//                            file = fileLog,
//                            userCode = studentCode,
//                            imei = imei
//                        )
//                    }
//                }
//            }
//        }
//    }

    fun getObjectsLinkedDat(callback: (action: AppAction, data: Any?) -> Unit) {
        Logger.d("getObjectsLinkedDat")
        // [DAT CER]: only use for get DAT certification
        // check internet available
        if (device.getCurrentNetworkConnection()?.checkConnectionAvailable() == false) {
            CoroutineScope(Dispatchers.Main).launch {
                callback(
                    AppAction.INIT_CONFIG_DATA_FAIL_BY_INTERNET,
                    "Lỗi xử lý!!\n\nXin hãy thử lại"
                )
            }
        } // [DAT CER]
        else {
            CoroutineScope(Dispatchers.Default).launch(
                CoroutineExceptionHandler { _, ex ->
                    Logger.e("getObjectsLinkedDat: Found an exception exception: ${ex.message}")
                    CoroutineScope(Dispatchers.Main).launch {
                        callback(AppAction.INIT_CONFIG_DATA_FAIL, "Lỗi xử lý!!\n\nXin hãy thử lại")
                    }
                }
            ) {
                val resResult: ResponseResult<GetObjectsLinkedDatResponse?> =
                    repository.getObjectsLinkedDat(imei = getImeiDevice(context))
                if (resResult.isError) {
                    Logger.e("Error: ${resResult.errorMessage}")
                    CoroutineScope(Dispatchers.Main).launch {
                        callback(AppAction.INIT_CONFIG_DATA_FAIL, resResult.errorMessage)
                    }
                } else {
                    val serviceDateTime: Long? =
                        Utils.convertServerTimeToMilliSecond(resResult.data?.currentDate)
                    if (serviceDateTime != null) {
                        // Check timezone device phải là Việt Nam GMT+7
                        val timezone = TimeZone.getDefault()
                        val timezoneOffsetHours = timezone.rawOffset / (1000 * 60 * 60)

                        // Có thể check theo offset hoặc timezone id
                        val isVietnamTimezone =
                                timezoneOffsetHours == 7 &&
                                        (timezone.id == "Asia/Ho_Chi_Minh" || timezone.id == "Asia/Saigon")

                        val deviceDateTime = Calendar.getInstance().timeInMillis
                        // check difference between server time and device time not more than 5 minutes
                        val checkChange: Boolean =
                            abs(deviceDateTime - serviceDateTime) <= (5 * 60 * 1000)
                        Logger.i(
                                "getObjectsLinkedDat: serviceDateTime: $serviceDateTime | " +
                                        "deviceDateTime: $deviceDateTime | " +
                                        "timezoneId: ${timezone.id} | " +
                                        "timezoneOffsetHours: $timezoneOffsetHours | " +
                                        "isVietnamTimezone: $isVietnamTimezone | " +
                                        "checkChange: $checkChange"
                        )
                        if (isVietnamTimezone && checkChange) {
                            datDevice = resResult.data?.datDevice
                            trainingCenter = resResult.data?.trainingCenter
                            vehicleInfo = resResult.data?.carVehicle
                            // save to local for offline case
                            repository.saveTrainingCenterName(trainingCenter?.name)
                            repository.savePlateSlug(vehicleInfo?.plateSlug)
                            trainingCenter?.teacherSendTc?.let {
                                repository.saveTeacherSendTc(it)
                            }
                            CoroutineScope(Dispatchers.Main).launch {
                                callback(AppAction.INIT_CONFIG_DATA_SUCCESS, null)
                            }
                        } else {
                            CoroutineScope(Dispatchers.Main).launch {
                                callback(AppAction.CHECK_DEVICE_DATE_TIME_FAIL, null)
                            }
                        }
                    } else {
                        CoroutineScope(Dispatchers.Main).launch {
                            callback(AppAction.CHECK_DEVICE_DATE_TIME_FAIL, null)
                        }
                    }
                }
            }
        }
    }

    fun isTeacherSendTc(): Boolean {
        // Ưu tiên lấy từ biến memory, nếu không có thì lấy từ SharedPreferences
        return trainingCenter?.teacherSendTc ?: repository.getTeacherSendTc()
    }

    fun uploadDeviceInfo(callback: (action: AppAction, data: Any?) -> Unit) {
        CoroutineScope(Dispatchers.Default).launch(
            CoroutineExceptionHandler { _, ex ->
                Logger.e("getDeviceInfo: Found an exception exception: ${ex.message}")
                CoroutineScope(Dispatchers.Main).launch {
                    callback(AppAction.INIT_CONFIG_DATA_FAIL, "Lỗi xử lý!!\n\nXin hãy thử lại")
                }
            }
        ) {
            val resResult: ResponseResult<UploadDeviceInfoResponse> =
                repository.uploadDeviceInfo(deviceInfo = Utils.getDeviceInfo(context))
            if (resResult.isError) {
                Logger.e("Error: ${resResult.errorMessage}")
                CoroutineScope(Dispatchers.Main).launch {
                    callback(AppAction.UPLOAD_DEVICE_INFO_FAIL, resResult.errorMessage)
                }
            } else {
                CoroutineScope(Dispatchers.Main).launch {
                    callback(AppAction.UPLOAD_DEVICE_INFO_SUCCESS, resResult.data?.message)
                }
            }
        }

    }

    fun renameFile(file: File): File {
        val originalFileName = file.name
        val safeFileName = originalFileName.replace(":", "-")
        val renamedFile = File(file.parent, safeFileName)
        file.renameTo(renamedFile)
        return renamedFile
    }
    fun pushFile(files: List<File>, callback: (action: AppAction, data: Any?) -> Unit) {
        Logger.d("pushLogFile")
        CoroutineScope(Dispatchers.IO).launch(
            CoroutineExceptionHandler { _, ex ->
                Logger.e("pushLogFile: Found an exception exception: ${ex.message}")
            }
        ) {
            if (device.getCurrentNetworkConnection()?.checkConnectionAvailable() == true) {
                val imei = getImeiDevice(context)
                val  result = repository.uploadLogs(
                    files = files,
                    imei = imei
                )
                if (result.isError) {
                    CoroutineScope(Dispatchers.Main).launch {
                        callback(AppAction.UPLOAD_FILE_FAIL, "gửi file thất bại")
                    }
                } else {
                    CoroutineScope(Dispatchers.Main).launch {
                        callback(AppAction.UPLOAD_FILE_SUCCESS, "gửi file thành công")
                    }
                }
            } else {
                CoroutineScope(Dispatchers.Main).launch {
                    callback(AppAction.UPLOAD_FILE_FAIL, "Không có kết nối internet")
                }
            }
        }
    }
    fun uriToFile(context: Context, uri: Uri): File? {
        var filePath: String? = null
        val projection = arrayOf(MediaStore.MediaColumns.DATA)
        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(uri, projection, null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
                filePath = cursor.getString(columnIndex)
            }
        } finally {
            cursor?.close()
        }
        return filePath?.let { File(it) }
    }
    fun getFileBySessionId(
        sessionId: String,
        studentCode: String,
        localSessionId: Long
    ): File? {
        val lastTimeAuth =
            repository.getListAuthenDataByLocalSessionId(localSessionId = localSessionId)
                .last().time;
        val appVersion = BuildConfig.VERSION_NAME
        val fileName = "${studentCode}_${sessionId}_$appVersion.csv"
        val dateString = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(
            lastTimeAuth
        )
        var hcReportFolder =
            File(
                Environment.getExternalStorageDirectory().toString() + "/HC_DAT_REPORT/$dateString"
            )
        val file = File(hcReportFolder, fileName)
        return if (file.exists()) file else null
    }
    private fun deleteOldFilesAndFolders(directory: File, daysOld: Int) {
        val currentTime = System.currentTimeMillis()
        val cutoffTime = currentTime - daysOld * 24 * 60 * 60 * 1000L

        if (directory.exists()) {
            val files = directory.listFiles()
            if (files != null) {
                for (file in files) {
                    if (file.isFile && file.lastModified() < cutoffTime) {
                        Logger.i("Deleting file: ${file.name}")
                        file.delete()
                    } else if (file.isDirectory) {
                        // Check if the directory is empty after deleting old files
                        if (file.listFiles()?.isEmpty() == true && file.lastModified() < cutoffTime) {
                            Logger.i("Deleting empty folder: ${file.name}")
                            file.delete()
                        }
                        // Recursively delete files in subdirectories
                        deleteOldFilesAndFolders(file, daysOld)
                    }
                }
            }
        } else {
            println("Directory does not exist: ${directory.path}")
        }
    }
}

enum class AppAction {
    GET_USER_INFO_SUCCESS,
    GET_USER_INFO_AUTO_SUCCESS,
    GET_USER_INFO_FAIL,
    INIT_CONFIG_DATA_SUCCESS,
    INIT_CONFIG_DATA_FAIL,
    INIT_CONFIG_DATA_FAIL_BY_INTERNET,
    CHECK_DEVICE_DATE_TIME_FAIL,
    UPLOAD_DEVICE_INFO_SUCCESS,
    UPLOAD_DEVICE_INFO_FAIL,
    UPLOAD_FILE_SUCCESS,
    UPLOAD_FILE_FAIL,
    GET_PATH_API_IMAGE_UPLOAD_SUCCESS,
    GET_PATH_API_IMAGE_UPLOAD_FAIL,
    GET_PATH_API_IMAGE_UPLOAD_FAIL_BY_INTERNET
}

enum class TriggerLogout {
    TIMEOUTS,
    FORCE_LOGOUT,
    SERVER_DECLINE
}
