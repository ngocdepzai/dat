package com.hc.dat.model.repository

import com.hc.dat.model.CarInfo
import com.hc.dat.model.UserInfo
import com.hc.dat.model.database.entity.*
import com.hc.dat.model.result.ResponseResult
import com.hc.dat.service.model.*
import hc.manager.datapp.models.response.ResentSessionResponse
import kotlinx.coroutines.channels.Channel
import java.io.File

/**
 * Created by Duc Bui on 2023/06.
 * Author: Duc Bui
 * Email: ducbui1890@gmail.com
 * Hanoi, VN.
 */
interface Repository {
    suspend fun getUserInfoByRfidCode(cardData: String, imei: String): ResponseResult<UserInfo?>
    suspend fun getUserLocalByRfidCode(cardData: String): UserEntity?
    suspend fun getUserLocalByUserCode(userCode: String): UserEntity?
    suspend fun getLastRiderSession(): RiderSessionEntity?
    suspend fun getUserInfoByUserCode(code: String, imei: String): ResponseResult<UserInfo?>
    suspend fun getListUserAssignInDevice(imei: String): ResponseResult<List<UserInfo>>
    suspend fun getObjectsLinkedDat(imei: String): ResponseResult<GetObjectsLinkedDatResponse?>
    suspend fun getDeviceConfig(
        seri: String,
        versionCode: String,
        imei: String,
        SIMSerialNumber: String,
    ): ResponseResult<DeviceConfigResponse?>

    //    suspend fun getCurrentSessionInfo(sessionId: String): ResponseResult<GetCurrentSessionInfoResponse?>
    suspend fun checkStudentAvailable(imei: String, studentCode: String): ResponseResult<Boolean>
    suspend fun checkMissingDataSession(sessionId: String): ResponseResult<CheckMissingDataSessionResponse>
    suspend fun fetchCurrentSession(sessionId: String): ResponseResult<FetchCurrentSessionResponse?>
    suspend fun getCarsByImeiAndCourse(
        imei: String,
        idCourse: String
    ): ResponseResult<List<CarInfo>>

    suspend fun getCarsByImeiAndTrainingCenter(
        imei: String,
        idTrainingCenter: String
    ): ResponseResult<List<CarInfo>>

    suspend fun getInProgressSessionByUser(userCode: String): ResponseResult<StudentSessionInProgressResponse?>
    suspend fun uploadImageStartSession(
        file: File,
        userCode: String,
        imei: String,
        searchScore: Float? = null
    ): ResponseResult<String?>

    suspend fun getAPIPathUploadImage(
        seri: String,
    ): ResponseResult<String?>

    suspend fun uploadImageInRecognition(
        files: List<File>,
        userCode: String,
        imei: String
    ): ResponseResult<Any?>

    suspend fun startRiderSession(
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
        networkStatus: Byte? = null
    ): ResponseResult<StartRiderSessionResponse?>

    suspend fun finishRiderSession(
        imei: String,
        studentCode: String,
        gpsLat: Double,
        gpsLong: Double,
        sessionId: String,
        logoutTime: Double,
        logoutImageUrl: String,
        isSendTC: Boolean,
        coverReSend: Boolean = false // coverReSend = true when resend session save in local device
    ): ResponseResult<Any?>

    suspend fun getRequestRetrieverLog(imei: String): ResponseResult<GetRequestRetrieverLogResponse>
    suspend fun uploadUserLog(file: File, userCode: String, imei: String): ResponseResult<Any?>

    suspend fun getRequestRetrieverLog2(
        imei: String
    ): ResponseResult<GetRequestRetrieverLog2Response>

    suspend fun uploadLogs(
        files: List<File>,
        imei: String
    ): ResponseResult<Any?>
    suspend fun getListSessionHistory(
        sessionHistoryRequest: SessionHistoryRequest
    ):ResponseResult<SessionHistoryResponse?>

    suspend fun uploadDeviceInfo(
        deviceInfo: UploadDeviceInfoRequest
    ):ResponseResult<UploadDeviceInfoResponse>

