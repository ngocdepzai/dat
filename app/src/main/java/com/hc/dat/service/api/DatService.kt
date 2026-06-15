package com.hc.dat.service.api

import com.hc.dat.service.ServiceDefinition
import com.hc.dat.service.model.*
import com.lws.type.Logger
import hc.manager.datapp.models.request.ResentSessionRequest
import hc.manager.datapp.models.response.ResentSessionResponse
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import retrofit2.http.*
import java.io.File

/**
 * APIs for communicate with server
 */
interface DatService {
    @Headers("Content-Type: application/json;charset=UTF-8")
    @POST(ServiceDefinition.CHECK_STUDENT_AVAILABLE_URL)
    suspend fun checkStudentAvailable(
        @Body loginRequest: CheckStudentAvailableRequest
    ): Response<CheckStudentAvailableResponse>

    @Headers("Content-Type: application/json;charset=UTF-8")
    @POST(ServiceDefinition.CHECK_MISSDATA_SESSION)
    suspend fun checkMissingDataSession(
        @Body checkMissingDataSessionRequest: CheckMissingDataSessionRequest
    ): Response<CheckMissingDataSessionResponse>

    @Headers("Content-Type: application/json;charset=UTF-8")
    @POST(ServiceDefinition.GET_OBJECTS_LINKED_DAT_URL)
    suspend fun getObjectsLinkedDat(
        @Body loginRequest: GetObjectsLinkedDatRequest
    ): Response<GetObjectsLinkedDatResponse>

    @Headers("Content-Type: application/json;charset=UTF-8")
    @POST(ServiceDefinition.GET_USER_INFO_URL)
    suspend fun getUserInfo(
        @Body loginRequest: GetUserInfoRequest
    ): Response<GetUserInfoResponse>

    @Headers("Content-Type: application/json;charset=UTF-8")
    @POST(ServiceDefinition.GET_LIST_USER_ASSIGNED_IN_DEVICE_URL)
    suspend fun getListUserAssignInDevice(
        @Body loginRequest: GetListUserAssignInDeviceRequest
    ): Response<GetListUserAssignInDeviceResponse>

    @Headers("Content-Type: application/json;charset=UTF-8")
    @GET(ServiceDefinition.GET_CARS_BY_IMEI_AND_COURSE_URL)
    suspend fun getCarsByImeiAndCourse(
        @Query("deviceSeri") imei: String,
        @Query("courseId") idCourse: String
    ): Response<GetCarsResponse>

    @Headers("Content-Type: application/json;charset=UTF-8")
    @GET(ServiceDefinition.GET_CARS_BY_IMEI_AND_TRAINING_CENTER_URL)
    suspend fun getCarsByImeiAndTrainingCenter(
        @Query("deviceSeri") imei: String,
        @Query("trainingCenterId") idTrainingCenter: String
    ): Response<GetCarsResponse>

    @Headers("Content-Type: application/json;charset=UTF-8")
    @GET(ServiceDefinition.GET_REQUEST_RETRIEVER_LOG_URL)
    suspend fun getRequestRetrieverLog(
        @Query("Seri") imei: String
    ): Response<GetRequestRetrieverLogResponse>

    @Multipart
    @POST(ServiceDefinition.UPLOAD_USER_LOG_URL)
    suspend fun uploadUserLog(
        @Part file: MultipartBody.Part,
        @Part("UserCode") userCode: RequestBody,
        @Part("Seri") imei: RequestBody
    ): Response<UploadUserLogResponse>

    @Headers("Content-Type: application/json;charset=UTF-8")
    @POST(ServiceDefinition.START_RIDER_SESSION_URL)
    suspend fun startRiderSession(
        @Body startRiderSessionRequest: StartRiderSessionRequest
    ): Response<StartRiderSessionResponse>

    @Headers("Content-Type: application/json;charset=UTF-8")
    @POST(ServiceDefinition.FINISH_RIDER_SESSION_V335_URL)
    suspend fun finishRiderSession(
        @Body startRiderSessionRequest: FinishRiderSessionRequest
    ): Response<FinishRiderSessionResponse>

    @Headers("Content-Type: application/json;charset=UTF-8")
    @POST(ServiceDefinition.GET_IN_PROGRESS_SESSION_BY_STUDENT_URL)
    suspend fun getInProgressSessionByStudent(
        @Body getInProgressSessionByStudentRequest: GetInProgressSessionByStudentRequest
    ): Response<StudentSessionInProgressResponse>

