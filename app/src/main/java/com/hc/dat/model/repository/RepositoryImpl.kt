package com.hc.dat.model.repository

import android.content.SharedPreferences
import android.os.Build
import com.hc.dat.model.CarInfo
import com.hc.dat.model.UserInfo
import com.hc.dat.model.database.AppDatabase
import com.hc.dat.model.database.entity.*
import com.hc.dat.model.result.ErrorCode.CAN_NOT_CONNECT_TO_SERVER
import com.hc.dat.model.result.ErrorCode.SUCCESS_WITH_ERROR
import com.hc.dat.model.result.ErrorCode.PUSH_SESSION_TO_TC_FAIL
import com.hc.dat.model.result.ResponseResult
import com.hc.dat.service.ServiceDefinition
import com.hc.dat.service.api.DatService
import com.hc.dat.service.api.generateBodyRequest
import com.hc.dat.service.model.*
import com.hc.dat.utils.ExportExcelReport
import com.hc.dat.utils.SentryLogUploader
import com.lws.type.LogRecorder
import com.lws.type.Logger
import com.omi.service.ResponseStatus
import com.omi.service.Service
import hc.manager.datapp.utils.UpdateUserType
import kotlinx.coroutines.channels.Channel
import okhttp3.MultipartBody
import java.io.File
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RepositoryImpl @Inject constructor(
    private val appDatabase: AppDatabase,
    private val sharedPreferences: SharedPreferences,
    private val datService: DatService
) : Repository {

    //    private var currentAuthenInfo: AuthenticInfo? = null
    private var isServiceOnline: Boolean = true

    companion object {
        const val APP_VERSION_ACTIVE_STATE = "APP_VERSION_ACTIVE_STATE"
        const val ALLOW_OFFLINE_START_SESSION  = "ALLOW_OFFLINE_START_SESSION"
        const val ALLOW_OFFLINE_FINISH_SESSION = "ALLOW_OFFLINE_FINISH_SESSION"
        const val LOG_CREATED_DATE = "LOG_CREATED_DATE"
        const val STATUS_DOWNLOAD_LICENSE = "STATUS_DOWNLOAD_LICENSE"
        const val TEACHER_CODE = "TEACHER_CODE"
        const val STUDENT_CODE = "STUDENT_CODE"
        const val SESSION_ID = "SESSION_ID"
        const val TRAINING_CENTER_NAME = "TRAINING_CENTER_NAME"
        const val PLATE_SLUG = "PLATE_SLUG"
        const val AUTO_LOGOUT_TIME = "AUTO_LOGOUT_TIME"
        const val SERIAL_NUMBER = "SERIAL_NUMBER"
        const val IMEI1 = "IMEI1"
        const val IMEI2 = "IMEI2"
    }

    // [DAT CER]: only use for get DAT certification
    override fun exportSessionReport(riderSessionEntity: RiderSessionEntity, channel: Channel<Any>?) {
        val listAuthenData: List<StudentAuthenticationEntity> =
            appDatabase.studentAuthenticationEntityDao()
                .getListAuthenDataByLocalSessionId(localSessionId = riderSessionEntity.id)
        val listGPSSignalData: List<GPSSignalEntity> =
            appDatabase.gpsSignalEntityDao()
                .getListGPSSignalByLocalSessionId(localSessionId = riderSessionEntity.id)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ExportExcelReport.exportSessionData(
                    riderSessionEntity = riderSessionEntity,
                    listAuthenData = listAuthenData,
                    listGPSSignalData = listGPSSignalData,
                    channel = channel
                )
        }
    }
    // [DAT CER]

    override suspend fun startRiderSession(
        seri: String,
        studentCode: String,
        gpsLat: Double,
        gpsLong: Double,
        loginType: Int,
        loginTime: Double,
        loginImageUrl: String,
        teacherCode: String,
        appVersion: String,
        imei: String,
        simSerialNumber: String,
        networkStatus: Byte?
    ): ResponseResult<StartRiderSessionResponse?> {
        Logger.d(
            "startRiderSession imei: $imei | studentCode: $studentCode" +
                " | gpsLat: $gpsLat" +
                " | gpsLong: $gpsLong" +
                " | loginType: $loginType" +
                " | loginTime: $loginTime" +
                " | loginImageUrl: $loginImageUrl" +
                " | teacherCode: $teacherCode " +
                " | appVersion: $appVersion"
        )
        try {
            val startRiderSessionRequest = StartRiderSessionRequest(
                seri,
                studentCode,
                gpsLat,
                gpsLong,
                loginType,
                loginTime,
                loginImageUrl,
                teacherCode,
                appVersion,
                imei,
                simSerialNumber,
                networkStatus
            )
            Logger.i("startRiderSession startRiderSessionRequest: $startRiderSessionRequest")
            val response = datService.startRiderSession(startRiderSessionRequest)
            val responseData = response.body()
            Logger.i("startRiderSession responseData: $responseData")
            return when (response.code()) {
                ResponseStatus.SUCCESS -> {
                    if (responseData?.status == 1 && responseData.sessionId.isNotEmpty()) {
                        SentryLogUploader.captureInfo(
                                tag = "SESSION_START_SUCCESS",
                                message = "Student $studentCode started session successfully",
                                extras = mapOf(
                                        "seri" to seri,
                                        "studentCode" to studentCode,
                                        "gpsLat" to gpsLat,
                                        "gpsLong" to gpsLong,
                                        "loginType" to loginType,
                                        "loginTime" to loginTime,
                                        "loginImageUrl" to loginImageUrl,
                                        "teacherCode" to teacherCode,
                                        "appVersion" to appVersion,
                                        "imei" to imei,
                                        "simSerialNumber" to simSerialNumber,
                                        "networkStatus" to (networkStatus ?: -1),
                                        "sessionId" to (responseData.sessionId ?: ""),
                                        "responseStatus" to responseData.status,
                                        "responseMessage" to (responseData.message ?: "")
                                )
                        )
                        ResponseResult(
                            data = responseData
                        )
                    } else {
                        SentryLogUploader.captureInfo(
                                tag = "SESSION_START_FAIL",
                                message = "Start session failed: ${responseData?.message ?: "Unknown"}",
                                extras = mapOf(
                                        "seri" to seri,
                                        "studentCode" to studentCode,
                                        "gpsLat" to gpsLat,
                                        "gpsLong" to gpsLong,
                                        "loginType" to loginType,
                                        "loginTime" to loginTime,
                                        "loginImageUrl" to loginImageUrl,
                                        "teacherCode" to teacherCode,
                                        "appVersion" to appVersion,
                                        "imei" to imei,
                                        "simSerialNumber" to simSerialNumber,
                                        "networkStatus" to (networkStatus ?: -1),
                                        "responseStatus" to (responseData?.status ?: -1),
                                        "responseMessage" to (responseData?.message ?: "Unknown"),
                                        "httpCode" to response.code()
                                )
                        )
                        ResponseResult(
                            isError = true,
                            errorCode = responseData?.status ?: -1,
                            errorMessage = responseData?.message ?: "Unknown!"
                        )
                    }
                }
                else -> {
                    SentryLogUploader.captureInfo(
                            tag = "SESSION_START_FAIL",
                            message = "Start session failed: ${responseData?.message ?: "Unknown"}",
                            extras = mapOf(
                                    "seri" to seri,
                                    "studentCode" to studentCode,
                                    "gpsLat" to gpsLat,
                                    "gpsLong" to gpsLong,
                                    "loginType" to loginType,
                                    "loginTime" to loginTime,
                                    "loginImageUrl" to loginImageUrl,
                                    "teacherCode" to teacherCode,
                                    "appVersion" to appVersion,
                                    "imei" to imei,
                                    "simSerialNumber" to simSerialNumber,
                                    "networkStatus" to (networkStatus ?: -1),
                                    "responseStatus" to (responseData?.status ?: -1),
                                    "responseMessage" to (responseData?.message ?: "Unknown"),
                                    "httpCode" to response.code()
                            )
                    )
                    ResponseResult(
                        isError = true,
                        errorCode = responseData?.status ?: -1
                    )
                }
            }
        } catch (ex: UnknownHostException) {
            Logger.i("Error ${ex.message}")
            SentryLogUploader.captureException(
                    ex,
                    "SESSION_START_EXCEPTION",
                    mapOf(
                            "seri" to seri.toString(),
                            "studentCode" to studentCode.toString(),
                            "gpsLat" to gpsLat.toString(),
                            "gpsLong" to gpsLong.toString(),
                            "loginType" to loginType.toString(),
                            "loginTime" to loginTime.toString(),
                            "loginImageUrl" to loginImageUrl.toString(),
                            "teacherCode" to teacherCode.toString(),
                            "appVersion" to appVersion.toString(),
                            "imei" to imei.toString(),
                            "simSerialNumber" to simSerialNumber.toString(),
                            "networkStatus" to (networkStatus?.toString() ?: ""),
                            "exceptionMessage" to (ex.message ?: ""),
                            "exceptionType" to ex.javaClass.simpleName
                    )
            )
            return ResponseResult(
                isError = true
            )
        }
    }

    override suspend fun finishRiderSession(
        imei: String,
        studentCode: String,
        gpsLat: Double,
        gpsLong: Double,
        sessionId: String,
        logoutTime: Double,
        logoutImageUrl: String,
        isSendTC: Boolean,
        coverReSend: Boolean
    ): ResponseResult<Any?> {
        Logger.d(
            "finishRiderSession imei: $imei | studentCode: $studentCode" +
                " | gpsLat: $gpsLat" +
                " | gpsLong: $gpsLong" +
                " | sessionId: $sessionId" +
                " | logoutTime: $logoutTime" +
                " | logoutImageUrl: $logoutImageUrl" +
                " | coverReSend: $coverReSend" +
                " | isSendTC: $isSendTC"
        )
        try {
            val response = datService.finishRiderSession(
                FinishRiderSessionRequest(
                    imei,
                    studentCode,
                    gpsLat,
                    gpsLong,
                    sessionId,
                    logoutTime,
                    logoutImageUrl,
                    isSendTC,
                    coverReSend
                )
            )
            val responseData = response.body()
            Logger.i("finishRiderSession responseData: $responseData")
            return when (response.code()) {
                ResponseStatus.SUCCESS -> {
                    when (responseData?.status) {
                        0 -> {
                            SentryLogUploader.captureInfo(
                                    tag = "SESSION_FINISH_FAIL",
                                    message = "Finish session failed: ${responseData?.message ?: "Unknown"}",
                                    extras = mapOf(
                                            "imei" to imei,
                                            "studentCode" to studentCode,
                                            "gpsLat" to gpsLat.toString(),
                                            "gpsLong" to gpsLong.toString(),
                                            "sessionId" to sessionId,
                                            "logoutTime" to logoutTime.toString(),
                                            "logoutImageUrl" to logoutImageUrl,
                                            "isSendTC" to isSendTC.toString(),
                                            "coverReSend" to coverReSend.toString(),
                                            "responseStatus" to (responseData.status ?: -1).toString(),
                                            "responseMessage" to (responseData.message ?: ""),
                                            "httpCode" to response.code().toString()
                                    )
                            )
                            ResponseResult(
                                errorCode = SUCCESS_WITH_ERROR,
                                errorMessage =  responseData?.message ?: ""
                            )
                        }
                        1 -> {
                            SentryLogUploader.captureInfo(
                                    tag = "SESSION_FINISH_SUCCESS",
                                    message = "Student $studentCode finished session successfully",
                                    extras = mapOf(
                                            "imei" to imei,
                                            "studentCode" to studentCode,
                                            "gpsLat" to gpsLat.toString(),
                                            "gpsLong" to gpsLong.toString(),
                                            "sessionId" to sessionId,
                                            "logoutTime" to logoutTime.toString(),
                                            "logoutImageUrl" to logoutImageUrl,
                                            "isSendTC" to isSendTC.toString(),
                                            "coverReSend" to coverReSend.toString(),
                                            "responseStatus" to (responseData.status ?: -1).toString(),
                                            "responseMessage" to (responseData.message ?: ""),
                                            "httpCode" to response.code().toString()
                                    )
                            )
                            // success with empty data response
                            ResponseResult()
                        }
                        2 -> {
                            SentryLogUploader.captureInfo(
                                    tag = "SESSION_FINISH_FAIL",
                                    message = "Push session to TC failed: ${responseData?.message ?: "Unknown"}",
                                    extras = mapOf(
                                            "imei" to imei,
                                            "studentCode" to studentCode,
                                            "gpsLat" to gpsLat.toString(),
                                            "gpsLong" to gpsLong.toString(),
                                            "sessionId" to sessionId,
                                            "logoutTime" to logoutTime.toString(),
                                            "logoutImageUrl" to logoutImageUrl,
                                            "isSendTC" to isSendTC.toString(),
                                            "coverReSend" to coverReSend.toString(),
                                            "responseStatus" to (responseData.status ?: -1).toString(),
                                            "responseMessage" to (responseData.message ?: ""),
                                            "httpCode" to response.code().toString()
                                    )
                            )
                            ResponseResult(
                                isError = true,
                                errorCode = PUSH_SESSION_TO_TC_FAIL,
                                errorMessage = "Đẩy phiên lên Tổng Cục thất bại!"
                            )
                        }
//                        2,
//                        1 -> {
//                            // success with empty data response
//                            ResponseResult()
//                        }
                        else -> {
                            SentryLogUploader.captureInfo(
                                    tag = "SESSION_FINISH_FAIL",
                                    message = "Finish session failed: ${responseData?.message ?: "Unknown"}",
                                    extras = mapOf(
                                            "imei" to imei,
                                            "studentCode" to studentCode,
                                            "gpsLat" to gpsLat.toString(),
                                            "gpsLong" to gpsLong.toString(),
                                            "sessionId" to sessionId,
                                            "logoutTime" to logoutTime.toString(),
                                            "logoutImageUrl" to logoutImageUrl,
                                            "isSendTC" to isSendTC.toString(),
                                            "coverReSend" to coverReSend.toString(),
                                            "responseStatus" to (responseData?.status ?: -1).toString(),
                                            "responseMessage" to (responseData?.message ?: ""),
                                            "httpCode" to response.code().toString()
                                    )
                            )
                            ResponseResult(
                                isError = true,
                                errorCode = responseData?.status ?: -1,
                                errorMessage = responseData?.message ?: ""
                            )
                        }
                    }
                }
                else -> {
                    SentryLogUploader.captureInfo(
                            tag = "SESSION_FINISH_FAIL",
                            message = "Finish session failed: ${responseData?.message ?: "Unknown"}",
                            extras = mapOf(
                                    "imei" to imei,
                                    "studentCode" to studentCode,
                                    "gpsLat" to gpsLat.toString(),
                                    "gpsLong" to gpsLong.toString(),
                                    "sessionId" to sessionId,
                                    "logoutTime" to logoutTime.toString(),
                                    "logoutImageUrl" to logoutImageUrl,
                                    "isSendTC" to isSendTC.toString(),
                                    "coverReSend" to coverReSend.toString(),
                                    "responseStatus" to (responseData?.status ?: -1).toString(),
                                    "responseMessage" to (responseData?.message ?: ""),
                                    "httpCode" to response.code().toString()
                            )
                    )
                    ResponseResult(
                        isError = true,
                        errorCode = responseData?.status ?: -1,
                        errorMessage = responseData?.message ?: ""
                    )
                }
            }
        } catch (ex: UnknownHostException) {
            Logger.i("Error ${ex.message}")
            SentryLogUploader.captureException(
                    ex,
                    "SESSION_FINISH_EXCEPTION",
                    mapOf(
                            "imei" to imei,
                            "studentCode" to studentCode,
                            "gpsLat" to gpsLat.toString(),
                            "gpsLong" to gpsLong.toString(),
                            "sessionId" to sessionId,
                            "logoutTime" to logoutTime.toString(),
                            "logoutImageUrl" to logoutImageUrl,
                            "isSendTC" to isSendTC.toString(),
                            "coverReSend" to coverReSend.toString(),
                            "exceptionMessage" to (ex.message ?: ""),
                            "exceptionType" to ex.javaClass.simpleName
                    )
            )
            return ResponseResult(
                isError = true
            )
        }
    }

    override suspend fun uploadImageInRecognition(
        files: List<File>,
        userCode: String,
        imei: String
    ): ResponseResult<Any?> {
        Logger.d("uploadImageInRecognition userCode: $userCode | imei: $imei | files: ${files.size}")
        try {
            val files: List<MultipartBody.Part>? = files.generateBodyRequest()
            if (files != null && files.isNotEmpty()) {
                val response = datService.uploadImageInRecognition(
                    files = files,
                    userCode = userCode.generateBodyRequest(),
                    imei = imei.generateBodyRequest()
                )
                val responseData = response.body()
                Logger.i("uploadImageInRecognition: $responseData")
                return when (response.code()) {
                    ResponseStatus.SUCCESS -> {
                        if (responseData?.status == 1) {
                            ResponseResult()
                        } else {
                            ResponseResult(
                                isError = true,
                                errorCode = responseData?.status ?: -1,
                                errorMessage = responseData?.message ?: ""
                            )
                        }
                    }
                    else -> {
                        ResponseResult(
                            isError = true
                        )
                    }
                }
            } else {
                return ResponseResult(
                    isError = true
                )
            }
        } catch (ex: UnknownHostException) {
            Logger.i("Error ${ex.message}")
            return ResponseResult(
                isError = true
            )
        }
    }

    override suspend fun getRequestRetrieverLog2(imei: String): ResponseResult<GetRequestRetrieverLog2Response> {
        Logger.d("getRequestRetrieverLog2 imei: $imei")
        try {
            val response = datService.getRequestRetrieverLog2(imei = imei)
            val responseData = response.body()
            Logger.i("getRequestRetrieverLog2: $responseData")
            return when (response.code()) {
                ResponseStatus.SUCCESS -> {
                    if (responseData?.status == 1) {
                        ResponseResult(
                            data = responseData
                        )
                    } else {
                        ResponseResult(
                            isError = true,
                            errorCode = responseData?.status ?: -1,
                            errorMessage = responseData?.message ?: "Unknown!"
                        )
                    }
                }
                else -> {
                    ResponseResult(
                        isError = true,
                        errorCode = responseData?.status ?: -1,
                        errorMessage = responseData?.message ?: ""
                    )
                }
            }
        } catch (ex: UnknownHostException) {
            Logger.i("Error ${ex.message}")
            return ResponseResult(
                isError = true
            )
        }
    }

    override suspend fun uploadLogs(files: List<File>, imei: String): ResponseResult<Any?> {
        Logger.d("=== uploadLogs START ===")
        Logger.d("IMEI: $imei")
        Logger.d("Input files size: ${files.size}")

        try {
            // 🔍 Log từng file
            files.forEachIndexed { index, file ->
                Logger.d("File[$index]: path=${file.absolutePath}")
                Logger.d("File[$index]: exists=${file.exists()} | size=${file.length()} | canRead=${file.canRead()}")
            }

            val parts = files.generateBodyRequest()
            Logger.d("Generated multipart parts: ${parts?.size}")

            if (parts == null || parts.isEmpty()) {
                Logger.e("❌ Multipart parts NULL or EMPTY")
                return ResponseResult(isError = true)
            }

            // 🔍 Log từng part
            parts.forEachIndexed { index, part ->
                Logger.d("Part[$index]: headers=${part.headers}")
            }

            val imeiPart = imei.generateBodyRequest()
            Logger.d("IMEI part: $imeiPart")
            Logger.d("🚀 CALL API uploadLogs...")

            val response = datService.uploadLogs(
                    files = parts,
                    imei = imeiPart
            )

            Logger.d("⬅️ Response received")
            Logger.d("HTTP Code: ${response.code()}")
            Logger.d("Raw response: ${response.raw()}")

            val responseData = response.body()
            Logger.d("Parsed body: $responseData")

            return when (response.code()) {
                ResponseStatus.SUCCESS -> {
                    if (responseData?.status == 1) {
                        Logger.d("✅ Upload SUCCESS")

                        SentryLogUploader.captureLogFileResult(
                                success = true,
                                file = files.firstOrNull(),
                                tag = "UPLOAD_LOGS",
                                imei = imei,
                                sessionId = sharedPreferences.getString(SESSION_ID, ""),
                                studentCode = sharedPreferences.getString(STUDENT_CODE, ""),
                                message = "Upload logs success"
                        )

                        ResponseResult()
                    } else {
                        Logger.e("❌ Server trả lỗi logic: ${responseData?.message}")

                        SentryLogUploader.captureLogFileResult(
                                success = false,
                                file = files.firstOrNull(),
                                tag = "UPLOAD_LOGS",
                                imei = imei,
                                sessionId = sharedPreferences.getString(SESSION_ID, ""),
                                studentCode = sharedPreferences.getString(STUDENT_CODE, ""),
                                message = "Server Error: ${responseData?.message ?: "Unknown"}"
                        )

                        ResponseResult(
                                isError = true,
                                errorCode = responseData?.status ?: -1,
                                errorMessage = responseData?.message ?: ""
                        )
                    }
                }

                else -> {
                    Logger.e("❌ HTTP ERROR: ${response.code()}")
                    Logger.e("Error body: ${response.errorBody()?.string()}")
                    ResponseResult(isError = true)
                }
            }

        } catch (ex: UnknownHostException) {
            Logger.e("❌ UnknownHostException (NO INTERNET): ${ex.message}")
            SentryLogUploader.captureException(
                    throwable = ex,
                    tag = "UPLOAD_LOGS_EXCEPTION",
                    extras = mapOf("imei" to imei),
                    file = files.firstOrNull()
            )
            return ResponseResult(isError = true)
        } catch (ex: Exception) {
            Logger.e("❌ Exception: ${ex.message}")
            ex.printStackTrace()
            SentryLogUploader.captureException(
                    throwable = ex,
                    tag = "UPLOAD_LOGS_EXCEPTION",
                    extras = mapOf("imei" to imei),
                    file = files.firstOrNull()
            )
            return ResponseResult(isError = true)
        }
    }

    override suspend fun uploadDeviceInfo(
        deviceInfo: UploadDeviceInfoRequest
    ): ResponseResult<UploadDeviceInfoResponse> {
        Logger.d("uploadDeviceInfo  seri: ${deviceInfo.seri}, imei1: ${deviceInfo.imei1},imei2: ${deviceInfo.imei2},SIM_SERIAL_NUMBER: ${deviceInfo.simReal},")
        try {
            val response = datService.uploadDeviceInfo(
                deviceInfo
            )
            val responseData = response.body()
            return when (response.code()) {
                ResponseStatus.SUCCESS -> {
                    if (responseData?.status == 1) {
                        ResponseResult(data = responseData)
                    } else {
                        ResponseResult(
                            isError = true,
                            errorMessage = responseData?.message ?: ""
                        )
                    }
                }
                else -> {
                    ResponseResult(
                        isError = true
                    )
                }
            }
        } catch (ex: UnknownHostException) {
            Logger.e("Error ${ex.message}")
            return ResponseResult(
                isError = true
            )
        }
    }

    override suspend fun uploadImageStartSession(
        file: File,
        userCode: String,
        imei: String,
        searchScore: Float?
    ): ResponseResult<String?> {
        Logger.d("uploadImageStartSession userCode: $userCode | imei: $imei | searchScore: $searchScore | file $file")
        try {
            val file: MultipartBody.Part? = file.generateBodyRequest()
            if (file != null) {
                val response = datService.uploadImageInAuthenProgress(
                    file = file,
                    userCode = userCode.generateBodyRequest(),
                    imei = imei.generateBodyRequest(),
                    confidence = searchScore.generateBodyRequest()
                )
                val responseData = response.body()
                Logger.i("uploadImageStartSession: $responseData")
                return when (response.code()) {
                    ResponseStatus.SUCCESS -> {
                        if (responseData?.status == 1) {
                            LogRecorder.i("Tải ảnh thành công", responseData.filePath)
                            ResponseResult(
                                data = responseData.filePath
                            )
                        } else {
                            LogRecorder.e("Tải ảnh thất bại", responseData?.message)
                            ResponseResult(
                                isError = true,
                                errorCode = responseData?.status ?: -1,
                                errorMessage = responseData?.message ?: ""
                            )
                        }
                    }
                    else -> {
                        ResponseResult(
                            isError = true
                        )
                    }
                }
            } else {
                return ResponseResult(
                    isError = true
                )
            }
        } catch (ex: UnknownHostException) {
            Logger.i("Error ${ex.message}")
            LogRecorder.e("Tải ảnh thất bại", ex.message)
            return ResponseResult(
                isError = true
            )
        }
    }

    override suspend fun getAPIPathUploadImage(
        seri: String,
    ): ResponseResult<String?> {
        Logger.d("getAPIPathUploadImage seri: $seri")
        try {
            val response = datService.getAPIPathUploadImage(
                seri = seri,
            )
            val responseData = response.body()
            Logger.i("getAPIPathUploadImage responseData: $responseData")
            return when (response.code()) {
                ResponseStatus.SUCCESS -> {
                    if (responseData?.status == 1) {
                        LogRecorder.i("lấy url thành công", responseData.urlUploadImage)
                        ResponseResult(
                            data = responseData.urlUploadImage
                        )
                    } else {
                        LogRecorder.e("lấy url thất bại", responseData?.message)
                        ResponseResult(
                            isError = true,
                            errorCode = responseData?.status ?: -1,
                            errorMessage = responseData?.message ?: ""
                        )
                    }
                }

                else -> {
                    ResponseResult(
                        isError = true,
                        errorMessage = responseData?.message ?: ""
                    )
                }
            }

        } catch (ex: UnknownHostException) {
            Logger.i("Error ${ex.message}")
            LogRecorder.e("lấy url thất bại", ex.message)
            return ResponseResult(
                isError = true
            )
        }
    }


    override suspend fun getInProgressSessionByUser(userCode: String): ResponseResult<StudentSessionInProgressResponse?> {
        Logger.d("getInProgressSessionByUser userCode: $userCode")
        try {
            val response = datService.getInProgressSessionByStudent(
                GetInProgressSessionByStudentRequest(userCode = userCode)
            )
            val responseData = response.body()
            Logger.i("getInProgressSessionByUser: $responseData")
            return when (response.code()) {
                ResponseStatus.SUCCESS -> {
//                    if (responseData?.status == 1 && responseData.inProgressSession != null) {
//                        ResponseResult(
//                            data = responseData.inProgressSession
//                        )
//                    } else {
//                        ResponseResult(
//                            isError = true,
//                            errorCode = responseData?.status ?: -1,
//                            errorMessage = responseData?.message ?: ""
//                        )
//                    }
                    ResponseResult(
                        data = responseData
                    )
                }
                else -> {
                    ResponseResult(
                        isError = true,
                        errorCode = responseData?.status ?: -1,
                        errorMessage = responseData?.message ?: ""
                    )
                }
            }
        } catch (ex: UnknownHostException) {
            Logger.i("Error ${ex.message}")
            return ResponseResult(
                isError = true
            )
        }
    }

    override suspend fun fetchCurrentSession(sessionId: String): ResponseResult<FetchCurrentSessionResponse?> {
        Logger.d("fetchCurrentSession sessionId: $sessionId")
        try {
            val response =
                datService.fetchCurrentSession(GetCurrentSessionInfoRequest(sessionId = sessionId))
            val responseData = response.body()
            Logger.i("fetchCurrentSession: $responseData")
//            Logger.i("response.code: ${response.code()}")
            return when (response.code()) {
                ResponseStatus.SUCCESS -> {
                    when (responseData?.status) {
                        1 -> {
                            ResponseResult(
                                data = responseData
                            )
                        }
                        0 -> {
                            ResponseResult(
                                isError = true,
                                errorCode = responseData.status,
                                errorMessage = responseData.message
                            )
                        }
                        else -> {
                            ResponseResult(
                                isError = true,
                                errorCode = responseData?.status ?: -1,
                                errorMessage = responseData?.message ?: ""
                            )
                        }
                    }
                }
                else -> {
                    ResponseResult(
                        isError = true,
                        errorCode = responseData?.status ?: -1,
                        errorMessage = responseData?.message ?: ""
                    )
                }
            }
        } catch (ex: UnknownHostException) {
            Logger.i("Error ${ex.message}")
            return ResponseResult(
                isError = true,
                errorCode = CAN_NOT_CONNECT_TO_SERVER
            )
        }
    }

    override suspend fun checkStudentAvailable(
        imei: String,
        studentCode: String
    ): ResponseResult<Boolean> {
        Logger.d("checkStudentAvailable imei: $imei | studentCode: $studentCode")
        try {
            val response = datService.checkStudentAvailable(
                CheckStudentAvailableRequest(
                    imei = imei,
                    studentCode = studentCode
                )
            )
            val responseData = response.body()
            Logger.i("checkStudentAvailable: $responseData")
            Logger.i("response.code: ${response.code()}")
            return when (response.code()) {
                ResponseStatus.SUCCESS -> {
                    if (responseData?.status == 1) {
                        ResponseResult(
                            data = responseData.canLogin,
                            errorMessage = responseData.message
                        )
                    } else {
                        ResponseResult(
                            isError = true,
                            errorCode = responseData?.status ?: -1,
                            errorMessage = responseData?.message ?: ""
                        )
                    }
                }
                else -> {
                    ResponseResult(
                        isError = true,
                        errorCode = responseData?.status ?: -1,
                        errorMessage = responseData?.message ?: ""
                    )
                }
            }
        } catch (ex: UnknownHostException) {
            Logger.i("Error ${ex.message}")
            return ResponseResult(
                isError = true
            )
        }
    }

    override suspend fun checkMissingDataSession(
        sessionId: String,
    ): ResponseResult<CheckMissingDataSessionResponse> {
        Logger.d("checkMissingDataSession sessionId: $sessionId")
        try {
            val response = datService.checkMissingDataSession(
                CheckMissingDataSessionRequest(sessionId = sessionId)
            )
            val responseData = response.body()
            Logger.i("missing data period: $responseData")
            Logger.i("response.code: ${response.code()}")
            return when (response.code()) {
                ResponseStatus.SUCCESS -> {
                    if (responseData?.status == 1) {
                        ResponseResult(
                            data = responseData,
                            errorMessage = responseData.message
                        )
                    } else {
                        ResponseResult(
                            isError = true,
                            errorCode = responseData?.status ?: -1,
                            errorMessage = responseData?.message ?: ""
                        )
                    }
                }

                else -> {
                    ResponseResult(
                        isError = true,
                        errorCode = responseData?.status ?: -1,
                        errorMessage = responseData?.message ?: ""
                    )
                }
            }
        } catch (ex: UnknownHostException) {
            Logger.i("Error ${ex.message}")
            return ResponseResult(
                isError = true
            )
        }
    }