    // Local query data
    fun saveAutoLogoutTime(autoLogoutTime : Int)
    fun getAutoLogoutTime(): Int
    fun saveOfflineStartSessionState(offlineStartSessionState : Boolean)
    fun saveSerialNumber(serialNumber : String)
    fun getSerialNumber(): String?
    fun saveImei1(imei1 : String)
    fun getImei1(): String?
    fun saveImei2(imei2 : String)
    fun getImei2(): String?
    fun getOfflineStartSessionState(): Boolean
    fun saveOfflineFinishSessionState(offlineFinishSessionState : Boolean)
    fun getOfflineFinishSessionState(): Boolean
    fun saveVersionAppActiveState(activeState: Boolean)
    fun getVersionAppActiveState(): Boolean
    fun saveStatusDownloadLicense(status: Boolean)
    fun getStatusDownloadLicense(): Boolean
    fun saveTeacherCode(code: String)
    fun removeTeacherCode()
    fun getTeacherCode(): String?
    fun saveStudentCode(code: String)
    fun removeStudentCode()
    fun getStudentCode(): String?
    fun saveSessionCode(code: String)
    fun removeSessionCode()
    fun getSessionCode(): String?
    fun saveTrainingCenterName(name: String?)
    fun getTrainingCenterName(): String?
    fun savePlateSlug(plateSlug: String?)
    fun getPlateSlug(): String?

    fun getListUserLocal(): List<UserEntity>
    fun insertNewUser(newUser: UserEntity)
    fun updateUserBasicInfo(
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
    )

    fun updateStudyResult(
        userId: String,
        totalTimeStudied: Double,
        totalDistanceRode: Float
    )

    fun updateFaceGroupReady(
        userId: String,
        faceGroupReady: Boolean
    )

    fun getAllFaceRecogImage(): List<FaceRecognitionImageEntity>
    fun insertNewFaceRecogImage(faceRecogImage: FaceRecognitionImageEntity)
    fun deleteFaceRecogImage(imageId: String)

    // [DAT CER]: only use for get DAT certification
    fun insertNewRiderSession(riderSessionEntity: RiderSessionEntity): RiderSessionEntity?
    fun insertIfNecessaryRiderSession(riderSessionEntity: RiderSessionEntity): RiderSessionEntity?
    fun getRiderSessionBySessionId(sessionId: String): RiderSessionEntity?
    fun getListSessionNotFinishOnline(): List<RiderSessionEntity>
    fun getListAuthenDataByLocalSessionId(localSessionId: Long): List<StudentAuthenticationEntity>
    fun getListRiderSessionEntity(): List<RiderSessionEntity>
    fun getListAuthenOfflineBySessionId(localSessionId: Long): List<StudentAuthenticationEntity>
    fun getListGpsSignalOfflineBySessionId(localSessionId: Long): List<GPSSignalEntity>

    fun getListAuthenDataUploadFailed(localSessionId: Long): List<StudentAuthenticationEntity>
    fun getListAuthDataRecognitionSuccess(localSessionId: Long): List<StudentAuthenticationEntity>
    fun getListGpsDataUploadFailed(localSessionId: Long): List<GPSSignalEntity>
    fun getListGPSSignalByLocalSessionId(localSessionId: Long): List<GPSSignalEntity>
    fun getFailureGPSUploadsCount(localSessionId: Long): Int
    fun getSuccessfulGPSUploadsCount(localSessionId: Long): Int
    fun getTotalGPSCount(localSessionId: Long): Int
    fun getSuccessfulAuthDataUploadCount(localSessionId: Long): Int
    fun getOfflineAuthDataCount(localSessionId: Long): Int
    fun getTotalAuthDataCount(localSessionId: Long): Int
    fun getListLogFilePushFail(savedLogState: Boolean): List<RiderSessionEntity>
    fun updateLogState(
        id: Long,
        sentLogState: Boolean,
    )

    fun updateDrivingProgress(
        id: Long,
        totalTime: Double,
        totalDistance: Float
    )

    fun updateCheckInterruptTime(
        id: Long,
        checkInterruptTime: Long,
    )

    fun updateLoginStatus(
        id: Long,
        loginStatus: String,
    )

    fun updateSessionDisruptionCount(
        id: Long,
        sessionDisruptionCount : Int,
    )

    fun updateEndSessionInfo(
        id: Long,
        gpsLatEnd: Double,
        gpsLongEnd: Double,
        logoutTime: Double,
        logoutImagePath: String,
        totalTime: Double,
        totalDistance: Float,
        isSendTC: Boolean,
        state: Int
    )

    fun updateSessionUploadState(
        id: Long,
        state: Int
    )

    fun updateStartSessionToOnline(
        id: Long,
        sessionId: String,
        state: Int
    )
    suspend fun resentSession(sessionId: String): ResponseResult<ResentSessionResponse?>
    fun insertNewGPSSignal(gpsSignalEntity: GPSSignalEntity)
    fun updateGPSSignalUploadState(
        id: Long,
        state: Int
    )

    fun insertNewStudentAuthentication(studentAuthenticationEntity: StudentAuthenticationEntity)
    fun updateStudentAuthenticationUploadState(
        id: Long,
        state: Int
    )

    fun exportSessionReport(
        riderSessionEntity: RiderSessionEntity,
        channel: Channel<Any>?
    )
    // [DAT CER]
}