    @Headers("Content-Type: application/json;charset=UTF-8")
    @POST(ServiceDefinition.FETCH_CURRENT_SESSION_URL)
    suspend fun fetchCurrentSession(
        @Body getCurrentSessionInfoRequest: GetCurrentSessionInfoRequest
    ): Response<FetchCurrentSessionResponse>

    @Multipart
    @POST()
    suspend fun uploadImageInAuthenProgress(
        @Url url: String = ServiceDefinition.UPLOAD_IMAGE_AUTHEN_PROGRESS_URL,
        @Part file: MultipartBody.Part,
        @Part("userCode") userCode: RequestBody,
        @Part("seri") imei: RequestBody,
        @Part("confidence") confidence: RequestBody
    ): Response<UploadImageStartSessionResponse>

    @Headers("Content-Type: application/json;charset=UTF-8")
    @POST(ServiceDefinition.RESENT_SESSION_TC_URL)
    suspend fun resentSession(
            @Body resentSessionRequest: ResentSessionRequest
    ): Response<ResentSessionResponse>

    @Multipart
    @POST(ServiceDefinition.UPDATE_IMAGE_RECOGNITION_URL)
    suspend fun uploadImageInRecognition(
        @Part files: List<MultipartBody.Part>,
        @Part("userCode") userCode: RequestBody,
        @Part("seri") imei: RequestBody
    ): Response<UploadImagesRecognitionResponse>

    @Multipart
    @POST(ServiceDefinition.UPLOAD_LOGS)
    suspend fun uploadLogs(
        @Part files: List<MultipartBody.Part>,
        @Part("Seri") imei: RequestBody
    ): Response<UploadUserLogResponse>

    @Headers("Content-Type: application/json;charset=UTF-8")
    @GET(ServiceDefinition.GET_REQUEST_RETRIEVER_LOG_URL_2)
    suspend fun getRequestRetrieverLog2(
        @Query("Seri") imei: String
    ): Response<GetRequestRetrieverLog2Response>

    @Headers("Content-Type: application/json;charset=UTF-8")
    @POST(ServiceDefinition.UPLOAD_DEVICE_INFO)
    suspend fun uploadDeviceInfo(
        @Body deviceInfoRequest: UploadDeviceInfoRequest
    ): Response<UploadDeviceInfoResponse>

    @Headers("Content-Type: application/json;charset=UTF-8")
    @GET(ServiceDefinition.GET_DEVICE_CONFIG)
    suspend fun getDeviceConfig(
        @Query("Seri") seri: String,
        @Query("VersionCode") versionCode: String,
        @Query("Imei") imei: String,
        @Query("SimReal") SIMSerialNumber: String,
    ): Response<DeviceConfigResponse>
    @Headers("Content-Type: application/json;charset=UTF-8")
    @POST(ServiceDefinition.GET_LIST_SESSION)
    suspend fun getListSessionHistory(
        @Body sessionHistoryRequest: SessionHistoryRequest
    ): Response<SessionHistoryResponse>

    @Headers("Content-Type: application/json;charset=UTF-8")
    @GET(ServiceDefinition.GET_API_SAVE_IMAGE)
    suspend fun getAPIPathUploadImage(
        @Query("Seri") seri: String,
    ): Response<GetAPIPathUploadImageResponse>

}

fun File.generateBodyRequest(): MultipartBody.Part? {
    return try {
        val mediaType = "image/*".toMediaTypeOrNull()
        val requestFile = this.asRequestBody(mediaType)
        Logger.i("mediaType: $mediaType | requestFile: $requestFile | File name: ${this.name}")
        MultipartBody.Part.createFormData("file", this.name, requestFile)
    } catch (ex: NullPointerException) {
        Logger.e("generateBodyRequest Failed!!!")
        null
    }
}

fun List<File>.generateBodyRequest(): List<MultipartBody.Part>? {
    return try {
        val mediaType = "image/*".toMediaTypeOrNull()
        this.map {
            val requestFile = it.asRequestBody(mediaType)
            Logger.i("mediaType: $mediaType | requestFile: $requestFile | File name: ${it.name}")
            MultipartBody.Part.createFormData("files", it.name, requestFile)
        }
    } catch (ex: NullPointerException) {
        Logger.e("generateBodyRequest Failed!!!")
        null
    }
}

fun String.generateBodyRequest(): RequestBody {
    val mediaType = "text/plain".toMediaTypeOrNull()
    return this.toRequestBody(mediaType)
}
fun Float?.generateBodyRequest(): RequestBody {
    val mediaType = "application/octet-stream".toMediaTypeOrNull()
    val floatAsString = this.toString()
    return floatAsString.toRequestBody(mediaType)
}