//    override suspend fun getCurrentSessionInfo(sessionId: String): ResponseResult<GetCurrentSessionInfoResponse?> {
//        Logger.d("getCurrentSessionInfo sessionId: $sessionId")
//        try {
//            val response = datService.getCurrentSessionInfo(GetCurrentSessionInfoRequest(sessionId = sessionId))
//            val responseData = response.body()
//            Logger.i("getCurrentSessionInfo: $responseData")
//            Logger.i("response.code: ${response.code()}")
//            return when (response.code()) {
//                ResponseStatus.SUCCESS -> {
//                    if (responseData?.status == 1) {
//                        ResponseResult(
//                            data = responseData
//                        )
//                    } else {
//                        ResponseResult(
//                            isError = true,
//                            errorCode = responseData?.status ?: -1,
//                            errorMessage = responseData?.message ?: ""
//                        )
//                    }
//                }
//                else -> {
//                    ResponseResult(
//                        isError = true,
//                        errorCode = responseData?.status ?: -1,
//                        errorMessage = responseData?.message ?: ""
//                    )
//                }
//            }
//        } catch (ex: UnknownHostException) {
//            Logger.i("Error ${ex.message}")
//            return ResponseResult(
//                isError = true,
//            )
//        }
//    }

    override suspend fun getObjectsLinkedDat(imei: String): ResponseResult<GetObjectsLinkedDatResponse?> {
        Logger.d("getObjectsLinkedDat imei: $imei")
        try {
            val response = datService.getObjectsLinkedDat(GetObjectsLinkedDatRequest(imei = imei))
            val responseData = response.body()
            Logger.i("getObjectsLinkedDat: $responseData")
            Logger.i("response.code: ${response.code()}")
            return when (response.code()) {
                ResponseStatus.SUCCESS -> {
                    if (responseData?.status == 1) {
                        ResponseResult(
                            data = responseData
                        )
                    } else {
                        ResponseResult(
                            isError = true,
                            errorCode = responseData?.status ?: -1,
                            errorMessage = responseData?.message ?: ""
                        )
                    }
                }
                else -> {
                    ResponseResult(
                        isError = true,
                        errorCode = responseData?.status ?: -1,
                        errorMessage = responseData?.message ?: ""
                    )
                }
            }
        } catch (ex: UnknownHostException) {
            Logger.i("Error ${ex.message}")
            return ResponseResult(
                isError = true
            )
        }
    }

    override suspend fun getDeviceConfig(
        seri: String,
        versionCode: String,
        imei: String,
        SIMSerialNumber: String,
    ): ResponseResult<DeviceConfigResponse?> {
        try {
            val response = datService.getDeviceConfig(
                seri = seri,
                versionCode = versionCode,
                imei =imei,
                SIMSerialNumber = SIMSerialNumber
            )
            val responseData = response.body()
            Logger.i("config device: $responseData")
            return when (response.code()) {
                ResponseStatus.SUCCESS -> {
                    if (responseData?.status == 1) {
                        ResponseResult(
                            data = responseData
                        )
                    } else {
                        ResponseResult(
                            isError = true,
                        )
                    }
                }
                else -> {
                    ResponseResult(
                        isError = true,
                    )
                }
            }
        } catch (ex: UnknownHostException) {
            Logger.i("Error ${ex.message}")
            return ResponseResult(
                isError = true
            )
        }

    }

    override suspend fun getUserInfoByRfidCode(
        cardData: String,
        imei: String
    ): ResponseResult<UserInfo?> {
        Logger.d("loginByNFC cardData: $cardData | imei: $imei")
        try {
            val response = datService.getUserInfo(
                GetUserInfoRequest(
                    idCard = cardData,
                    seri = imei,
                    codeOrIdNo = null,
                    seriCard = null
                )
            )
            val responseData = response.body()
            Logger.i("loginByNFC: $responseData")
            return when (response.code()) {
                ResponseStatus.SUCCESS -> {
                    if (responseData?.status == 1) {
                        ResponseResult(
                            data = responseData.user
                        )
                    } else {
                        ResponseResult(
                            isError = true,
                            errorCode = responseData?.status ?: -1,
                            errorMessage = responseData?.message ?: ""
                        )
                    }
                }
                else -> {
                    ResponseResult(
                        isError = true,
                        errorCode = responseData?.status ?: -1,
                        errorMessage = responseData?.message ?: ""
                    )
                }
            }
        } catch (ex: UnknownHostException) {
            Logger.i("Error ${ex.message}")
            return ResponseResult(
                isError = true
            )
        }
    }

    override suspend fun getUserInfoByUserCode(
        code: String,
        imei: String
    ): ResponseResult<UserInfo?> {
        Logger.d("loginByCode code: $code | imei: $imei")
        try {
            val response = datService.getUserInfo(
                GetUserInfoRequest(
                    idCard = null,
                    seri = imei,
                    codeOrIdNo = code,
                    seriCard = null
                )
            )
            val responseData = response.body()
            Logger.i("loginByCode: $responseData")
            return when (response.code()) {
                ResponseStatus.SUCCESS -> {
                    if (responseData?.status == 1) {
                        ResponseResult(
                            data = responseData.user
                        )
                    } else {
                        ResponseResult(
                            isError = true,
                            errorCode = responseData?.status ?: -1,
                            errorMessage = responseData?.message ?: "Unknown!"
                        )
                    }
                }
                else -> {
                    ResponseResult(
                        isError = true,
                        errorCode = responseData?.status ?: -1,
                        errorMessage = responseData?.message ?: ""
                    )
                }
            }
        } catch (ex: UnknownHostException) {
            Logger.i("Error ${ex.message}")
            return ResponseResult(
                isError = true
            )
        }
    }

    override suspend fun getListUserAssignInDevice(imei: String): ResponseResult<List<UserInfo>> {
        Logger.d("getListUserAssignInDevice imei: $imei")
        try {
            val response = datService.getListUserAssignInDevice(
                GetListUserAssignInDeviceRequest(
                    seri = imei,
                    type = UpdateUserType.UPDATE_AVATAR
                )
            )
            val responseData = response.body()
            Logger.i("getListUserAssignInDevice: $responseData")
            return when (response.code()) {
                ResponseStatus.SUCCESS -> {
                    if (responseData?.status == 1) {
                        ResponseResult(
                            data = responseData.users
                        )
                    } else {
                        ResponseResult(
                            isError = true,
                            errorCode = responseData?.status ?: -1,
                            errorMessage = responseData?.message ?: "Unknown!"
                        )
                    }
                }
                else -> {
                    ResponseResult(
                        isError = true,
                        errorCode = responseData?.status ?: -1,
                        errorMessage = responseData?.message ?: ""
                    )
                }
            }
        } catch (ex: UnknownHostException) {
            Logger.i("Error ${ex.message}")
            return ResponseResult(
                isError = true
            )
        }
    }

    override suspend fun getCarsByImeiAndCourse(
        imei: String,
        idCourse: String
    ): ResponseResult<List<CarInfo>> {
        Logger.d("getVehiclesByImeiAndCourse imei: $imei | idCourse: $idCourse")
        try {
            val response = datService.getCarsByImeiAndCourse(imei = imei, idCourse = idCourse)
            val responseData = response.body()
            Logger.i("getCarsByImeiAndCourse: $responseData")
            return when (response.code()) {
                ResponseStatus.SUCCESS -> {
                    ResponseResult(
                        data = responseData?.cars
                    )
                }
                else -> {
                    ResponseResult(
                        isError = true,
                        errorCode = responseData?.status ?: -1,
                        errorMessage = responseData?.message ?: ""
                    )
                }
            }
        } catch (ex: UnknownHostException) {
            Logger.i("Error ${ex.message}")
            return ResponseResult(
                isError = true
            )
        }
    }

    override suspend fun getCarsByImeiAndTrainingCenter(
        imei: String,
        idTrainingCenter: String
    ): ResponseResult<List<CarInfo>> {
        Logger.d("getCarsByImeiAndTrainingCenter imei: $imei | idTrainingCenter: $idTrainingCenter")
        try {
            val response = datService.getCarsByImeiAndTrainingCenter(
                imei = imei,
                idTrainingCenter = idTrainingCenter
            )
            val responseData = response.body()
            Logger.i("getCarsByImeiAndTrainingCenter response: $response")
            return when (response.code()) {
                ResponseStatus.SUCCESS -> {
                    ResponseResult(
                        data = responseData?.cars
                    )
                }
                else -> {
                    ResponseResult(
                        isError = true,
                        errorCode = responseData?.status ?: -1,
                        errorMessage = responseData?.message ?: ""
                    )
                }
            }
        } catch (ex: UnknownHostException) {
            Logger.i("Error ${ex.message}")
            return ResponseResult(
                isError = true
            )
        }
    }

    override suspend fun getRequestRetrieverLog(imei: String): ResponseResult<GetRequestRetrieverLogResponse> {
        Logger.d("getRequestRetrieverLog imei: $imei")
        try {
            val secondService: DatService = Service.buildService(
                DatService::class.java,
                ServiceDefinition.HOST_BASE_URL2,
                ServiceDefinition.CONNECTING_TIMEOUTS_DEFAULT
            )
            val response = secondService.getRequestRetrieverLog(imei = imei)
            val responseData = response.body()
            Logger.i("getRequestRetrieverLog: $responseData")
            return when (response.code()) {
                ResponseStatus.SUCCESS -> {
                    if (responseData?.status == 1) {
                        ResponseResult(
                            data = responseData
                        )
                    } else {
                        ResponseResult(
                            isError = true,
                            errorCode = responseData?.status ?: -1,
                            errorMessage = responseData?.message ?: "Unknown!"
                        )
                    }
                }
                else -> {
                    ResponseResult(
                        isError = true,
                        errorCode = responseData?.status ?: -1,
                        errorMessage = responseData?.message ?: ""
                    )
                }
            }
        } catch (ex: UnknownHostException) {
            Logger.i("Error ${ex.message}")
            return ResponseResult(
                isError = true
            )
        }
    }

    override suspend fun uploadUserLog(
        file: File,
        userCode: String,
        imei: String
    ): ResponseResult<Any?> {
        Logger.d("uploadUserLog imei:  $userCode | imei: $imei")
        try {
            val file: MultipartBody.Part? = file.generateBodyRequest()
            if (file != null) {
                // TODO create new service because API haven't just merge to main service
                val secondService: DatService = Service.buildService(
                    DatService::class.java,
                    ServiceDefinition.HOST_BASE_URL2,
                    ServiceDefinition.CONNECTING_TIMEOUTS_DEFAULT
                )
                val response = secondService.uploadUserLog(
                    file = file,
                    userCode = userCode.generateBodyRequest(),
                    imei = imei.generateBodyRequest()
                )
                val responseData = response.body()
                Logger.i("uploadUserLog: $responseData")
                return when (response.code()) {
                    ResponseStatus.SUCCESS -> {
                        if (responseData?.status == 1) {
                            ResponseResult()
                        } else {
                            ResponseResult(
                                isError = true,
                                errorCode = responseData?.status ?: -1,
                                errorMessage = responseData?.message ?: ""
                            )
                        }
                    }
                    else -> {
                        ResponseResult(
                            isError = true
                        )
                    }
                }
            } else {
                return ResponseResult(
                    isError = true
                )
            }
        } catch (ex: UnknownHostException) {
            Logger.i("Error ${ex.message}")
            return ResponseResult(
                isError = true
            )
        }
    }

    override suspend fun getListSessionHistory(
        sessionHistoryRequest: SessionHistoryRequest
    ): ResponseResult<SessionHistoryResponse?> {
        try {
            val response = datService.getListSessionHistory(
                sessionHistoryRequest
            )
            val responseData = response.body()
            Logger.i("session history: $responseData")
            return when (response.code()) {
                ResponseStatus.SUCCESS -> {
                    if (responseData?.status == 1) {
                        ResponseResult(
                            data = responseData
                        )
                    } else {
                        ResponseResult(
                            isError = true,
                            errorMessage = responseData?.message ?: ""
                        )
                    }
                }
                else -> {
                    ResponseResult(
                        isError = true,
                    )
                }
            }
        } catch (ex: UnknownHostException) {
            Logger.i("Error ${ex.message}")
            return ResponseResult(
                isError = true
            )
        }

    }
    override fun saveAutoLogoutTime(autoLogoutTime: Int) {
        sharedPreferences.edit().putInt(AUTO_LOGOUT_TIME, autoLogoutTime).apply()
    }
    override fun getAutoLogoutTime(): Int {
        return sharedPreferences.getInt(AUTO_LOGOUT_TIME, 0)
    }
    override fun saveOfflineStartSessionState(offlineStartSessionState: Boolean) {
        sharedPreferences.edit().putBoolean(ALLOW_OFFLINE_START_SESSION, offlineStartSessionState).apply()
    }

    override fun saveSerialNumber(serialNumber: String) {
        sharedPreferences.edit().putString(SERIAL_NUMBER, serialNumber).apply()
    }

    override fun getSerialNumber(): String? {
        return sharedPreferences.getString(SERIAL_NUMBER, "")
    }

    override fun saveImei1(imei1: String) {
        sharedPreferences.edit().putString(IMEI1, imei1).apply()
    }

    override fun getImei1(): String? {
        return sharedPreferences.getString(IMEI1, "")
    }

    override fun saveImei2(imei2: String) {
        sharedPreferences.edit().putString(IMEI2, imei2).apply()
    }

    override fun getImei2(): String? {
        return sharedPreferences.getString(IMEI2, "")
    }

    override fun getOfflineStartSessionState(): Boolean {
        return sharedPreferences.getBoolean(ALLOW_OFFLINE_START_SESSION, false)
    }

    override fun saveOfflineFinishSessionState(offlineFinishSessionState: Boolean) {
        sharedPreferences.edit().putBoolean(ALLOW_OFFLINE_FINISH_SESSION, offlineFinishSessionState)
            .apply()
    }

    override fun getOfflineFinishSessionState(): Boolean {
        return sharedPreferences.getBoolean(ALLOW_OFFLINE_FINISH_SESSION, false)
    }

    override fun saveVersionAppActiveState(activeState: Boolean) {
        sharedPreferences.edit().putBoolean(APP_VERSION_ACTIVE_STATE, activeState).apply()
    }
    override fun getVersionAppActiveState(): Boolean {
        return sharedPreferences.getBoolean(APP_VERSION_ACTIVE_STATE, false)
    }
    override fun saveStatusDownloadLicense(status: Boolean) {
        sharedPreferences.edit().putBoolean(STATUS_DOWNLOAD_LICENSE, status).apply()
    }

    override fun getStatusDownloadLicense(): Boolean {
        return sharedPreferences.getBoolean(STATUS_DOWNLOAD_LICENSE, true)
    }

    override fun saveTeacherCode(code: String) {
        sharedPreferences.edit().putString(TEACHER_CODE, code).apply()
    }

    override fun removeTeacherCode() {
        sharedPreferences.edit().putString(TEACHER_CODE, null).apply()
    }

    override fun getTeacherCode(): String? {
        return sharedPreferences.getString(TEACHER_CODE, null)
    }

    override fun saveStudentCode(code: String) {
        sharedPreferences.edit().putString(STUDENT_CODE, code).apply()
    }

    override fun removeStudentCode() {
        sharedPreferences.edit().putString(STUDENT_CODE, null).apply()
    }

    override fun getStudentCode(): String? {
        return sharedPreferences.getString(STUDENT_CODE, null)
    }

    override fun saveSessionCode(code: String) {
        sharedPreferences.edit().putString(SESSION_ID, code).apply()
    }

    override fun removeSessionCode() {
        sharedPreferences.edit().putString(SESSION_ID, null).apply()
    }

    override fun getSessionCode(): String? {
        return sharedPreferences.getString(SESSION_ID, null)
    }

    override fun saveTrainingCenterName(name: String?) {
        sharedPreferences.edit().putString(TRAINING_CENTER_NAME, name).apply()
    }

    override fun getTrainingCenterName(): String? {
        return sharedPreferences.getString(TRAINING_CENTER_NAME, null)
    }

    override fun savePlateSlug(plateSlug: String?) {
        sharedPreferences.edit().putString(PLATE_SLUG, plateSlug).apply()
    }

    override fun getPlateSlug(): String? {
        return sharedPreferences.getString(PLATE_SLUG, null)
    }

    override fun getListUserLocal(): List<UserEntity> {
        return try {
            val listUserEntity = appDatabase.userEntityDao().getListUserEntity()
            Logger.i("getListUserLocal listUserEntity: $listUserEntity")
            listUserEntity
        } catch (ex: Exception) {
            Logger.i("Error ${ex.message}")
            emptyList()
        }
    }

    override fun insertNewUser(newUser: UserEntity) {
        try {
            Logger.i("insertNewUser newUser: $newUser")
            appDatabase.userEntityDao().insert(newUser)
        } catch (ex: Exception) {
            Logger.i("Error ${ex.message}")
        }
    }

    override fun updateStudyResult(
        userId: String,
        totalTimeStudied: Double,
        totalDistanceRode: Float
    ) {
        try {
            Logger.i(
                "updateStudyResult userId: $userId | " +
                    "totalTimeStudied: $totalTimeStudied | " + "totalDistanceRode: $totalDistanceRode"
            )
            appDatabase.userEntityDao().updateCurrentStudyResult(
                userId = userId,
                sessionTimeStudied = totalTimeStudied,
                sessionDistanceRode = totalDistanceRode
            )
        } catch (ex: Exception) {
            Logger.i("Error ${ex.message}")
        }
    }

    override fun updateUserBasicInfo(
        userId: String,
        userName: String,
        gender: Int,
        fullName: String,
        phoneNumber: String,
        userCode: String,
        address: String,
        avatarId: String?,
        trainingCenterId: String,
        birthday: String,
        courseId: String?,
        courseCode: String?,
        courseLicense: String?,
        nfcId: String?,
        totalTimeStudied: Double?,
        totalDistanceRode: Float?,
        totalCourseTime: Double?,
        totalCourseDistance: Float?
    ) {
        try {
            Logger.i(
                "updateUserBasicInfo userId: $userId | " +
                    "userName: $userName | " + "gender: $gender | " +
                    "fullName: $fullName | " + "phoneNumber: $phoneNumber | " +
                    "userCode: $userCode | " + "address: $address | avatarId: $avatarId" +
                    "trainingCenterId: $trainingCenterId | " + "birthday: $birthday | " +
                    "courseId: $courseId | " + "courseCode: $courseCode | " +
                    "courseLicense: $courseLicense | " + "nfcId: $nfcId | " +
                    "totalTimeStudied: $totalTimeStudied | " + "totalDistanceRode: $totalDistanceRode | " +
                    "totalCourseTime: $totalCourseTime | " + "totalCourseDistance: $totalCourseDistance | "
            )
            appDatabase.userEntityDao().updateUserBasicInfo(
                userId = userId,
                userName = userName,
                gender = gender,
                fullName = fullName,
                phoneNumber = phoneNumber,
                userCode = userCode,
                address = address,
                avatarId = avatarId,
                trainingCenterId = trainingCenterId,
                birthday = birthday,
                courseId = courseId,
                courseCode = courseCode,
                courseLicense = courseLicense,
                nfcId = nfcId,
                totalTimeStudied = totalTimeStudied,
                totalDistanceRode = totalDistanceRode,
                totalCourseTime = totalCourseTime,
                totalCourseDistance = totalCourseDistance
            )
        } catch (ex: Exception) {
            Logger.i("Error ${ex.message}")
        }
    }

    override fun updateFaceGroupReady(userId: String, faceGroupReady: Boolean) {
        try {
            Logger.i("updateFaceGroupReady userId: $userId | faceGroupReady: $faceGroupReady")
            appDatabase.userEntityDao().updateFaceGroupReady(userId, faceGroupReady)
        } catch (ex: Exception) {
            Logger.i("Error ${ex.message}")
        }
    }

    override fun getAllFaceRecogImage(): List<FaceRecognitionImageEntity> {
        return try {
            val listFaceRecogImage = appDatabase.faceRecogImageEntityDao().getAllFaceRecogImage()
            Logger.i("getAllFaceRecogImage listFaceRecogImage: $listFaceRecogImage")
            listFaceRecogImage
        } catch (ex: Exception) {
            Logger.i("Error ${ex.message}")
            emptyList()
        }
    }

    override fun insertNewFaceRecogImage(faceRecogImage: FaceRecognitionImageEntity) {
        try {
            Logger.i("insertNewFaceRecogImage faceRecogImage: $faceRecogImage")
            appDatabase.faceRecogImageEntityDao().insert(faceRecogImage)
        } catch (ex: Exception) {
            Logger.i("Error ${ex.message}")
        }
    }

    override fun deleteFaceRecogImage(imageId: String) {
        try {
            Logger.i("deleteFaceRecogImage imageId: $imageId")
            appDatabase.faceRecogImageEntityDao().deleteFaceRecogImage(imageId)
        } catch (ex: Exception) {
            Logger.i("Error ${ex.message}")
        }
    }

    // [DAT CER]: only use for get DAT certification
    override fun insertNewRiderSession(riderSessionEntity: RiderSessionEntity): RiderSessionEntity? {
        return try {
            Logger.i("insertNewRiderSession riderSessionEntity: $riderSessionEntity")
            appDatabase.riderSessionEntityDao().insert(riderSessionEntity)
            val lastSession: RiderSessionEntity? =
                appDatabase.riderSessionEntityDao().getLastRiderSession()
            lastSession?.let {
                riderSessionEntity.id = it.id
                riderSessionEntity
            } ?: let { null }
        } catch (ex: Exception) {
            Logger.i("Error ${ex.message}")
            null
        }
    }

    override fun getListSessionNotFinishOnline(): List<RiderSessionEntity> {
        return try {
            Logger.i("getListSessionNotFinishOnline")
            appDatabase.riderSessionEntityDao().getListSessionNotFinishOnline()
        } catch (ex: Exception) {
            Logger.i("Error ${ex.message}")
            emptyList()
        }
    }

    override fun getListAuthenOfflineBySessionId(localSessionId: Long): List<StudentAuthenticationEntity> {
        return try {
            Logger.i("getListAuthenOfflineBySessionId")
            appDatabase.studentAuthenticationEntityDao()
                .getListAuthenOfflineBySessionId(localSessionId)
        } catch (ex: Exception) {
            Logger.i("Error ${ex.message}")
            emptyList()
        }
    }

    override fun getListAuthenDataUploadFailed(localSessionId: Long): List<StudentAuthenticationEntity> {
        return try {
            Logger.i("getListAuthenDataUploadFailed")
            appDatabase.studentAuthenticationEntityDao()
                .getListAuthenDataUploadFailed(localSessionId)
        } catch (ex: Exception) {
            Logger.i("Error ${ex.message}")
            emptyList()
        }
    }
    override fun getListAuthDataRecognitionSuccess(localSessionId: Long): List<StudentAuthenticationEntity> {
        return try {
            appDatabase.studentAuthenticationEntityDao()
                .getListAuthDataRecognitionSuccess(localSessionId)
        } catch (ex: Exception) {
            Logger.i("Error ${ex.message}")
            emptyList()
        }
    }
    override fun getListGpsSignalOfflineBySessionId(localSessionId: Long): List<GPSSignalEntity> {
        return try {
            Logger.i("getListGpsSignalOfflineBySessionId")
            appDatabase.gpsSignalEntityDao().getListGpsSignalOfflineBySessionId(localSessionId)
        } catch (ex: Exception) {
            Logger.i("Error ${ex.message}")
            emptyList()
        }
    }

    override fun getListGpsDataUploadFailed(localSessionId: Long): List<GPSSignalEntity> {
        return try {
            Logger.i("getListGpsDataUploadFailed")
            appDatabase.gpsSignalEntityDao().getListGpsDataUploadFailed(localSessionId)
        } catch (ex: Exception) {
            Logger.i("Error ${ex.message}")
            emptyList()
        }
    }

    override fun getListGPSSignalByLocalSessionId(localSessionId: Long): List<GPSSignalEntity> {
        return try {
            Logger.i("getListGPSSignalByLocalSessionId")
            appDatabase.gpsSignalEntityDao()
                .getListGPSSignalByLocalSessionId(localSessionId = localSessionId)
        } catch (ex: Exception) {
            Logger.i("Error ${ex.message}")
            emptyList()
        }

    }
    override fun getFailureGPSUploadsCount(localSessionId: Long): Int {
        return try {
            Logger.i("getFailureGPSUploadsCount")
            appDatabase.gpsSignalEntityDao().getFailureGPSUploadsCount(localSessionId)
        } catch (ex: Exception) {
            Logger.i("Error ${ex.message}")
            0
        }
    }
    override fun getSuccessfulGPSUploadsCount(localSessionId: Long): Int {
        return try {
            Logger.i("getCountGpsDataUploadSuccess")
            appDatabase.gpsSignalEntityDao().getSuccessfulGPSUploadsCount(localSessionId)
        } catch (ex: Exception) {
            Logger.i("Error ${ex.message}")
            0
        }
    }
    override fun getTotalGPSCount(localSessionId: Long): Int {
        return try {
            Logger.i("getCountGPSSignal")
            appDatabase.gpsSignalEntityDao().getTotalGPSCount(localSessionId)
        } catch (ex: Exception) {
            Logger.i("Error ${ex.message}")
            0
        }
    }

    override fun getOfflineAuthDataCount(localSessionId: Long): Int {
        return try {
            Logger.i("getOfflineAuthDataCount")
            appDatabase.studentAuthenticationEntityDao().getOfflineAuthDataCount(localSessionId)
        } catch (ex: Exception) {
            Logger.i("Error ${ex.message}")
            0
        }
    }

    override fun getSuccessfulAuthDataUploadCount(localSessionId: Long): Int {
        return try {
            Logger.i("getCountAuthDataUploadSuccess")
            appDatabase.studentAuthenticationEntityDao().getSuccessfulAuthDataUploadCount(localSessionId)
        } catch (ex: Exception) {
            Logger.i("Error ${ex.message}")
            0
        }
    }
    override fun getTotalAuthDataCount(localSessionId: Long): Int {
        return try {
            Logger.i("getCountAuthDataBySessionId")
            appDatabase.studentAuthenticationEntityDao().getTotalAuthDataCount(localSessionId)
        } catch (ex: Exception) {
            Logger.i("Error ${ex.message}")
            0
        }
    }
    override fun getListLogFilePushFail(savedLogState: Boolean): List<RiderSessionEntity> {
        return try {
            Logger.i("getListLogFilePushFail")
            appDatabase.riderSessionEntityDao().getListLogFilePushFail(savedLogState)
        } catch (ex: Exception) {
            Logger.i("Error ${ex.message}")
            emptyList()
        }
    }

    override fun updateLogState(
        id: Long,
        sentLogState: Boolean,
    ) {
        try {
            Logger.i("updateLogState")
            appDatabase.riderSessionEntityDao()
                .updateLogState(id = id, sentLogState = sentLogState)
        } catch (ex: Exception) {
            Logger.i("Error ${ex.message}")
        }
    }

    override fun insertIfNecessaryRiderSession(riderSessionEntity: RiderSessionEntity): RiderSessionEntity? {
        return try {
            Logger.i("insertIfNecessaryRiderSession riderSessionEntity: $riderSessionEntity")
            var foundSession: RiderSessionEntity? = appDatabase.riderSessionEntityDao()
                .findSessionBySessionId(riderSessionEntity.sessionId!!)
            // if not found session in local db -> insert new
            if (foundSession == null) {
                appDatabase.riderSessionEntityDao().insert(riderSessionEntity)
                val lastSession: RiderSessionEntity? =
                    appDatabase.riderSessionEntityDao().getLastRiderSession()
                foundSession = riderSessionEntity
                lastSession?.let {
                    foundSession?.id = it.id
                } ?: let { foundSession = null }
            }
            foundSession
        } catch (ex: Exception) {
            Logger.i("Error ${ex.message}")
            null
        }
    }

    override fun getRiderSessionBySessionId(sessionId: String): RiderSessionEntity? {
        return try {
            Logger.i("getRiderSessionBySessionId sessionId: $sessionId")
            val foundSession: RiderSessionEntity? = appDatabase.riderSessionEntityDao()
                .findSessionBySessionId(sessionId)
            foundSession
        } catch (ex: Exception) {
            Logger.i("Error ${ex.message}")
            null
        }
    }

    override fun updateDrivingProgress(id: Long, totalTime: Double, totalDistance: Float) {
        try {
            Logger.i(
                "updateDrivingProgress id: $id | " +
                    "totalTime: $totalTime | " +
                    "totalDistance: $totalDistance "
            )
            appDatabase.riderSessionEntityDao().updateDrivingProgress(
                id = id,
                totalTime = totalTime,
                totalDistance = totalDistance
            )
        } catch (ex: Exception) {
            Logger.i("Error ${ex.message}")
        }
    }

    override fun updateCheckInterruptTime(id: Long, checkInterruptTime: Long) {
        try {
            Logger.i(
                "updateCheckInterruptTime id: $id | " +
                        "checkInterruptTime: $checkInterruptTime"
            )
            appDatabase.riderSessionEntityDao().updateCheckInterruptTime(
                id = id,
                checkInterruptTime = checkInterruptTime,
            )
        } catch (ex: Exception) {
            Logger.i("Error ${ex.message}")
        }
    }

    override fun updateLoginStatus(
        id: Long,
        loginStatus: String,
    ) {
        try {
            Logger.i(
                "updateLoginStatus id: $id | " +
                        "loginStatus: $loginStatus"
            )
            appDatabase.riderSessionEntityDao().updateLoginStatus(
                id = id,
                loginStatus = loginStatus,
            )
        } catch (ex: Exception) {
            Logger.i("Error ${ex.message}")
        }
    }

    override fun updateSessionDisruptionCount(id: Long, sessionDisruptionCount: Int) {
        try {
            Logger.i(
                "updateSessionDisruptionCount id: $id | " +
                        "sessionDisruptionCount: $sessionDisruptionCount"
            )
            appDatabase.riderSessionEntityDao().updateSessionDisruptionCount(
                id = id,
                sessionDisruptionCount = sessionDisruptionCount,
            )
        } catch (ex: Exception) {
            Logger.i("Error ${ex.message}")
        }
    }

    override fun updateEndSessionInfo(
        id: Long,
        gpsLatEnd: Double,
        gpsLongEnd: Double,
        logoutTime: Double,
        logoutImagePath: String,
        totalTime: Double,
        totalDistance: Float,
        isSendTC: Boolean,
        state: Int
    ) {
        try {
            Logger.i(
                "updateEndSessionInfo id: $id | " +
                    "gpsLatEnd: $gpsLatEnd | " +
                    "gpsLongEnd: $gpsLatEnd | " +
                    "logoutTime: $logoutTime | " +
                    "logoutImagePath: $logoutImagePath | " +
                    "totalTime: $totalTime | " +
                    "totalDistance: $totalDistance | " +
                    "isSendTC: $isSendTC | " +
                    "state: $state | "
            )
            appDatabase.riderSessionEntityDao().updateEndSessionInfo(
                id = id,
                gpsLatEnd = gpsLatEnd,
                gpsLongEnd = gpsLongEnd,
                logoutTime = logoutTime,
                logoutImagePath = logoutImagePath,
                totalTime = totalTime,
                totalDistance = totalDistance,
                isSendTC = isSendTC,
                state = state
            )
        } catch (ex: Exception) {
            Logger.i("Error ${ex.message}")
        }
    }

    override fun updateSessionUploadState(id: Long, state: Int) {
        try {
            Logger.i("updateSessionUploadState id: $id | state: $state | ")
            appDatabase.riderSessionEntityDao().updateUploadState(
                id = id,
                state = state
            )
        } catch (ex: Exception) {
            Logger.i("Error ${ex.message}")
        }
    }

    override fun updateStartSessionToOnline(id: Long, sessionId: String, state: Int) {
        try {
            Logger.i("updateSessionUploadState id: $id | state: $state | sessionId: $sessionId")
            appDatabase.riderSessionEntityDao().updateStartSessionToOnline(
                id = id,
                sessionId = sessionId,
                state = state
            )
        } catch (ex: Exception) {
            Logger.i("Error ${ex.message}")
        }
    }

    override fun insertNewGPSSignal(gpsSignalEntity: GPSSignalEntity) {
        try {
            Logger.i("insertNewGPSSignal gpsSignalEntity: $gpsSignalEntity")
            appDatabase.gpsSignalEntityDao().insert(gpsSignalEntity)
        } catch (ex: Exception) {
            Logger.i("Error ${ex.message}")
        }
    }

    override fun updateGPSSignalUploadState(id: Long, state: Int) {
        try {
            Logger.i("updateGPSSignalUploadState id: $id | state: $state | ")
            appDatabase.gpsSignalEntityDao().updateUploadState(
                id = id,
                state = state
            )
        } catch (ex: Exception) {
            Logger.i("Error ${ex.message}")
        }
    }

    override fun insertNewStudentAuthentication(studentAuthenticationEntity: StudentAuthenticationEntity) {
        try {
            Logger.i("insertNewStudentAuthentication studentAuthenticationEntity: $studentAuthenticationEntity")
            appDatabase.studentAuthenticationEntityDao().insert(studentAuthenticationEntity)
            LogRecorder.i("ảnh xác thực lưu thành công", "")
        } catch (ex: Exception) {
            Logger.e("insertNewStudentAuthentication Error ${ex.message}")
            LogRecorder.e("ảnh xác thực lưu thất bại", "${ex.message}")
        }
    }

    override fun updateStudentAuthenticationUploadState(id: Long, state: Int) {
        try {
            Logger.i("updateStudentAuthenticationUploadState id: $id | state: $state | ")
            appDatabase.studentAuthenticationEntityDao().updateUploadState(
                id = id,
                state = state
            )
        } catch (ex: Exception) {
            Logger.i("Error ${ex.message}")
        }
    }

    override fun getListRiderSessionEntity(): List<RiderSessionEntity> {
        return try {
            appDatabase.riderSessionEntityDao()
                .getListRiderSessionEntity()
        } catch (ex: Exception) {
            Logger.i("Error ${ex.message}")
            emptyList()
        }
    }

    override fun getListAuthenDataByLocalSessionId(localSessionId: Long): List<StudentAuthenticationEntity> {
        return try {
            Logger.i("getListAuthenDataByLocalSessionId localSessionId: $localSessionId")
            appDatabase.studentAuthenticationEntityDao()
                .getListAuthenDataByLocalSessionId(localSessionId)
        } catch (ex: Exception) {
            Logger.i("Error ${ex.message}")
            emptyList()
        }
    }

    override suspend fun getUserLocalByRfidCode(cardData: String): UserEntity? {
        return try {
            Logger.i("getUserLocalByRfidCode cardData: $cardData")
            appDatabase.userEntityDao().getUserByRfidCode(nfcId = cardData)
        } catch (ex: Exception) {
            Logger.i("Error ${ex.message}")
            null
        }
    }

    override suspend fun getUserLocalByUserCode(userCode: String): UserEntity? {
        return try {
            Logger.i("getUserLocalByUserCode userCode: $userCode")
            appDatabase.userEntityDao().getUserByUserCode(userCode = userCode)
        } catch (ex: Exception) {
            Logger.i("Error ${ex.message}")
            null
        }
    }

    override suspend fun getLastRiderSession(): RiderSessionEntity? {
        return try {
            Logger.i("getInProgressSessionLocal")
            appDatabase.riderSessionEntityDao().getLastRiderSession()
        } catch (ex: Exception) {
            Logger.i("Error ${ex.message}")
            null
        }
    }
}
