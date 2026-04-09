package com.hc.dat.viewmodel

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Environment
import androidx.lifecycle.ViewModel
import com.hc.dat.di.ApplicationContext
import com.hc.dat.model.*
import com.hc.dat.model.database.entity.*
import com.hc.dat.model.repository.Repository
import com.hc.dat.model.result.ErrorCode
import com.hc.dat.model.result.ErrorCode.SUCCESS_WITH_ERROR
import com.hc.dat.model.result.ErrorCode.PUSH_SESSION_TO_TC_FAIL
import com.hc.dat.model.result.ResponseResult
import com.hc.dat.service.model.CheckMissingDataSessionResponse
import com.hc.dat.service.model.DateMissing
import com.hc.dat.service.model.FetchCurrentSessionResponse
import com.hc.dat.service.model.SessionHistoryRequest
import com.hc.dat.service.model.SessionHistoryResponse
import com.hc.dat.service.model.StartRiderSessionResponse
import com.hc.dat.service.model.StudentSessionInProgressResponse
import com.hc.dat.utils.Utils
import com.lws.device.Device
import com.lws.device.gps.GPSEvent
import com.lws.device.network.NetworkConnectionAction
import com.lws.device.network.NetworkConnectionEvent
import com.lws.type.LogRecorder
import com.lws.type.Logger
import hc.manager.datapp.BuildConfig
import hc.manager.datapp.models.AuthModel
import hc.manager.datapp.models.GpsModel
import com.hc.dat.service.Sender
import com.hc.dat.service.SenderAuth
import com.hc.dat.utils.SentryLogUploader
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.io.File
import java.lang.Math.floor
import java.text.ParseException
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.*
import javax.inject.Inject

data class RiderSessionViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: Repository,
    private val device: Device
) : ViewModel() {
    var teacherAuthInfo: UserEntity? = null
    var studentAuthInfo: UserEntity? = null
    var sessionVerificationInfo: SessionVerificationInfo = SessionVerificationInfo()
    var inProgressSession: InProgressSession? = null
    private var localRiderSession: RiderSessionEntity? = null
    private var fetchSessionJob: Job? = null
    private var recoverUploadInProgress = false
    private var recoverUploadLogFiles = false
    private var isReUploadDataInSession = false
    var notifyErrorVelocity = false
    var notifyUse4G = true
    var notifyDistanceNotChange = true

    private val networkConnectionEvent = object : NetworkConnectionEvent {
        override fun onNetworkUpdate(
            action: NetworkConnectionAction,
            connectionAvailable: Boolean,
            internetAvailable: Boolean
        ) {
            Logger.i("onNetworkUpdate action: $action | connectionAvailable: $connectionAvailable | internetAvailable: $internetAvailable")
            // start recover send local data when internet available
            if (
                (action == NetworkConnectionAction.NETWORK_INIT && internetAvailable) ||
                (action == NetworkConnectionAction.NETWORK_AVAILABLE && internetAvailable)
            ) {
                // call recover
                Logger.i("networkConnectionEvent recoverUploadInProgress: $recoverUploadInProgress")
                if (!recoverUploadInProgress) {
                    recoverUploadInProgress = true
                    recoverSendOfflineData()
                }
                if(!recoverUploadLogFiles){
                    recoverUploadLogFiles = true
                    recoverSendLogFilesFail()
                }
            }
        }
    }

    //Calculate the time when re-entering the application
    suspend fun calculateAuthenticationPeriod(
        timeFrequencySentData: Long
    ): Long {
        var lastAuthenTime: Long? = getLastAuthTime()

        return if (lastAuthenTime != null) {
            // convert milliseconds to seconds
            val currentTime = Calendar.getInstance().timeInMillis / 1000
            lastAuthenTime /= 1000

            if (currentTime - lastAuthenTime >= timeFrequencySentData) {
                // Time send data instantly
                15
            } else {
                timeFrequencySentData - (currentTime - lastAuthenTime)
            }
        } else {
            // Time send data instantly
            15
        }
    }


    // method for checking list data push failed in session
    // next action is try push them again
    @Synchronized
    suspend fun checkAndReUploadData() {
        if (device.getCurrentNetworkConnection()?.checkConnectionAvailable() == true
            && localRiderSession != null && localRiderSession!!.sessionId?.isNotEmpty() == true
            && !isReUploadDataInSession) {
                // set flag inprogress
                isReUploadDataInSession = true
                // get list authentication data haven't just upload
                val listAuthenUploadFailed: List<StudentAuthenticationEntity> =
                    repository.getListAuthenDataUploadFailed(localSessionId = localRiderSession!!.id)

                Logger.i("checkAndReUploadData for listAuthenUploadFailed: $listAuthenUploadFailed")
                LogRecorder.i("checkAndReUploadData for listAuthenUploadFailed:"," $listAuthenUploadFailed")

                listAuthenUploadFailed.forEach { studentAuthentication ->
                    val result = handleRecoverUploadAuthen(
                        riderSessionEntity = localRiderSession!!,
                        studentAuthenticationEntity = studentAuthentication
                    )
                    // update state in local
                    if (result) {
                        repository.updateStudentAuthenticationUploadState(
                            id = studentAuthentication.id,
                            state = AuthenUploadState.SENT_ONLINE.code
                        )
                    }
                }

                // get list GPS Signal data offline if it's exist
                val listGpsUploadFailed: List<GPSSignalEntity> =
                    repository.getListGpsDataUploadFailed(localSessionId = localRiderSession!!.id)
                Logger.i("checkAndReUploadData listGpsUploadFailed: $listGpsUploadFailed")
                listGpsUploadFailed.forEach { gpsSignal ->
                    val result = handleRecoverUploadGpsSignal(
                        riderSessionEntity = localRiderSession!!,
                        gpsSignalEntity = gpsSignal
                    )
                    // update state in local
                    if (result) {
                        repository.updateGPSSignalUploadState(
                            id = gpsSignal.id,
                            state = GPSUploadState.SENT_ONLINE.code
                        )
                    }
                }
                // re-set flag in-progress
                isReUploadDataInSession = false
        }
    }

    @Synchronized
    private fun recoverSendOfflineData() {
        CoroutineScope(Dispatchers.Default).launch(
            CoroutineExceptionHandler { _, ex ->
                Logger.e("recoverSendOfflineData: Found an exception exception: ${ex.message}")
                recoverUploadInProgress = false
            }
        ) {
            // Step 1: get list in-progress session.
            val listSessionRecover: MutableList<RiderSessionEntity> =
                repository.getListSessionNotFinishOnline() as MutableList<RiderSessionEntity>

            // if last session in listSessionRecover is not completed then remove it.
            // Because it will duplicate with ReUpload frequency
            if (listSessionRecover.isNotEmpty()
                && listSessionRecover.last().state != SessionState.START_ONLINE_FINISH_OFFLINE.code
                && listSessionRecover.last().state != SessionState.START_FINISH_OFFLINE.code
                && listSessionRecover.last().state != SessionState.START_ONLINE_FINISH_ONLINE.code) {
                listSessionRecover.removeLast()
            }
            Logger.i("recoverSendOfflineData listSessionRecover: $listSessionRecover")
            val listRecoverSessionData = mutableListOf<RecoverSessionData>()
            listSessionRecover.forEach { riderSessionEntity ->
                // Step 2: get list Student authen data offline if it's exist
                val listAuthenOffline: List<StudentAuthenticationEntity> =
                    repository.getListAuthenOfflineBySessionId(localSessionId = riderSessionEntity.id)
                // Step 3: get list GPS Signal data offline if it's exist
                val listGpsSignalOffline: List<GPSSignalEntity> =
                    repository.getListGpsSignalOfflineBySessionId(localSessionId = riderSessionEntity.id)
                listRecoverSessionData.add(
                    RecoverSessionData(
                        riderSessionEntity = riderSessionEntity,
                        listAuthenData = listAuthenOffline,
                        listGpsSignal = listGpsSignalOffline
                    )
                )
            }
            // Step 4: Recover re-upload data to server
            Logger.i("recoverSendOfflineData listRecoverSessionData.size: ${listRecoverSessionData.size}")
            listRecoverSessionData.forEach { recoverSessionData ->
                // Step 4.1: re-upload session if state is START_OFFLINE or START_FINISH_OFFLINE
                if (recoverSessionData.riderSessionEntity.state == SessionState.START_OFFLINE.code ||
                    recoverSessionData.riderSessionEntity.state == SessionState.START_FINISH_OFFLINE.code
                ) {
                    handleRecoverUploadSession(recoverSessionData.riderSessionEntity)?.also { sessionId ->
                        // check current rider session is this session recover
                        if (localRiderSession?.id == recoverSessionData.riderSessionEntity.id) {
                            // update sessionId to
                            localRiderSession?.sessionId = sessionId
                            inProgressSession?.id = localRiderSession?.sessionId!!
                        }

                        // update data in local session
                        recoverSessionData.riderSessionEntity.sessionId = sessionId
                        recoverSessionData.riderSessionEntity.state =
                            if (recoverSessionData.riderSessionEntity.state == SessionState.START_OFFLINE.code) SessionState.START_ONLINE.code
                            else SessionState.START_ONLINE_FINISH_OFFLINE.code
                        // update session state to sent to online
                        repository.updateStartSessionToOnline(
                            id = recoverSessionData.riderSessionEntity.id,
                            sessionId = sessionId,
                            state = recoverSessionData.riderSessionEntity.state
                        )
                    }
                }
                recoverSessionData.riderSessionEntity.sessionId?.also { _ ->
                    // Step 4.2: Re-upload Student authentication data
                    Logger.i("recoverSendOfflineData listAuthenData: ${recoverSessionData.listAuthenData}")
                    recoverSessionData.listAuthenData.forEach { studentAuthentication ->
                        val result = handleRecoverUploadAuthen(
                            riderSessionEntity = recoverSessionData.riderSessionEntity,
                            studentAuthenticationEntity = studentAuthentication
                        )
                        // update state in local
                        if (result) {
                            repository.updateStudentAuthenticationUploadState(
                                id = studentAuthentication.id,
                                state = AuthenUploadState.SENT_ONLINE.code
                            )
                        }
                    }
                    // Step 4.3: Re-upload GPS Signal data
                    Logger.i("recoverSendOfflineData listGpsSignal: ${recoverSessionData.listGpsSignal}")
                    recoverSessionData.listGpsSignal.forEach { gpsSignal ->
                        val result = handleRecoverUploadGpsSignal(
                            riderSessionEntity = recoverSessionData.riderSessionEntity,
                            gpsSignalEntity = gpsSignal
                        )
                        // update state in local
                        if (result) {
                            repository.updateGPSSignalUploadState(
                                id = gpsSignal.id,
                                state = GPSUploadState.SENT_ONLINE.code
                            )
                        }
                    }
                }
                // Step 4.1: re-upload finish session if state is START_OFFLINE or START_FINISH_OFFLINE
                if (recoverSessionData.riderSessionEntity.state == SessionState.START_ONLINE_FINISH_OFFLINE.code) {
                    recoverSessionData.riderSessionEntity.logoutImagePath?.also {
                        val result =
                            handleRecoverUploadFinishSession(recoverSessionData.riderSessionEntity)
                        // update state in local
                        if (result) {
                            repository.updateSessionUploadState(
                                id = recoverSessionData.riderSessionEntity.id,
                                state = SessionState.START_ONLINE_FINISH_ONLINE.code
                            )
                        }
                    }
                }
            }
            recoverUploadInProgress = false
        }
    }
    fun getLastSuccessRecognitionImage(): File? {
        Logger.d("getLastSuccessRecognitionImage")
        if(localRiderSession != null && localRiderSession!!.sessionId?.isNotEmpty() == true){
            val listAuthDataRecognitionSuccess: List<StudentAuthenticationEntity> =
                repository.getListAuthDataRecognitionSuccess(localSessionId = localRiderSession!!.id)
            Logger.i("listAuthDataRecognitionSuccess: ${listAuthDataRecognitionSuccess.size}")
            if(listAuthDataRecognitionSuccess.isNotEmpty()){
                val lastImageAuthSuccess = listAuthDataRecognitionSuccess.last().authenImagePath
                return File(lastImageAuthSuccess)
            }
        }
        return null
    }
    fun getLastAuthTime(): Long? {
        val lastTimeLocalAuth = localRiderSession?.let {
            repository.getListAuthenDataByLocalSessionId(
                localSessionId = it.id
            ).lastOrNull()?.time
        }

        var lastAuthTime = inProgressSession?.lastAuthenTime?.time ?: lastTimeLocalAuth

        if (lastTimeLocalAuth != null && lastAuthTime != null) {
            lastAuthTime = maxOf(lastAuthTime, lastTimeLocalAuth)
        }
        return lastAuthTime
    }
    fun pushReportFile(riderSessionEntity: RiderSessionEntity) {
        Logger.i("push report file START")
        LogRecorder.i("pushReportFile", "START")
        CoroutineScope(Dispatchers.IO).launch(
            CoroutineExceptionHandler { _, ex ->
                Logger.e("RecoverSendLogFilesFail: Found an exception exception: ${ex.message}")
                recoverUploadLogFiles = false
            }
        ) {
            val hcLogFolder =
                File(Environment.getExternalStorageDirectory().toString() + "/HC_DAT_REPORT")

            val convertSdfNew = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
            val dateFolder = File(hcLogFolder, convertSdfNew.format(Date()))
            if (!dateFolder.exists()) {
                dateFolder.mkdirs()
            }
            val versionApp = BuildConfig.VERSION_NAME
            val fileName =
                "${riderSessionEntity.studentCode}_${riderSessionEntity.sessionId}_$versionApp.csv"

            val reportFile = File(dateFolder, fileName)
            val imei = getImeiDevice(context)
            val result = repository.uploadLogs(
                files = listOf(reportFile),
                imei = imei
            )

            if(result.isError){
                Logger.e("push report file fail: ${result.errorMessage}")
                LogRecorder.e("pushReportFile","push report file fail: ${result.errorMessage}")

                if (reportFile.exists()) {
                    SentryLogUploader.attachLogToEvent(reportFile)
                }

                SentryLogUploader.captureException(
                        throwable = Exception(result.errorMessage ?: "Upload report file failed"),
                        tag = "push_report_file",
                        extras = mapOf(
                                "student_code" to (riderSessionEntity.studentCode ?: ""),
                                "session_id" to (riderSessionEntity.sessionId ?: ""),
                                "report_file_path" to reportFile.absolutePath
                        )
                )

                SentryLogUploader.clearAttachments()
            } else {
                Logger.i("push report file success")
                LogRecorder.i("pushReportFile", "push report file success")

                SentryLogUploader.captureLogFileResult(
                        success = !result.isError,
                        file = reportFile,
                        tag = "push_report_file",
                        sessionId = riderSessionEntity.sessionId,
                        studentCode = riderSessionEntity.studentCode,
                        message = result.errorMessage ?: "Upload report file success"
                )
            }
        }
        Logger.i("push report file END")
        LogRecorder.i("pushReportFile", "push report file END")
    }
    fun pushLogFile(callback: ((action: AppAction, data: Any?) -> Unit)? = null) {
        Logger.d("pushLogFile START")
        LogRecorder.i("pushLogFile","START")
        CoroutineScope(Dispatchers.IO).launch(
            CoroutineExceptionHandler { _, ex ->
                callback?.let {
                    CoroutineScope(Dispatchers.Main).launch {
                        callback(AppAction.UPLOAD_FILE_FAIL, "gửi file thất bại")
                    }
                }
                Logger.e("checkRequestRetrieverLog2: Found an exception exception: ${ex.message}")
            }
        ) {

            // Wait for the log to be saved
            delay(1000)

            if (device.getCurrentNetworkConnection()?.checkConnectionAvailable() == true) {
                val imei = getImeiDevice(context)
                val result = repository.uploadLogs(
                    files = listOf(LogRecorder.recordLogFile!!),
                    imei = imei
                )

                if (result.isError) {
                    Logger.e("PushLogFile error: ${result.errorMessage}, path: ${LogRecorder.recordLogFile!!.path}")

                    LogRecorder.recordLogFile?.let {
                        if (it.exists()) {
                            SentryLogUploader.attachLogToEvent(it)
                        }
                    }

                    SentryLogUploader.captureLogFileResult(
                            success = false,
                            file = LogRecorder.recordLogFile,
                            tag = "push_log_file",
                            sessionId = localRiderSession?.sessionId,
                            studentCode = localRiderSession?.studentCode,
                            message = result.errorMessage
                    )

                    SentryLogUploader.clearAttachments()

                    repository.updateLogState(
                        id = localRiderSession!!.id,
                        sentLogState = false,
                    )
                    callback?.let {
                        withContext(Dispatchers.Main) {
                            callback(AppAction.UPLOAD_FILE_FAIL, "gửi file thất bại")
                            Logger.d("pushLogFile gửi file thất bại")
                            LogRecorder.i("pushLogFile","gửi file thất bại")
                        }
                    }
                } else {
                    Logger.e("PushLogFile success: ${result.errorMessage}, path: ${LogRecorder.recordLogFile!!.path}")
                    repository.updateLogState(
                        id = localRiderSession!!.id,
                        sentLogState = true,
                    )

                    SentryLogUploader.captureLogFileResult(
                            success = true,
                            file = LogRecorder.recordLogFile,
                            tag = "push_log_file",
                            sessionId = localRiderSession?.sessionId,
                            studentCode = localRiderSession?.studentCode,
                            message = "Upload thành công"
                    )

                    callback?.let {
                        withContext(Dispatchers.Main) {
                            callback(AppAction.UPLOAD_FILE_SUCCESS, "gửi file thành công")
                            Logger.d("pushLogFile gửi file thành công")
                            LogRecorder.i("pushLogFile","gửi file thành công")
                        }
                    }
                }
            } else {
                Logger.e("PushLogFile error path: ${LogRecorder.recordLogFile!!.path}")

                repository.updateLogState(
                    id = localRiderSession!!.id,
                    sentLogState = false,
                )
                callback?.let {
                    withContext(Dispatchers.Main) {
                        callback(AppAction.UPLOAD_FILE_FAIL, "Không có kết nối internet")
                    }
                }
            }
            LogRecorder.recordLogFile = null
            // handle delete old log files
            handleDeleteOldLogs()
        }
        Logger.d("pushLogFile END")
        LogRecorder.i("pushLogFile","END")
    }

    fun recoverSendLogFilesFail() {
        Logger.d("recoverSendLogFilesFail")
        CoroutineScope(Dispatchers.IO).launch(
            CoroutineExceptionHandler { _, ex ->
                Logger.e("RecoverSendLogFilesFail: Found an exception exception: ${ex.message}")
                recoverUploadLogFiles = false
            }
        ) {
            val hcLogFolder: File = File(Environment.getExternalStorageDirectory().toString() + "/HC_DAT_LOGS")
            val imei = getImeiDevice(context)
            val listLogFilePushFail = repository.getListLogFilePushFail(false)
            Logger.i("recover file list: ${listLogFilePushFail.size}")

            val listFile = mutableListOf<File>()

            // get log file path
            listLogFilePushFail.forEach { riderSessionEntity ->
                val dateString = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(
                    Date(
                        riderSessionEntity.loginTime.toLong() * 1000
                    )
                )
                val dateFolder = File(hcLogFolder, dateString)
                val versionApp = BuildConfig.VERSION_NAME
                val logFile = File(dateFolder, "${riderSessionEntity.studentCode}_${riderSessionEntity.sessionId}_$versionApp.log")
                listFile.add(logFile)
            }
            val result = repository.uploadLogs(
                files = listFile,
                imei = imei
            )
            if (!result.isError) {
                Logger.e("Recover Log Files SUCCESS")
                listLogFilePushFail.forEach {
                    repository.updateLogState(
                        id = it.id,
                        sentLogState = true
                    )
                }
            }
            recoverUploadLogFiles = false
        }
    }

    private fun handleDeleteOldLogs() {
        val folderPath = Environment.getExternalStorageDirectory().toString() + "/HC_DAT_LOGS"
        val formatDate = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val sevenDateBefore = Date.from(LocalDate.now().minusDays(7).atStartOfDay(ZoneId.systemDefault()).toInstant())
            Logger.i("handleDeleteOldLogs sevenDateBefore: $sevenDateBefore")
            File(folderPath).listFiles()?.forEach { file ->
                Logger.i("handleDeleteOldLogs file name: ${file.name}")
                try {
                    val folderDate: Date? = formatDate.parse(file.name)
                    folderDate?.run {
                        if (folderDate.before(sevenDateBefore)) {
                            Logger.i("handleDeleteOldLogs log file expired -> delete: ${file.name}")
                            file.deleteRecursively()
                        }
                    }
                } catch (ex: ParseException) {
                    Logger.w("Folder's name wrong format -> delete folder: ${file.name}")
                    file.deleteRecursively()
                }
            }
        }

    }
    private fun getAuthenDataUploadResult(): Triple<Int, Int, Int> {
        var totalAuthData: Int = 0
        var totalAuthDataUploadSuccess = 0
        var totalOfflineAuthData = 0
        localRiderSession?.let {
            totalAuthData =
                repository.getTotalAuthDataCount(localSessionId = localRiderSession!!.id)
            totalAuthDataUploadSuccess =
                repository.getSuccessfulAuthDataUploadCount(localSessionId = localRiderSession!!.id)
            totalOfflineAuthData =
                repository.getOfflineAuthDataCount(localSessionId = localRiderSession!!.id)
        }

        return Triple(totalAuthDataUploadSuccess, totalAuthData, totalOfflineAuthData)
    }

    private fun getGpsDataUploadResult(): Triple<Int, Int, Int> {
        var totalGpsData = 0
        var totalGpsDataUploadSuccess = 0
        var totalGPSUploadFail = 0
        localRiderSession?.let {
            totalGpsData =
                repository.getTotalGPSCount(localSessionId = localRiderSession!!.id)
            totalGpsDataUploadSuccess =
                repository.getSuccessfulGPSUploadsCount(localSessionId = localRiderSession!!.id)
            totalGPSUploadFail =
                repository.getFailureGPSUploadsCount(localSessionId = localRiderSession!!.id)
        }

        return Triple(totalGpsDataUploadSuccess, totalGpsData, totalGPSUploadFail)
    }
    fun updateDrivingProgress(totalTime: Double, totalDistance: Float) {
        CoroutineScope(Dispatchers.Default).launch(
            CoroutineExceptionHandler { _, ex ->
                Logger.e("updateDrivingProgress: Found an exception exception: ${ex.message}")
            }
        ) {
            localRiderSession?.also {
                repository.updateDrivingProgress(
                    id = it.id,
                    totalTime,
                    totalDistance
                )
            }
        }
    }
    private fun updateSessionDisruptionCount() {
        Logger.d("updateSessionDisruptionCount")
        CoroutineScope(Dispatchers.Default).launch(
            CoroutineExceptionHandler { _, ex ->
                Logger.e("updateSessionDisruptionCount: Found an exception exception: ${ex.message}")
            }
        ) {
            localRiderSession?.apply {
                localRiderSession!!.sessionDisruptionCount += 1
                repository.updateSessionDisruptionCount(
                    id = this.id,
                    sessionDisruptionCount = localRiderSession!!.sessionDisruptionCount,
                )
            }
        }
    }
    private fun getSessionDisruptionCount(): Int {
        Logger.d("getSessionDisruptionCount")
        return localRiderSession?.let {
            localRiderSession!!.sessionDisruptionCount
        } ?: 0
    }
    fun canContinueSessionAfterDisruption(interruptedTime: Int): Boolean{
        val allowedDisruptionTime = 20 // minutes
        updateSessionDisruptionCount()
        val sessionDisruptionCount = getSessionDisruptionCount()
        /* If the interruption exceeds 20 minutes or
           the interruption is announced more than twice,
           the session will end */
        if(interruptedTime > allowedDisruptionTime || sessionDisruptionCount >= 2){
           return false
        }
        return  true
    }

    fun saveInProgressSessionInterruptTime() {
        Logger.d("saveInProgressSessionInterruptTime")
        CoroutineScope(Dispatchers.Default).launch(
            CoroutineExceptionHandler { _, ex ->
                Logger.e("saveInProgressSessionInterruptTime: Found an exception exception: ${ex.message}")
            }
        ) {
            localRiderSession?.apply {
                checkInterruptTime = Utils.getRealTimeStamp()
                repository.updateCheckInterruptTime(
                    id = this.id,
                    checkInterruptTime = checkInterruptTime!!,
                )
            }
        }

    }

    fun getInProgressSessionInterruptInMinutes(): Int {
        Logger.d("getInProgressSessionInterruptInMinutes")
        return localRiderSession?.let {
            localRiderSession!!.checkInterruptTime?.let { checkInterruptTime ->
                ((Utils.getRealTimeStamp() - checkInterruptTime) / (60 * 1000)).toInt()
            } ?: 0
        } ?: 0
    }

    private suspend fun handleRecoverUploadGpsSignal(
        riderSessionEntity: RiderSessionEntity,
        gpsSignalEntity: GPSSignalEntity
    ): Boolean {
        try {
            val gpsModel = GpsModel()
            gpsModel.apply {
                sessionId = riderSessionEntity.sessionId
                seri = riderSessionEntity.imei
                lat = gpsSignalEntity.gpsLat
                lng = gpsSignalEntity.gpsLong
                userCode = riderSessionEntity.studentCode
                dis = gpsSignalEntity.gpsDistance
                teacherCode = riderSessionEntity.teacherCode
                this.gsmStatus = gpsSignalEntity.gsmStatus
                vel = gpsSignalEntity.gpsSpeed
                time = gpsSignalEntity.time / 1000 // old logic get time -> Todo refactor late
                this.gpsStatus = 1
            }
            // Todo refactor code send to rabbit
            val rabbitMq = Sender(gpsModel).sendData()
            Logger.i("handleRecoverUploadGpsSignal Rabbit sendStatus: $rabbitMq")
            return true
        } catch (ex: Exception) {
            Logger.e("handleRecoverUploadGpsSignal Error: ${ex.message}")
            return false
        }
    }

    private suspend fun handleRecoverUploadAuthen(
        riderSessionEntity: RiderSessionEntity,
        studentAuthenticationEntity: StudentAuthenticationEntity
    ): Boolean {
        try {
            val authenImageFile = File(studentAuthenticationEntity.authenImagePath)
            val resResult: ResponseResult<String?> = repository.uploadImageStartSession(
                file = authenImageFile,
                userCode = riderSessionEntity.studentCode,
                imei = riderSessionEntity.imei,
                searchScore = studentAuthenticationEntity.searchScore
            )
            Logger.i("handleRecoverUploadAuthen imageUploadUrl: ${resResult.data}")
            resResult.data?.also { imageUploadUrl ->
                val authModel = AuthModel(studentAuthenticationEntity.recognitionResult)
                authModel.apply {
                    sessionId = riderSessionEntity.sessionId
                    seri = riderSessionEntity.imei
                    lat = studentAuthenticationEntity.gpsLat
                    lng = studentAuthenticationEntity.gpsLong
                    userCode = riderSessionEntity.studentCode
                    dis = studentAuthenticationEntity.gpsDistance
                    teacherCode = riderSessionEntity.teacherCode
                    sent = 1 // can not understand logic
                    speed = studentAuthenticationEntity.gpsSpeed
                    filePathLocal = studentAuthenticationEntity.authenImagePath
                    filePath = imageUploadUrl
                    time =
                        (studentAuthenticationEntity.time / 1000L) + 25200 // old logic get time -> Todo refactor late
                }
                // Todo refactor code send to rabbit
                val rabbitMq = SenderAuth(authModel).sendAuth()
                Logger.i("handleRecoverUploadAuthen Rabbit sendStatus: $rabbitMq")
                return true
            }
            return false
        } catch (ex: Exception) {
            Logger.e("handleRecoverUploadAuthen Error: ${ex.message}")
            return false
        }
    }

    private suspend fun handleRecoverUploadFinishSession(riderSessionEntity: RiderSessionEntity): Boolean {
        try {
            Logger.i("handleRecoverUploadFinishSession riderSessionEntity: $riderSessionEntity")
            val logoutImageFile = File(riderSessionEntity.logoutImagePath!!)
            val imageUploadUrl: String? =
                uploadImage(logoutImageFile, riderSessionEntity.studentCode)
            Logger.i("handleRecoverUploadFinishSession imageUploadUrl: $imageUploadUrl")
            if (imageUploadUrl != null && riderSessionEntity.gpsLatEnd != null &&
                riderSessionEntity.gpsLongEnd != null && riderSessionEntity.sessionId != null
            ) {
                val resResult: ResponseResult<Any?> = repository.finishRiderSession(
                    imei = riderSessionEntity.imei,
                    studentCode = riderSessionEntity.studentCode,
                    gpsLat = riderSessionEntity.gpsLatEnd!!,
                    gpsLong = riderSessionEntity.gpsLongEnd!!,
                    sessionId = riderSessionEntity.sessionId!!,
                    logoutTime = riderSessionEntity.logoutTime!!,
                    logoutImageUrl = imageUploadUrl,
                    isSendTC = riderSessionEntity.isSendTC ?: false,
                    coverReSend = true
                )
                return !resResult.isError
            }
            return false
        } catch (ex: Exception) {
            Logger.e("handleRecoverUploadFinishSession Error: ${ex.message}")
            return false
        }
    }

    private suspend fun handleRecoverUploadSession(riderSessionEntity: RiderSessionEntity): String? {
        val deviceInfo = Utils.getDeviceInfo(context)
        try {
            val loginImageFile = File(riderSessionEntity.loginImagePath)
            val imageUploadUrl: String? =
                uploadImage(loginImageFile, riderSessionEntity.studentCode)
            Logger.i("handleRecoverUploadSession imageUploadUrl: $imageUploadUrl")
            if (imageUploadUrl != null) {
                sessionVerificationInfo.studentImageAuthUrl = imageUploadUrl
                val resResult: ResponseResult<StartRiderSessionResponse?> =
                    repository.startRiderSession(
                        seri = riderSessionEntity.imei,
                        studentCode = riderSessionEntity.studentCode,
                        gpsLat = riderSessionEntity.gpsLatStart,
                        gpsLong = riderSessionEntity.gpsLongStart,
                        loginType = riderSessionEntity.loginType,
                        // Todo keep old logic + 25200
                        loginTime = riderSessionEntity.loginTime + 25200,
                        loginImageUrl = imageUploadUrl,
                        teacherCode = riderSessionEntity.teacherCode,
                        appVersion = BuildConfig.VERSION_NAME,
                        imei = deviceInfo.imei1,
                        simSerialNumber = deviceInfo.simReal
                    )
                Logger.i("handleRecoverUploadSession resResult: ${resResult.data}")
                if (!resResult.isError) {
                    Logger.i("sessionId: ${resResult.data?.sessionId}")
                    return resResult.data?.sessionId
                }
            }
            return null
        } catch (ex: Exception) {
            Logger.e("handleRecoverUploadSession Error: ${ex.message}")
            return null
        }
    }
    // [DAT CER]

    init {
        device.getCurrentGPS()?.getLatestLocation()?.also { location ->
            Logger.i("location speed: ${location.speed} | latitude: ${location.latitude} | longitude: ${location.longitude}  | time: ${location.time}")
            sessionVerificationInfo.apply {
                setLastLocation(location)
//                lat = location.latitude
//                long = location.longitude
            }
        }

        // [DAT CER]: only use for get DAT certification
        device.getCurrentNetworkConnection()
            ?.setNetworkConnectionEventListener(networkConnectionEvent)
        // [DAT CER]
    }

    private fun resetDataSession() {
        sessionVerificationInfo = SessionVerificationInfo()
        dropStudentOutSession()
        dropTeachOutWorking()
        removeCurrentSession()
    }

    fun startGPSEventListener(gpsEvent: GPSEvent) {
        device.getCurrentGPS()?.addGPSEventListener(gpsEvent)
    }

    fun stopGPSEventListener(gpsEvent: GPSEvent) {
        device.getCurrentGPS()?.removeGPSEventListener(gpsEvent)
    }

    fun checkGPSAvailable(activity: Activity): Boolean {
        return device.getCurrentGPS()?.checkGPSAvailable(activity) ?: false
    }

    fun pushAuthenticateData(callback: () -> Unit) {
        Logger.d("pushAuthenticateData sessionVerificationInfo.verifyResult: ${sessionVerificationInfo.verifyResult}")
        Logger.i(
            "inProgressSession: $inProgressSession | " +
                    "sessionVerificationInfo.lat: ${sessionVerificationInfo.lat} | " +
                    "sessionVerificationInfo.long: ${sessionVerificationInfo.long}"
        )
        LogRecorder.i("pushAuthenticateData:  ",
            "inProgressSession: $inProgressSession | " +
                "sessionVerificationInfo.lat: ${sessionVerificationInfo.lat} | " +
                "sessionVerificationInfo.long: ${sessionVerificationInfo.long}" +
                    "studentImageAuthPath: ${sessionVerificationInfo.studentImageAuthPath}"
        )
        CoroutineScope(Dispatchers.IO).launch(
            CoroutineExceptionHandler { _, ex ->
                Logger.e("pushAuthenticateData: Found an exception exception: ${ex.message}")
            }
        ) {
            if (inProgressSession != null &&
                sessionVerificationInfo.lat != 0.0 &&
                sessionVerificationInfo.long != 0.0 &&
                sessionVerificationInfo.studentImageAuthPath != null
            ) {
                val authModel = AuthModel(sessionVerificationInfo.verifyResult.code)
                authModel.apply {
                    sessionId = inProgressSession!!.id
                    seri = getImeiDevice(context)
                    lat = sessionVerificationInfo.lat
                    lng = sessionVerificationInfo.long
                    userCode = studentAuthInfo!!.userCode
                    dis = inProgressSession!!.totalDis
//                    dis = 999999f
                    teacherCode = teacherAuthInfo!!.userCode
                    sent = 0 // can not understand logic
                    speed = sessionVerificationInfo.getSpeed()
                    filePathLocal = sessionVerificationInfo.studentImageAuthPath
                    filePath = sessionVerificationInfo.studentImageAuthUrl
//                    time = Utils.getTimeStamp()
                    time = (
                        (
                            sessionVerificationInfo.timeAuth
                                ?: Calendar.getInstance().timeInMillis
                            ) / 1000L
                        ) + 25200 // old logic get time -> Todo refactor late
                }
                LogRecorder.i("Thông tin dữ liệu xác thực đẩy rabbit: ", authModel.toString())
                // Todo refactor code send to rabbit
                val rabbitMq = SenderAuth(authModel).sendAuth()
                LogRecorder.i("pushAuthenticateData Rabbit sendStatus: ", "$rabbitMq")
                Logger.i("pushAuthenticateData Rabbit sendStatus: $rabbitMq")
                if (rabbitMq) {
                    inProgressSession!!.totalAuthen ++
                    authModel.sent = 1
                    LogRecorder.i("Push Authenticate data thành công","")
                } else {
                    authModel.sent = 0
                    LogRecorder.e("Push Authenticate data lỗi","")
                }
                handleSaveLocalAuthentication(authModel.sent != 1, callback = callback)
            }
        }
    }

    fun handleSaveLocalAuthentication(isSaveLocal: Boolean, callback: () -> Unit) {
        CoroutineScope(Dispatchers.IO).launch(
            CoroutineExceptionHandler { _, ex ->
                Logger.e("handleSaveLocalAuthentication: Found an exception exception: ${ex.message}")
            }
        ) {
            if (inProgressSession != null &&
                sessionVerificationInfo.lat != 0.0 &&
                sessionVerificationInfo.long != 0.0 &&
                sessionVerificationInfo.studentImageAuthPath != null
            ) {
                localRiderSession?.also {
                    val studentAuthentication = StudentAuthenticationEntity(
                        localSessionId = it.id,
                        gpsLat = sessionVerificationInfo.lat,
                        gpsLong = sessionVerificationInfo.long,
                        gpsDistance = inProgressSession!!.totalDis,
                        gpsSpeed = sessionVerificationInfo.getSpeed(),
                        authenImagePath = sessionVerificationInfo.studentImageAuthPath!!,
                        time = sessionVerificationInfo.timeAuth
                            ?: Calendar.getInstance().timeInMillis,
                        recognitionResult = sessionVerificationInfo.verifyResult.code,
                        searchScore = sessionVerificationInfo.searchScore ?: 0f,
                        state = if (isSaveLocal) AuthenUploadState.SAVE_LOCAL.code
                        else AuthenUploadState.SENT_ONLINE.code
                    )
                    Logger.i("pushAuthenticateData studentAuthentication: $studentAuthentication")
                    repository.insertNewStudentAuthentication(studentAuthentication)
                    countGPSDataUpload()
                    countAuthDataUpload()
                    LogRecorder.i("Thông tin đẩy dữ liệu ảnh xác thực:", " ${inProgressSession?.totalAuthDataUploadSuccess}/${inProgressSession?.totalAuthDataUpload}(${1 + floor(inProgressSession?.totalTime ?: 0.0).toInt()/300})")
                    withContext(Dispatchers.Main){
                        callback()
                    }
                }
            }
        }
    }

    fun pushGPSData(
        gpsStatus: Int,
        gsmStatus: Int,
        callback: (action: RiderSessionAction, data: Any?) -> Unit
    ) {
        Logger.d("pushGPSData sessionVerificationInfo.verifyResult: ${sessionVerificationInfo.verifyResult}")
        LogRecorder.i("pushGPSData:  ",
            "inProgressSession: $inProgressSession | " +
                    "sessionVerificationInfo.lat: ${sessionVerificationInfo.lat} | " +
                    "sessionVerificationInfo.long: ${sessionVerificationInfo.long}" +
                    "studentImageAuthPath: ${sessionVerificationInfo.studentImageAuthPath}"
        )

//        Logger.i(
//            "inProgressSession: $inProgressSession | " +
//                    "sessionVerificationInfo.lat: ${sessionVerificationInfo.lat} | " +
//                    "sessionVerificationInfo.long: ${sessionVerificationInfo.long}"
//        )
        CoroutineScope(Dispatchers.IO).launch(
            CoroutineExceptionHandler { _, ex ->
                Logger.e("pushGPSData: Found an exception exception: ${ex.message}")
            }
        ) {
            if (inProgressSession != null &&
                sessionVerificationInfo.lat != 0.0 &&
                sessionVerificationInfo.long != 0.0
            ) {
                val gpsModel = GpsModel()
                val timeGps = Calendar.getInstance().timeInMillis
                gpsModel.apply {
                    sessionId = inProgressSession!!.id
                    seri = getImeiDevice(context)
                    lat = sessionVerificationInfo.lat
                    lng = sessionVerificationInfo.long
                    userCode = studentAuthInfo!!.userCode
                    dis = inProgressSession!!.totalDis
                    teacherCode = teacherAuthInfo!!.userCode
                    this.gsmStatus = gsmStatus
                    vel = sessionVerificationInfo.getSpeed()
                    time = timeGps / 1000
                    this.gpsStatus = 1
                }
                LogRecorder.i("Thông tin GPS đẩy rabbit: ", gpsModel.toString())

                // Todo refactor code send to rabbit
                val rabbitMq = Sender(gpsModel).sendData()
                LogRecorder.i("pushGPSData Rabbit sendStatus:"," $rabbitMq")
                Logger.i("pushGPSData Rabbit sendStatus: $rabbitMq")
                if (rabbitMq) {
                    gpsModel.sent = 1
                    inProgressSession!!.totalGps++
                } else {
                    gpsModel.sent = 0
                }
                localRiderSession?.also {
                    val gpsSignalEntity = GPSSignalEntity(
                        localSessionId = it.id,
                        gpsLat = sessionVerificationInfo.lat,
                        gpsLong = sessionVerificationInfo.long,
                        gpsDistance = inProgressSession!!.totalDis,
                        gpsSpeed = sessionVerificationInfo.getSpeed(),
                        gpsStatus = gpsStatus,
                        gsmStatus = gsmStatus,
                        time = timeGps,
                        state = if (gpsModel.sent == 1) GPSUploadState.SENT_ONLINE.code
                        else GPSUploadState.SAVE_LOCAL.code
                    )
                    Logger.i("pushGPSData gpsSignalEntity: $gpsSignalEntity")
                    repository.insertNewGPSSignal(gpsSignalEntity)
                    countGPSDataUpload()
                    countAuthDataUpload()
                    if (gpsModel.sent == 1) {
                        inProgressSession!!.lastUploadGPSTime = Utils.getRealTimeStamp() / 1000
                        LogRecorder.i("Đẩy GPS lên server thành công", gpsSignalEntity.toString())
                    } else {
                        LogRecorder.e("Đẩy GPS lên server lỗi", gpsSignalEntity.toString())
                    }
                    LogRecorder.i("Thông tin đẩy dữ liệu GPS:", " ${inProgressSession?.totalGPSUploadSuccess}/${inProgressSession?.totalGPSUpload}(${1 + floor(inProgressSession?.totalTime ?: 0.0).toInt()/10})")
                }

                // call fetch session from server
                fetchCurrentSession(
                    sessionId = inProgressSession!!.id,
                    callback = callback
                )
            }
        }
    }

    fun finishRiderSession(
        notSendTC: Boolean,
        studentLogoutImage: File,
        callback: (action: RiderSessionAction, data: Any?) -> Unit
    ) {
        Logger.d("finishRiderSession")
        CoroutineScope(Dispatchers.Default).launch(
            CoroutineExceptionHandler { _, ex ->
                Logger.e("finishRiderSession: Found an exception exception: ${ex.message}")
                CoroutineScope(Dispatchers.Main).launch {
                    callback(
                        RiderSessionAction.FINISH_RIDER_SESSION_FAIL,
                        "Lỗi xử lý!!\n\nXin hãy thử lại"
                    )
                }
            }
        ) {
            checkAndReUploadData()
            if (sessionVerificationInfo.lat == 0.0 || sessionVerificationInfo.long == 0.0) {
                LogRecorder.e("Kết thúc phiên không thành công", "Không định vị được vị trí để kết thúc phiên học!\nHãy kiểm tra kết nối GPS!")
                CoroutineScope(Dispatchers.Main).launch {
                    callback(
                        RiderSessionAction.FINISH_RIDER_SESSION_FAIL_BY_LOCATION,
                        "Không định vị được vị trí để kết thúc phiên học!\nHãy kiểm tra kết nối GPS!"
                    )
                }
            } else {
                // check internet available
                if (device.getCurrentNetworkConnection()?.checkConnectionAvailable() == false) {
                    // update finish session info
                    localRiderSession?.also {
                        val sessionState =
                            if (it.state == SessionState.START_OFFLINE.code) SessionState.START_FINISH_OFFLINE.code
                            else SessionState.START_ONLINE_FINISH_OFFLINE.code // else is case start online
                        Logger.i("finishRiderSession it.state: ${it.state} | sessionState: $sessionState")
                        it.updateEndSessionInfo(
                            gpsLatEnd = sessionVerificationInfo.lat,
                            gpsLongEnd = sessionVerificationInfo.long,
                            logoutTime = Calendar.getInstance().timeInMillis.toDouble(), // don't use logoutTime for export file report
                            logoutImagePath = studentLogoutImage.path,
                            totalTime = inProgressSession!!.totalTime,
                            totalDistance = inProgressSession!!.totalDis,
                            isSendTC = !notSendTC,
                            state = sessionState
                        )
                        Logger.i("finishRiderSession localRiderSession: $localRiderSession")
                        val logoutTime = Utils.getTimeStamp().toDouble()
                        repository.updateEndSessionInfo(
                            id = it.id,
                            gpsLatEnd = it.gpsLatEnd!!,
                            gpsLongEnd = it.gpsLongEnd!!,
                            logoutTime = logoutTime,
                            logoutImagePath = studentLogoutImage.path,
                            totalTime = inProgressSession!!.totalTime,
                            totalDistance = inProgressSession!!.totalDis,
                            isSendTC = !notSendTC,
                            state = sessionState
                        )
                        try {
                            repository.exportSessionReport(it,null)
                            LogRecorder.i("Kết thúc phiên thành công - OFFLINE", localRiderSession.toString())
                        } catch (e: Exception) {
                            LogRecorder.e("finish session error: ", e.message)
                        } finally {
                            try {
                                LogRecorder.saveLog(false)
                                pushLogFile()
                            } catch (e: Exception) {
                                LogRecorder.e("push log file error: ", e.message)
                            }
                        }
                        resetDataSession()
                        CoroutineScope(Dispatchers.Main).launch {
                            callback(RiderSessionAction.FINISH_RIDER_SESSION_SUCCESS_OFFLINE, null)
                        }
                    }
                }
                else {
                    val imageUploadUrl: String? =
                        uploadImage(studentLogoutImage, studentAuthInfo!!.userCode)
                    if (imageUploadUrl == null) {
                        CoroutineScope(Dispatchers.Main).launch {
                            LogRecorder.e("Kết thúc phiên không thành công", "Upload ảnh nhận diện lỗi")
                            callback(
                                RiderSessionAction.FINISH_RIDER_SESSION_FAIL,
                                "Upload ảnh nhận diện lỗi"
                            )
                        }
                    } else {
                        sessionVerificationInfo.studentImageLogoutUrl = imageUploadUrl
                        val logoutTime = Calendar.getInstance().timeInMillis.toDouble()
                        Utils.getTimeStamp().toDouble()
                        val resResult: ResponseResult<Any?> = repository.finishRiderSession(
                            imei = getImeiDevice(context),
                            studentCode = studentAuthInfo!!.userCode,
                            gpsLat = sessionVerificationInfo.lat,
                            gpsLong = sessionVerificationInfo.long,
                            sessionId = inProgressSession!!.id,
                            logoutTime = (logoutTime / 1000L) + 25200, // Todo keep old logic + 25200
                            logoutImageUrl = sessionVerificationInfo.studentImageLogoutUrl!!,
                            isSendTC = !notSendTC
                        )
                        if (resResult.isError) {
                            Logger.e("Response Error: ${resResult.errorMessage}")
                            LogRecorder.e("Kết thúc phiên không thành công", resResult.errorMessage)
                            CoroutineScope(Dispatchers.Main).launch {
                                if (resResult.errorCode == PUSH_SESSION_TO_TC_FAIL) {
                                    callback(
                                        RiderSessionAction.PUSH_SESSION_TO_TC_FAIL,
                                        resResult.errorMessage
                                    )
                                } else {
                                    callback(
                                        RiderSessionAction.FINISH_RIDER_SESSION_FAIL,
                                        resResult.errorMessage
                                    )
                                }
                            }
                        } else {
                            // update finish session info
                            localRiderSession?.also {
                                it.updateEndSessionInfo(
                                    gpsLatEnd = sessionVerificationInfo.lat,
                                    gpsLongEnd = sessionVerificationInfo.long,
                                    logoutTime = logoutTime, // don't use logoutTime for export file report
                                    logoutImagePath = studentLogoutImage.path,
                                    totalTime = inProgressSession!!.totalTime,
                                    totalDistance = inProgressSession!!.totalDis,
                                    isSendTC = !notSendTC,
                                    state = SessionState.START_ONLINE_FINISH_ONLINE.code
                                )
                                Logger.i("finishRiderSession localRiderSession: $localRiderSession")
                                repository.updateEndSessionInfo(
                                    id = it.id,
                                    gpsLatEnd = it.gpsLatEnd!!,
                                    gpsLongEnd = it.gpsLongEnd!!,
                                    logoutTime = (logoutTime / 1000L) + 25200, // Todo keep old logic + 25200,
                                    logoutImagePath = studentLogoutImage.path,
                                    totalTime = inProgressSession!!.totalTime,
                                    totalDistance = inProgressSession!!.totalDis,
                                    isSendTC = !notSendTC,
                                    state = SessionState.START_ONLINE_FINISH_ONLINE.code
                                )
                                // hot update study result of this session
                                repository.updateStudyResult(
                                    userId = studentAuthInfo!!.userId,
                                    totalTimeStudied = inProgressSession!!.totalTime,
                                    totalDistanceRode = inProgressSession!!.totalDis
                                )
                                try {
                                    val channel = Channel<Any>()
                                    repository.exportSessionReport(it, channel)
                                    channel.receive()
                                    pushReportFile(riderSessionEntity = it)
                                    LogRecorder.i("Kết thúc phiên thành công - ONLINE", localRiderSession.toString())
                                } catch (e: Exception) {
                                    LogRecorder.e("finish session error: ", e.message)
                                } finally {
                                    try {
                                        LogRecorder.saveLog(false)
                                        pushLogFile()
                                    } catch (e: Exception) {
                                        LogRecorder.e("push log file error: ", e.message)
                                    }
                                }
                                resetDataSession()
                                CoroutineScope(Dispatchers.Main).launch {
                                    if (resResult.errorCode == SUCCESS_WITH_ERROR) {
                                        callback(
                                            RiderSessionAction.FINISH_RIDER_SESSION_SUCCESS_WITH_ERROR,
                                            resResult.errorMessage
                                        )
                                    } else {
                                        callback(
                                            RiderSessionAction.FINISH_RIDER_SESSION_SUCCESS,
                                            it.sessionId
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    suspend fun adminLogoutHandler(studentLogoutImage: File){
        val logoutTime = Calendar.getInstance().timeInMillis.toDouble()
        // update finish session info
        localRiderSession?.also {
            it.updateEndSessionInfo(
                gpsLatEnd = sessionVerificationInfo.lat,
                gpsLongEnd = sessionVerificationInfo.long,
                logoutTime = logoutTime,
                logoutImagePath = studentLogoutImage.path,
                totalTime = inProgressSession!!.totalTime,
                totalDistance = inProgressSession!!.totalDis,
                isSendTC = true,
                state = SessionState.START_ONLINE_FINISH_ONLINE.code
            )
            Logger.i("adminLogoutHandler localRiderSession: $localRiderSession")
            // hot update study result of this session
            repository.updateEndSessionInfo(
                id = it.id,
                gpsLatEnd = it.gpsLatEnd!!,
                gpsLongEnd = it.gpsLongEnd!!,
                logoutTime = (logoutTime / 1000L) + 25200, // Todo keep old logic + 25200,
                logoutImagePath = studentLogoutImage.path,
                totalTime = inProgressSession!!.totalTime,
                totalDistance = inProgressSession!!.totalDis,
                isSendTC = false,
                state = SessionState.START_ONLINE_FINISH_ONLINE.code
            )
            // hot update study result of this session
            repository.updateStudyResult(
                userId = studentAuthInfo!!.userId,
                totalTimeStudied = inProgressSession!!.totalTime,
                totalDistanceRode = inProgressSession!!.totalDis
            )
            try {
                val channel = Channel<Any>()
                repository.exportSessionReport(it, channel)
                channel.receive()
                pushReportFile(riderSessionEntity = it)
                LogRecorder.i("Phiên học được đăng xuất bởi quản trị viên", localRiderSession.toString())
            } catch (e: Exception) {
                LogRecorder.e("finish session error:", e.message)
            } finally {
                try {
                    LogRecorder.saveLog(false)
                    pushLogFile()
                } catch (e: Exception) {
                    LogRecorder.e("push log file error: ", e.message)
                }
            }
            resetDataSession()
        }
    }
    fun startRiderSession(
        userCode: String,
        loginType: Int,
        plateSlug: String,
        studentLoginImage: File,
        callback: (action: RiderSessionAction, data: Any?) -> Unit
    ) {
        Logger.d("startRiderSession")
        CoroutineScope(Dispatchers.Default).launch(
            CoroutineExceptionHandler { _, ex ->
                Logger.e("startRiderSession: Found an exception exception: ${ex.message}")
                CoroutineScope(Dispatchers.Main).launch {
                    callback(
                        RiderSessionAction.START_RIDER_SESSION_FAIL,
                        "Lỗi xử lý!!\n\nXin hãy thử lại"
                    )
                }
            }
        ) {
            // check internet available
            if (device.getCurrentNetworkConnection()?.checkConnectionAvailable() == false) {
                if (sessionVerificationInfo.lat == 0.0 || sessionVerificationInfo.long == 0.0) {
                    CoroutineScope(Dispatchers.Main).launch {
                        LogRecorder.e("Mở phiên không thành công", "Không định vị được vị trí bắt đầu phiên học!\nHãy kiểm tra kết nối GPS!")
                        callback(
                            RiderSessionAction.START_RIDER_SESSION_FAIL_BY_LOCATION,
                            "Không định vị được vị trí bắt đầu phiên học!\nHãy kiểm tra kết nối GPS!"
                        )
                    }
                } else {
                    localRiderSession = RiderSessionEntity(
                        sessionId = null,
                        imei = getImeiDevice(context),
                        studentCode = studentAuthInfo!!.userCode,
                        studentName = studentAuthInfo!!.fullName,
                        gpsLatStart = sessionVerificationInfo.lat,
                        gpsLongStart = sessionVerificationInfo.long,
                        loginType = loginType,
                        plateSlug = repository.getPlateSlug() ?: "Unknown",
//                        loginTime = Utils.getTimeStamp().toDouble(),
                        loginTime = (Utils.getRealTimeStamp() / 1000).toDouble(),
                        loginImagePath = studentLoginImage.path,
                        teacherCode = teacherAuthInfo!!.userCode,
                        state = SessionState.START_OFFLINE.code
                    )
                    localRiderSession = repository.insertNewRiderSession(localRiderSession!!)
                    Logger.i("localRiderSession: $localRiderSession")
//                    repository.saveSessionCode(sessionId)
                    localRiderSession?.also {
                        inProgressSession = InProgressSession(
                            id = it.id.toString(), // temporary set session id is local session id
                            studentCode = it.studentCode,
                            plateSlug = it.plateSlug,
                            seri = it.imei,
                            teacherCode = it.teacherCode,
                            teacherName = teacherAuthInfo!!.fullName,
                            totalTime = it.totalTime,
                            startTime = it.loginTime.toLong(),
                            totalDis = it.totalDistance,
                            totalVerifyCounter = 0,
                            successVerifyCounter = 0
                        )
                        Logger.i("inProgressSession: $inProgressSession")
                        CoroutineScope(Dispatchers.Main).launch {
                            callback(
                                RiderSessionAction.START_RIDER_SESSION_SUCCESS,
                                it.id.toString()
                            )
                        }
                        LogRecorder.i("Mở phiên thành công - OFFLINE", localRiderSession.toString())
                        LogRecorder.saveLog(stopWriteLog = false)
                    }
                }
            }
            else {
                val imageUploadUrl: String? = uploadImage(studentLoginImage, userCode)
                if (imageUploadUrl == null) {
                    CoroutineScope(Dispatchers.Main).launch {
                        LogRecorder.e("Mở phiên không thành công", "Upload ảnh nhận diện lỗi!")
                        callback(
                            RiderSessionAction.START_RIDER_SESSION_FAIL,
                            "Upload ảnh nhận diện lỗi!"
                        )
                    }
                } else if (sessionVerificationInfo.lat == 0.0 || sessionVerificationInfo.long == 0.0) {
                    CoroutineScope(Dispatchers.Main).launch {
                        LogRecorder.e("Mở phiên không thành công", "Không định vị được vị trí bắt đầu phiên học!\nHãy kiểm tra kết nối GPS!")
                        callback(
                            RiderSessionAction.START_RIDER_SESSION_FAIL_BY_LOCATION,
                            "Không định vị được vị trí bắt đầu phiên học!\nHãy kiểm tra kết nối GPS!"
                        )
                    }
                } else {
                    val connManager =
                            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                    val mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
                    val mMobile = connManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE)
                    val networkStatus: Byte? =  when {
                        mWifi?.isConnected == true -> 0
                        mMobile?.isConnected == true -> 1
                        else -> null
                    }

                    sessionVerificationInfo.studentImageAuthUrl = imageUploadUrl
                    val loginTime = Utils.getRealTimeStamp().toDouble()
                    val  deviceInfo = Utils.getDeviceInfo(context)
                    val resResult: ResponseResult<StartRiderSessionResponse?> =
                        repository.startRiderSession(
                            seri = getImeiDevice(context),
                            studentCode = userCode,
                            gpsLat = sessionVerificationInfo.lat,
                            gpsLong = sessionVerificationInfo.long,
                            loginType = loginType,
                            loginTime = (loginTime / 1000L) + 25200, // Todo keep old logic + 25200
                            loginImageUrl = sessionVerificationInfo.studentImageAuthUrl!!,
                            teacherCode = teacherAuthInfo!!.userCode,
                            appVersion = BuildConfig.VERSION_NAME,
                            imei = deviceInfo.imei1,
                            simSerialNumber = deviceInfo.simReal,
                            networkStatus = networkStatus
                        )
                    if (resResult.isError) {
                        Logger.e("Response Error: ${resResult.errorMessage}")
                        CoroutineScope(Dispatchers.Main).launch {
                            callback(
                                RiderSessionAction.START_RIDER_SESSION_FAIL,
                                resResult.errorMessage
                            )
                        }
                    } else {
                        val startRiderSession: StartRiderSessionResponse? = resResult.data
                        Logger.i("startRiderSession: ${startRiderSession?.sessionId}")
                        if (startRiderSession != null) {
                            repository.saveSessionCode(startRiderSession.sessionId)
                            inProgressSession = InProgressSession(
                                id = startRiderSession.sessionId,
                                studentCode = studentAuthInfo!!.userCode,
                                plateSlug = plateSlug,
                                seri = getImeiDevice(context),
                                teacherCode = teacherAuthInfo!!.userCode,
                                teacherName = teacherAuthInfo!!.fullName,
                                totalTime = 0.0,
                                totalDis = 0f,
                                startTime = loginTime.toLong() / 1000L,
                                totalVerifyCounter = 0,
                                successVerifyCounter = 0,
                                nightTime = startRiderSession.nightTime,
                                fromNightTime = startRiderSession.fromNightTime,
                                toNightTime = startRiderSession.toNightTime,
                                automaticTransmissionTime = startRiderSession.automaticTransmissionTime,
                                totalAutomaticTransmissionTime = startRiderSession.totalAutomaticTransmissionTime,
                                timeIn24H = startRiderSession.timeIn24H
                            )
                            Logger.i("inProgressSession: $inProgressSession")
                            // insert new session to local db
                            localRiderSession = inProgressSession!!.convertToModelEntity(
                                imei = getImeiDevice(context),
                                studentName = studentAuthInfo!!.fullName,
                                gpsLatStart = sessionVerificationInfo.lat,
                                gpsLongStart = sessionVerificationInfo.long,
                                loginType = loginType,
                                loginImagePath = studentLoginImage.path,
                                state = SessionState.START_ONLINE
                            )
                            localRiderSession =
                                repository.insertNewRiderSession(localRiderSession!!)
                            Logger.i("localRiderSession: $localRiderSession")
                            CoroutineScope(Dispatchers.Main).launch {
                                callback(
                                    RiderSessionAction.START_RIDER_SESSION_SUCCESS,
                                    startRiderSession.sessionId
                                )
                            }
                            LogRecorder.i("Mở phiên thành công - ONLINE", localRiderSession.toString())
                            LogRecorder.saveLog(stopWriteLog = false)
                        } else {
                            CoroutineScope(Dispatchers.Main).launch {
                                callback(
                                    RiderSessionAction.START_RIDER_SESSION_FAIL,
                                    "Session ID is null!"
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    fun continueInProgressSession(inProgressSession: InProgressSession) {
        if (inProgressSession.startTime == 0L && inProgressSession.loginDate != "") {
            inProgressSession.startTime =
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).parse(
                    inProgressSession.loginDate
                )?.time?.div(1000L) ?: (Utils.getRealTimeStamp() / 1000L)
        }

        this.inProgressSession = inProgressSession

        Logger.i("continueInProgressSession inProgressSession: $inProgressSession")
        // Todo must BE return loginType and loginImagePath
        val sessionState =
            if (localRiderSession?.state != null) SessionState.findByCode(localRiderSession!!.state) else SessionState.START_ONLINE
        localRiderSession = this.inProgressSession!!.convertToModelEntity(
            imei = getImeiDevice(context),
            studentName = studentAuthInfo!!.fullName,
            gpsLatStart = sessionVerificationInfo.lat,
            gpsLongStart = sessionVerificationInfo.long,
            loginType = localRiderSession?.loginType ?: LoginType.OTHER.code, // Todo
            loginImagePath = localRiderSession?.loginImagePath ?: "", // Todo
            state = sessionState
        )

        // check insert if necessary session info if it don't have localID
        localRiderSession = if (inProgressSession.localRiderSession == null) {
            repository.insertIfNecessaryRiderSession(localRiderSession!!)
        } else {
            inProgressSession.localRiderSession
        }
        CoroutineScope(Dispatchers.IO).launch {
            countAuthDataUpload()
            countGPSDataUpload()
        }
        // create log file if it not exist
            LogRecorder.createLogFile(
                sessionId = inProgressSession.id,
                studentCode = inProgressSession.studentCode,
                versionApp = BuildConfig.VERSION_NAME,
                startTime = getSessionStartTime(),
                loginStatus = localRiderSession?.loginStatus ?: "",
                timeLogin = getSessionStartTime()
                    ?.let { Utils.convertTimeStampToTime(it) }
            )
        Logger.i("localRiderSession: $localRiderSession")
    }
    fun getInternetStatus(): String{
        val connManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
        var mMobile = false
        val networks = connManager.allNetworks
        for (network in networks) {
            // > android M
            val capabilities = connManager.getNetworkCapabilities(network)
            if (capabilities != null && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                mMobile = true
            }
        }
        return if (mWifi?.isConnected == true) "WIFI" else if (mMobile) "4G" else "OFF"
    }
    fun getSessionStartTime(): Long? {
        return if (inProgressSession?.startTime != 0L) {
            inProgressSession?.startTime?.times(1000)
        } else {
            inProgressSession?.loginDate?.let {
                try {
                    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SS", Locale.getDefault()).parse(
                        it
                    )?.time
                } catch (e: Exception) {
                    Logger.e("Error parsing loginDate: ${e.message}")
                    null
                }

            }
        }
    }

    fun uploadImageInSession(
        imageFile: File,
        userCode: String,
        tryTime: Int = 1,
        callback: (action: RiderSessionAction, data: Any?) -> Unit
    ) {
        Logger.d("uploadImageStartSession imageFile: $imageFile")
        CoroutineScope(Dispatchers.IO).launch(
            CoroutineExceptionHandler { _, ex ->
                Logger.e("uploadImageStartSession: Found an exception exception: ${ex.message}")
                CoroutineScope(Dispatchers.Main).launch {
                    callback(
                        RiderSessionAction.UPLOAD_IMAGE_CAPTURE_FAIL,
                        "Lỗi xử lý!!\n\nXin hãy thử lại"
                    )
                }
            }
        ) {
            val resResult: ResponseResult<String?> = repository.uploadImageStartSession(
                file = imageFile,
                userCode = userCode,
                getImeiDevice(context),
                searchScore = sessionVerificationInfo.searchScore
            )
            if (resResult.isError) {
                Logger.e("Response Error: ${resResult.errorMessage}")
                // try upload image 3 times
                if (tryTime < 3) {
                    uploadImageInSession(
                        imageFile = imageFile,
                        userCode = userCode,
                        tryTime = tryTime + 1,
                        callback = callback
                    )
                } else {
                    LogRecorder.e("Tải ảnh xác thực lỗi", resResult.errorMessage)
                    CoroutineScope(Dispatchers.Main).launch {
                        callback(
                            RiderSessionAction.UPLOAD_IMAGE_CAPTURE_FAIL,
                            resResult.errorMessage
                        )
                    }
                }
            } else {
                val imageUrl: String? = resResult.data
                Logger.i("imageUrl: $imageUrl")
                LogRecorder.i("Tải ảnh xác thực thành công", imageUrl)
                CoroutineScope(Dispatchers.Main).launch {
                    callback(RiderSessionAction.UPLOAD_IMAGE_CAPTURE_SUCCESS, imageUrl)
                }
            }
        }
    }

    private suspend fun uploadImage(imageFile: File, userCode: String): String? {
        val resResult: ResponseResult<String?> = repository.uploadImageStartSession(
            file = imageFile,
            userCode = userCode,
            getImeiDevice(context),
            searchScore = sessionVerificationInfo.searchScore
        )
        return if (resResult.isError) {
            Logger.e("Response Error: ${resResult.errorMessage}")
            null
        } else {
            val imageUrl: String? = resResult.data
            Logger.i("imageUrl: $imageUrl")
            imageUrl
        }
    }

    fun updateLoginStatus(
        loginStatus: String,
    ) {
        CoroutineScope(Dispatchers.Default).launch(
            CoroutineExceptionHandler { _, ex ->
                Logger.e("updateLoginStatus: Found an exception: ${ex.message}")
            }
        ) {
            localRiderSession?.apply {
                repository.updateLoginStatus(id = this.id, loginStatus = loginStatus)
            }
        }

    }

    fun getInProgressSessionByStudent(
        userCode: String,
        callback: (action: RiderSessionAction, data: Any?) -> Unit
    ) {
        Logger.d("getInProgressSessionByStudent userCode: $userCode")
        CoroutineScope(Dispatchers.Default).launch(
            CoroutineExceptionHandler { _, ex ->
                Logger.e("getInProgressSessionByStudent: Found an exception exception: ${ex.message}")
                CoroutineScope(Dispatchers.Main).launch {
                    callback(
                        RiderSessionAction.GET_SESSION_IN_PROGRESS_BY_STUDENT_FAIL,
                        "Lỗi xử lý!!\n\nXin hãy thử lại"
                    )
                }
            }
        ) {
            // check internet available
            if (device.getCurrentNetworkConnection()?.checkConnectionAvailable() == false) {
                val lastSession: RiderSessionEntity? = repository.getLastRiderSession()
                lastSession?.also {
                    if (it.state == SessionState.START_ONLINE.code ||
                        it.state == SessionState.START_OFFLINE.code
                    ) {
                        if (it.studentCode != userCode) {
                            CoroutineScope(Dispatchers.Main).launch {
                                callback(
                                    RiderSessionAction.LOCAL_STUDENT_IN_PROGRESSS_SESSION_NOT_MATCH,
                                    "Student code not match with in-progress session in local!"
                                )
                            }
                        } else if (it.teacherCode != teacherAuthInfo?.userCode) {
                            CoroutineScope(Dispatchers.Main).launch {
                                callback(
                                    RiderSessionAction.STUDENT_AND_TEACHER_NOT_MATCH,
                                    "Teacher code not match with in-progress session in local!"
                                )
                            }
                        } else {
                            val listAuthen = repository.getListAuthenDataByLocalSessionId(it.id)
                            Logger.i("getInProgressSessionByStudent listAuthen: ${listAuthen.size}")
                            Logger.i("getInProgressSessionByStudent RiderSessionEntity: $it")
                            val inProgressSession: InProgressSession = it.convertToModel(
                                teacherName = teacherAuthInfo?.fullName ?: "Unknown",
                                totalVerifyCounter = listAuthen.size,
                                successVerifyCounter = listAuthen.filter { studentAuth ->
                                    studentAuth.recognitionResult == VerifyResult.VERIFY_SUCCESS.code
                                }.size
                            )
                            Logger.i("getInProgressSessionByStudent inProgressSession: $inProgressSession")
                            CoroutineScope(Dispatchers.Main).launch {
                                callback(
                                    RiderSessionAction.GET_SESSION_IN_PROGRESS_BY_STUDENT_SUCCESS,
                                    inProgressSession
                                )
                            }
                        }
                    } else {
                        CoroutineScope(Dispatchers.Main).launch {
                            callback(
                                RiderSessionAction.GET_SESSION_IN_PROGRESS_BY_STUDENT_SUCCESS,
                                null
                            )
                        }
                    }
                } ?: also {
                    CoroutineScope(Dispatchers.Main).launch {
                        callback(
                            RiderSessionAction.GET_SESSION_IN_PROGRESS_BY_STUDENT_SUCCESS,
                            null
                        )
                    }
                }
            }
            else {
                val resResult: ResponseResult<StudentSessionInProgressResponse?> =
                    repository.getInProgressSessionByUser(userCode)
                if (resResult.isError) {
                    Logger.e("Error: ${resResult.errorMessage}")
                    CoroutineScope(Dispatchers.Main).launch {
                        callback(
                            RiderSessionAction.GET_SESSION_IN_PROGRESS_BY_STUDENT_FAIL,
                            resResult.errorMessage
                        )
                    }
                } else {
                    CoroutineScope(Dispatchers.Main).launch {
                        if (resResult.data?.inProgressSession != null) {
                            val serialInSession = resResult.data.inProgressSession.seri
                            val teacherInSession = resResult.data.inProgressSession.teacherCode
                            Logger.i("getInProgressSessionByStudent serialInSession: $serialInSession | teacherInSession: $teacherInSession")
                            if (serialInSession != getImeiDevice(context)) {
                                callback(
                                    RiderSessionAction.STUDENT_HAS_SESSION_IN_OTHER_DEVICE,
                                    resResult.errorMessage
                                )
                            } else if (teacherInSession != null && teacherInSession != teacherAuthInfo?.userCode) {
                                callback(
                                    RiderSessionAction.STUDENT_AND_TEACHER_NOT_MATCH,
                                    resResult.errorMessage
                                )
                            } else {
                                callback(
                                    RiderSessionAction.GET_SESSION_IN_PROGRESS_BY_STUDENT_SUCCESS,
                                    resResult.data.inProgressSession
                                )
                            }
                        } else { // if data session is null -> return for create new session
                            callback(
                                RiderSessionAction.GET_SESSION_IN_PROGRESS_BY_STUDENT_SUCCESS,
                                null
                            )
                        }
                    }
                }
            }
        }
    }

    fun getInProgressSessionByTeacher(
        userCode: String,
        callback: (action: RiderSessionAction, data: Any?) -> Unit
    ) {
        Logger.d("getInProgressSessionByTeacher userCode: $userCode")
        CoroutineScope(Dispatchers.Default).launch(
            CoroutineExceptionHandler { _, ex ->
                Logger.e("getInProgressSessionByTeacher: Found an exception exception: ${ex.message}")
                CoroutineScope(Dispatchers.Main).launch {
                    callback(
                        RiderSessionAction.GET_SESSION_IN_PROGRESS_BY_TEACHER_FAIL,
                        "Lỗi xử lý!!\n\nXin hãy thử lại"
                    )
                }
            }
        ) {
            // check internet available
            if (device.getCurrentNetworkConnection()?.checkConnectionAvailable() == false) {
                // bypass check -> always true
                CoroutineScope(Dispatchers.Main).launch {
                    callback(RiderSessionAction.CHECK_SESSION_IN_PROGRESS_BY_TEACHER_PASS, null)
                }
            }
            else {
                val resResult: ResponseResult<StudentSessionInProgressResponse?> =
                    repository.getInProgressSessionByUser(userCode)
                if (resResult.isError) {
                    CoroutineScope(Dispatchers.Main).launch {
                        // set pass check teacher if teacher don't have any session in-progress
                        callback(
                            RiderSessionAction.CHECK_SESSION_IN_PROGRESS_BY_TEACHER_PASS,
                            resResult.errorMessage
                        )
                    }
                } else {
                    CoroutineScope(Dispatchers.Main).launch {
                        if (resResult.data?.inProgressSession != null) {
                            val studentCodeInSession = resResult.data.inProgressSession.studentCode
                            val sessionId = resResult.data.inProgressSession.id
                            Logger.i("getInProgressSessionByTeacher studentCodeInSession: $studentCodeInSession | sessionId: $sessionId")
                            if (studentCodeInSession != null && studentCodeInSession != studentAuthInfo?.userCode && sessionId != inProgressSession?.id) {
                                callback(
                                    RiderSessionAction.TEACHER_HAS_SESSION_IN_OTHER_DEVICE,
                                    resResult.errorMessage
                                )
                            } else {
                                // pass in case continues current session
                                callback(
                                    RiderSessionAction.CHECK_SESSION_IN_PROGRESS_BY_TEACHER_PASS,
                                    resResult.data.inProgressSession
                                )
                            }
                        } else { // if data session is null -> pass check
                            callback(
                                RiderSessionAction.CHECK_SESSION_IN_PROGRESS_BY_TEACHER_PASS,
                                null
                            )
                        }
                    }
                }
            }
        }
    }

    fun checkStudentAvailable(
        studentCode: String,
        callback: (action: RiderSessionAction, data: Any?) -> Unit
    ) {
        Logger.d("checkStudentAvailable studentCode: $studentCode")
        // first cancel last processing
        CoroutineScope(Dispatchers.Default).launch(
            CoroutineExceptionHandler { _, ex ->
                Logger.e("checkStudentAvailable: Found an exception exception: ${ex.message}")
                CoroutineScope(Dispatchers.Main).launch {
                    callback(
                        RiderSessionAction.CHECK_STUDENT_AVAILABLE_FAIL,
                        "Lỗi xử lý!!\n\nXin hãy thử lại"
                    )
                }
            }
        ) {
            // check internet available
            if (device.getCurrentNetworkConnection()?.checkConnectionAvailable() == false) {
                // bypass check -> always true
                CoroutineScope(Dispatchers.Main).launch {
                    callback(RiderSessionAction.CHECK_STUDENT_AVAILABLE_SUCCESS, true)
                }
            }
            else {
                val resResult: ResponseResult<Boolean> = repository.checkStudentAvailable(
                    imei = getImeiDevice(context),
                    studentCode = studentCode
                )
                if (resResult.isError) {
                    Logger.e("Error: ${resResult.errorMessage}")
                    CoroutineScope(Dispatchers.Main).launch {
                        callback(
                            RiderSessionAction.CHECK_STUDENT_AVAILABLE_FAIL,
                            resResult.errorMessage
                        )
                    }
                } else {
                    val canLogin: Boolean = resResult.data as Boolean
                    Logger.d("checkStudentAvailable canLogin: $canLogin | error message: ${resResult.errorMessage}")
                    if (canLogin) {
                        CoroutineScope(Dispatchers.Main).launch {
                            callback(RiderSessionAction.CHECK_STUDENT_AVAILABLE_SUCCESS, true)
                        }
                    } else {
                        CoroutineScope(Dispatchers.Main).launch {
                            callback(
                                RiderSessionAction.CHECK_STUDENT_AVAILABLE_FAIL,
                                resResult.errorMessage
                            )
                        }
                    }
                }
            }
        }
    }
    fun checkMissingDataSession1(
        sessionId: String
    ) {
        Logger.d("checkMissingDataSession1 sessionId: $sessionId | ${localRiderSession?.sessionId} | ${localRiderSession?.id}")
        CoroutineScope(Dispatchers.Default).launch {
            if (device.getCurrentNetworkConnection()?.checkConnectionAvailable() == true) {
                val resResult: ResponseResult<CheckMissingDataSessionResponse> =
                    repository.checkMissingDataSession(
                        sessionId = sessionId,
                    )
                if (resResult.isError) {
                    Logger.e("Error: ${resResult.errorMessage}")
                } else {
                    var timeListMissDataAuthen: List<DateMissing> = resResult.data!!.listMissDataAuthen
                    var timeListMissDataGps: List<DateMissing> = resResult.data.listMissDataGps
                    if (timeListMissDataAuthen.isNotEmpty() || timeListMissDataGps.isNotEmpty()) {
                        pushDataMissing1(
                            Triple(sessionId, timeListMissDataAuthen, timeListMissDataGps)
                        )
                    }
                    Logger.i("checkMissingDataSession1 timeListMissDataAuthen: $timeListMissDataAuthen | timeListMissDataGps: $timeListMissDataGps")
                }
            }
        }
    }
    private fun pushDataMissing1(
        dataMissing: Triple<String, List<DateMissing>, List<DateMissing>>,
    ) {
        CoroutineScope(Dispatchers.Default).launch {

            Logger.d("pushDataMissing1 sessionId: ${dataMissing.first}")
            if (device.getCurrentNetworkConnection()?.checkConnectionAvailable() == true) {
                val sessionId: String = dataMissing.first
                val timeListMissDataAuthen: List<DateMissing> = dataMissing.second
                val timeListMissDataGps: List<DateMissing> = dataMissing.third

                val riderLocalSession: RiderSessionEntity? =
                    repository.getRiderSessionBySessionId(sessionId)
                Logger.i("riderLocalSession: $riderLocalSession")

                val listAuthenData =
                    riderLocalSession?.let {
                        repository.getListAuthenDataByLocalSessionId(
                            localSessionId = it.id
                        )
                    } ?: emptyList()

                val listGPSData =
                    riderLocalSession?.let {
                        repository.getListGPSSignalByLocalSessionId(
                            localSessionId = it.id
                        )
                    } ?: emptyList()

                Logger.i("listAuthenData: $listAuthenData")
                Logger.i("listGPSData: $listGPSData")

                val listMissDataAuthen = listAuthenData.filter { studentAuthenticationEntity ->
                    val time = Date((studentAuthenticationEntity.time / 1000) * 1000)
                    timeListMissDataAuthen.any { range ->
                        time.after(
                            Utils.convertServerTimeToDate(range.fromTime)
                        ) && time.before(Utils.convertServerTimeToDate(range.toTime))
                    }
                }

                val listMissDataGPS = listGPSData.filter { gpsEntity ->
                    val time = Date((gpsEntity.time / 1000) * 1000)
                    timeListMissDataGps.any { range ->
                        time.after(
                            Utils.convertServerTimeToDate(range.fromTime)
                        ) && time.before(Utils.convertServerTimeToDate(range.toTime))
                    }
                }
                Logger.i("listMissDataAuthen: $listMissDataAuthen")
                Logger.i("listMissDataGPS: $listMissDataGPS")

                riderLocalSession?.let {
                    if (listMissDataAuthen.isNotEmpty() || listMissDataGPS.isNotEmpty()) {

                        listMissDataAuthen.forEach { studentAuthentication ->
                            val result = handleRecoverUploadAuthen(
                                riderSessionEntity = riderLocalSession,
                                studentAuthenticationEntity = studentAuthentication
                            )
                            Logger.i("resultRecoverUploadAuthen: $result")
                        }

                        listMissDataGPS.forEach { gpsEntity ->
                            val result = handleRecoverUploadGpsSignal(
                                riderSessionEntity = riderLocalSession,
                                gpsSignalEntity = gpsEntity
                            )
                            Logger.i("resultRecoverUploadGpsSignal: $result")
                        }
                    }

                }
            }
        }
    }

    fun checkMissingDataSession(
        sessionId: String,
        callback: (action: RiderSessionAction, data: Any?) -> Unit
    ) {
        Logger.d("checkMissingDataSession sessionId: $sessionId | ${localRiderSession?.sessionId} | ${localRiderSession?.id}")
        CoroutineScope(Dispatchers.Default).launch(
            CoroutineExceptionHandler { _, ex ->
                Logger.e("checkMissingDataSession: Found an exception exception: ${ex.message}")
                CoroutineScope(Dispatchers.Main).launch {
                    callback(
                        RiderSessionAction.CHECK_MISSING_DATA_FAIL,
                        "Lỗi xử lý!!\n\nXin hãy thử lại"
                    )
                }

            }
        ) {
            if (device.getCurrentNetworkConnection()?.checkConnectionAvailable() == true) {
                val resResult: ResponseResult<CheckMissingDataSessionResponse> =
                    repository.checkMissingDataSession(
                        sessionId = sessionId,
                    )
                if (resResult.isError) {
                    Logger.e("Error: ${resResult.errorMessage}")
                    CoroutineScope(Dispatchers.Main).launch {
                        callback(
                            RiderSessionAction.CHECK_MISSING_DATA_FAIL,
                            resResult.errorMessage
                        )
                    }
                } else {
                    var timeListMissDataAuthen: List<DateMissing> = resResult.data!!.listMissDataAuthen
                    var timeListMissDataGps: List<DateMissing> = resResult.data.listMissDataGps
                    CoroutineScope(Dispatchers.Main).launch {
                        if (timeListMissDataAuthen.isEmpty() && timeListMissDataGps.isEmpty()) {
                            callback(
                                RiderSessionAction.CHECK_MISSING_DATA_SUCCESS,
                                null
                            )
                        } else {
                            callback(
                                RiderSessionAction.CHECK_MISSING_DATA_SUCCESS,
                                Triple(sessionId,timeListMissDataAuthen, timeListMissDataGps)
                            )
                        }
                    }
                    Logger.i("checkMissingDataSession timeListMissDataAuthen: $timeListMissDataAuthen | timeListMissDataGps: $timeListMissDataGps")
                }
            } else {
                CoroutineScope(Dispatchers.Main).launch {
                    callback(
                        RiderSessionAction.CHECK_MISSING_DATA_FAIL,
                        "Không có kết nối internet"
                    )
                }

            }
        }
    }

    fun pushDataMissing(
        dataMissing: Triple<String, List<DateMissing>, List<DateMissing>>,
        riderSessionCallback: (action: RiderSessionAction, data: Any?) -> Unit
    ) {
        Logger.d("pushDataMissing sessionId: ${dataMissing.first}")
        if (device.getCurrentNetworkConnection()?.checkConnectionAvailable() == false) {
            CoroutineScope(Dispatchers.Main).launch {
                riderSessionCallback(
                    RiderSessionAction.PUSH_DATA_MISSING_FAIL,
                    "Can not connect to internet!"
                )
            }
        } else {
            CoroutineScope(Dispatchers.Default).launch {
                val sessionId: String = dataMissing.first
                val timeListMissDataAuthen: List<DateMissing> = dataMissing.second
                val timeListMissDataGps: List<DateMissing> = dataMissing.third

                val riderLocalSession: RiderSessionEntity? =
                    repository.getRiderSessionBySessionId(sessionId)
                Logger.i("riderLocalSession: $riderLocalSession")

                val listAuthenData =
                    riderLocalSession?.let {
                        repository.getListAuthenDataByLocalSessionId(
                            localSessionId = it.id
                        )
                    } ?: emptyList()

                val listGPSData =
                    riderLocalSession?.let {
                        repository.getListGPSSignalByLocalSessionId(
                            localSessionId = it.id
                        )
                    } ?: emptyList()

                Logger.i("listAuthenData: $listAuthenData")
                Logger.i("listGPSData: $listGPSData")

                val listMissDataAuthen = listAuthenData.filter { studentAuthenticationEntity ->
                    val time = Date((studentAuthenticationEntity.time / 1000) * 1000)
                    timeListMissDataAuthen.any { range ->
                        time.after(
                            Utils.convertServerTimeToDate(range.fromTime)
                        ) && time.before(Utils.convertServerTimeToDate(range.toTime))
                    }
                }

                val listMissDataGPS = listGPSData.filter { gpsEntity ->
                    val time = Date((gpsEntity.time / 1000) * 1000)
                    timeListMissDataGps.any { range ->
                        time.after(
                            Utils.convertServerTimeToDate(range.fromTime)
                        ) && time.before(Utils.convertServerTimeToDate(range.toTime))
                    }
                }
                Logger.i("listMissDataAuthen: $listMissDataAuthen")
                Logger.i("listMissDataGPS: $listMissDataGPS")

                riderLocalSession?.let {
                    if (listMissDataAuthen.isEmpty() && listMissDataGPS.isEmpty()) {
                        withContext(Dispatchers.Main) {
                            riderSessionCallback(
                                RiderSessionAction.PUSH_DATA_MISSING_SUCESS,
                                null
                            )
                        }
                    } else {
                        var resultRecoverUploadAuthen = false
                        var resultRecoverUploadGpsSignal = false

                        listMissDataAuthen.forEach { studentAuthentication ->
                            resultRecoverUploadAuthen = handleRecoverUploadAuthen(
                                riderSessionEntity = riderLocalSession,
                                studentAuthenticationEntity = studentAuthentication
                            )
                            Logger.i("resultRecoverUploadAuthen: $resultRecoverUploadAuthen")
                        }

                        listMissDataGPS.forEach { gpsEntity ->
                            resultRecoverUploadGpsSignal = handleRecoverUploadGpsSignal(
                                riderSessionEntity = riderLocalSession,
                                gpsSignalEntity = gpsEntity
                            )
                            Logger.i("resultRecoverUploadGpsSignal: $resultRecoverUploadGpsSignal")
                        }
                        if (resultRecoverUploadAuthen || resultRecoverUploadGpsSignal) {
                            withContext(Dispatchers.Main) {
                                riderSessionCallback(
                                    RiderSessionAction.PUSH_DATA_MISSING_SUCESS,
                                    "đẩy thành công"
                                )
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                riderSessionCallback(
                                    RiderSessionAction.PUSH_DATA_MISSING_FAIL,
                                    null
                                )
                            }
                        }
                    }

                } ?: run {
                    withContext(Dispatchers.Main) {
                        riderSessionCallback(RiderSessionAction.PUSH_DATA_MISSING_FAIL, null)
                    }
                }
            }

        }
    }

    private fun fetchCurrentSession(
        sessionId: String,
        callback: (action: RiderSessionAction, data: Any?) -> Unit
    ) {
        Logger.d("fetchCurrentSession userCode: $sessionId")
        // check internet available
        if (device.getCurrentNetworkConnection()?.checkConnectionAvailable() == false) {
            CoroutineScope(Dispatchers.Main).launch {
                callback(
                    RiderSessionAction.FETCH_CURRENT_SESSION_FAIL_BY_INTERNET,
                    "Can not connect to internet!"
                )
            }
        }
        else {
            // first cancel last processing
            fetchSessionJob?.cancel()
            // make new processing
            fetchSessionJob = CoroutineScope(Dispatchers.IO + Job()).launch(
                CoroutineExceptionHandler { _, ex ->
                    Logger.e("fetchCurrentSession: Found an exception exception: ${ex.message}")
                    CoroutineScope(Dispatchers.Main).launch {
                        callback(
                            RiderSessionAction.FETCH_CURRENT_SESSION_FAIL,
                            "Lỗi xử lý!!\n\nXin hãy thử lại"
                        )
                    }
                }
            ) {
                Logger.i("fetchCurrentSession Call fetchCurrentSession")
                val resResult: ResponseResult<FetchCurrentSessionResponse?> =
                    repository.fetchCurrentSession(sessionId)
                // check condition for fix issue show error dialog after current session finished
                if (inProgressSession != null) {
                    if (resResult.isError) {
                        Logger.e("fetchCurrentSession Error: ${resResult.errorMessage}")
                        when (resResult.errorCode) {
                            ErrorCode.CAN_NOT_CONNECT_TO_SERVER -> {
                                CoroutineScope(Dispatchers.Main).launch {
                                    callback(
                                        RiderSessionAction.CONNECT_SERVICE_ERROR,
                                        "Can not fetch data!!"
                                    )
                                }

                            }

                            0 -> {
                                CoroutineScope(Dispatchers.Main).launch {
                                    callback(
                                        RiderSessionAction.FORCE_LOGOUT_CURRENT_SESSION_BY_ADMIN,
                                        resResult.errorMessage
                                    )
                                }
                            }

                            else -> {
                                CoroutineScope(Dispatchers.Main).launch {
                                    callback(
                                        RiderSessionAction.FETCH_CURRENT_SESSION_FAIL,
                                        resResult.errorMessage
                                    )
                                }

                            }
                        }
                    } else {
                        val totalDistance: Float? = resResult.data?.totalDis
                        val totalTime: Double? = resResult.data?.totalTime
                        Logger.d("fetchCurrentSession totalDistance: $totalDistance | totalTime: $totalTime")
                        if (totalDistance != null && totalTime != null) {
                            val pairData: Pair<Float, Double> = Pair(totalDistance, totalTime)
                            CoroutineScope(Dispatchers.Main).launch {
                                callback(RiderSessionAction.FETCH_CURRENT_SESSION_SUCCESS, pairData)
                            }
                            LogRecorder.i("Đồng bộ dữ liệu phiên từ hệ thống", "totalDistance: $totalDistance | totalTime: $totalTime")
                        } else {
                            LogRecorder.e("Đồng bộ dữ liệu phiên thất bại", "FETCH_CURRENT_SESSION_FAIL")
                            CoroutineScope(Dispatchers.Main).launch {
                                callback(
                                    RiderSessionAction.FETCH_CURRENT_SESSION_FAIL,
                                    "Data has been null!!"
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    suspend fun getAutoLogoutTime(): Int {
        return withContext(Dispatchers.Default) {
            repository.getAutoLogoutTime()
        }
    }
    fun saveAutoLogoutTime(autoLogoutTime: Int){
        CoroutineScope(Dispatchers.Default).launch {
            repository.saveAutoLogoutTime(autoLogoutTime = autoLogoutTime)
        }
    }

    fun getImageLogin(): File? {
        return localRiderSession?.loginImagePath?.let { File(it) }
    }

    fun getCarsByImeiAndTrainingCenter(
        idTrainingCenter: String,
        callback: (action: RiderSessionAction, data: Any?) -> Unit
    ) {
        Logger.d("getCarsByImeiAndTrainingCenter idTrainingCenter: $idTrainingCenter")
        // [DAT CER]: only use for get DAT certification
        // check internet available
        if (device.getCurrentNetworkConnection()?.checkConnectionAvailable() == false) {
            CoroutineScope(Dispatchers.Main).launch {
                callback(
                    RiderSessionAction.GET_LIST_CARS_TEACHER_FAIL_BY_INTERNET,
                    "Can not connect to internet!"
                )
            }
        } // [DAT CER]
        else {
            CoroutineScope(Dispatchers.Default).launch(
                CoroutineExceptionHandler { _, ex ->
                    Logger.e("getCarsByImeiAndTrainingCenter: Found an exception exception: ${ex.message}")
                    CoroutineScope(Dispatchers.Main).launch {
                        callback(
                            RiderSessionAction.GET_LIST_CARS_TEACHER_FAIL,
                            "Lỗi xử lý!!\n\nXin hãy thử lại"
                        )
                    }
                }
            ) {
                val resResult: ResponseResult<List<CarInfo>> =
                    repository.getCarsByImeiAndTrainingCenter(
                        getImeiDevice(context),
                        idTrainingCenter
                    )
                if (resResult.isError) {
                    Logger.e("Response Error: ${resResult.errorMessage}")
                    CoroutineScope(Dispatchers.Main).launch {
                        callback(
                            RiderSessionAction.GET_LIST_CARS_TEACHER_FAIL,
                            resResult.errorMessage
                        )
                    }
                } else {
                    val listCars: List<CarInfo>? = resResult.data
                    listCars?.also {
                        Logger.i("listCars: $listCars")
                        CoroutineScope(Dispatchers.Main).launch {
                            callback(RiderSessionAction.GET_LIST_CARS_TEACHER_SUCCESS, listCars)
                        }
                        if (it.isNotEmpty()) {
                            // Save current teacher info
                            teacherAuthInfo?.userCode?.also {
                                repository.saveTeacherCode(it)
                            }
                        }
                    }
                }
            }
        }
    }
    fun getImeiDevice(context: Context): String{
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if(repository.getSerialNumber() != null){
                repository.getSerialNumber()!!
            }else{
                Utils.readDataConfig()?.get(0) ?: ""
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Utils.getImeiDevice(context = context)
        }else throw RuntimeException("Version Android not supported!")
    }
    fun  isConnectionAvailable(): Boolean{
        return  device.getCurrentNetworkConnection()?.checkConnectionAvailable() ?: false
    }


    fun pushTeacherInWorking(teacher: UserEntity) {
        teacherAuthInfo = teacher
        repository.saveTeacherCode(teacherAuthInfo!!.userCode)
    }

    fun dropTeachOutWorking() {
        teacherAuthInfo = null
        repository.removeTeacherCode()
    }

    fun pushStudentInSession(student: UserEntity) {
        studentAuthInfo = student
        repository.saveStudentCode(studentAuthInfo!!.userCode)
    }

    fun getStudentInSession(): String? = repository.getStudentCode()

    fun dropStudentOutSession() {
        studentAuthInfo = null
        repository.removeStudentCode()
    }

    private fun removeCurrentSession() {
        inProgressSession = null
        repository.removeSessionCode()
        localRiderSession = null
    }

    fun saveInProgressSession(sessionId: String) {
        repository.saveSessionCode(sessionId)
    }

    fun getSessionInProgress(): String? {
        return repository.getSessionCode()
    }

    fun getCarsByImeiAndCourse(
        idCourse: String,
        callback: (action: RiderSessionAction, data: Any?) -> Unit
    ) {
        Logger.d("getCarsByImeiAndCourse idCourse: $idCourse")
        CoroutineScope(Dispatchers.Default).launch(
            CoroutineExceptionHandler { _, ex ->
                Logger.e("getCarsByImeiAndCourse: Found an exception exception: ${ex.message}")
                CoroutineScope(Dispatchers.Main).launch {
                    callback(
                        RiderSessionAction.GET_LIST_CARS_STUDENT_FAIL,
                        "Lỗi xử lý!!\n\nXin hãy thử lại"
                    )
                }
            }
        ) {
            // [DAT CER]: only use for get DAT certification
            // check internet available
            if (device.getCurrentNetworkConnection()?.checkConnectionAvailable() == false) {
                CoroutineScope(Dispatchers.Main).launch {
                    callback(
                        RiderSessionAction.GET_LIST_CARS_STUDENT_FAIL_BY_INTERNET,
                        "Can not connect to internet!"
                    )
                }
            } // [DAT CER]
            else {
                val resResult: ResponseResult<List<CarInfo>> =
                    repository.getCarsByImeiAndCourse(getImeiDevice(context), idCourse)
                if (resResult.isError) {
                    Logger.e("Error: ${resResult.errorMessage}")
                    CoroutineScope(Dispatchers.Main).launch {
                        callback(
                            RiderSessionAction.GET_LIST_CARS_STUDENT_FAIL,
                            resResult.errorMessage
                        )
                    }
                } else {
                    val listCars: List<CarInfo>? = resResult.data
                    listCars?.also {
                        Logger.i("listCars: $listCars")
                        CoroutineScope(Dispatchers.Main).launch {
                            callback(RiderSessionAction.GET_LIST_CARS_STUDENT_SUCCESS, listCars)
                        }
                    }
                }
            }
        }
    }

    fun checkCarPause(): Boolean {
        // [DAT CER]: only use for get DAT certification
        return device.getCurrentGPS()?.checkMoving() == false
        // [DAT CER]
//        return true
    }

    private fun handleErrorFromServer(errorCode: Int) {
        when (errorCode) {
            ErrorCode.SERVER_DECLINE_AUTHENTICATE -> {
                Logger.i("handleErrorFromServer REQUEST_SERVER_HAS_PROBLEM")
            }
        }
    }
    fun getListSessionHistory(
        sessionHistoryRequest: SessionHistoryRequest,
        callback: (action: RiderSessionAction, data: Any?) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch(
            CoroutineExceptionHandler { _, ex ->
                Logger.e("getSessionHistory: Found an exception: ${ex.message}")
            }
        ) {
            val resResult: ResponseResult<SessionHistoryResponse?> =
                repository.getListSessionHistory(sessionHistoryRequest = sessionHistoryRequest)
            if (resResult.isError) {
                Logger.e("Error: ${resResult.errorMessage}")
                CoroutineScope(Dispatchers.Main).launch {
                    callback(RiderSessionAction.GET_SESSION_HISTORY_FAIL, resResult.errorMessage)
                }
            } else {
                CoroutineScope(Dispatchers.Main).launch {
                    callback(
                        RiderSessionAction.GET_SESSION_HISTORY_SUCCESS,
                        resResult.data
                    )
                }
            }
        }
    }

    private fun countAuthDataUpload() {
        val authenDataUploadResult = getAuthenDataUploadResult()
        // if local data deleted then get totalAuthDataUploadSuccess from api
        // authenDataUploadResult.first < inProgressSession!!.totalAuthen ( data has been deleted )
        if (authenDataUploadResult.first >= inProgressSession!!.totalAuthen) {
            inProgressSession!!.totalAuthDataUploadSuccess = authenDataUploadResult.first
        } else {
            inProgressSession!!.totalAuthDataUploadSuccess = inProgressSession!!.totalAuthen
        }

        // if local data deleted then get totalAuthDataUpload by totalAuthen data from api + authenData upload fail from local
        // authenDataUploadResult.second < inProgressSession!!.totalAuthen ( data has been deleted )
        if (authenDataUploadResult.second >= inProgressSession!!.totalAuthen) {
            inProgressSession!!.totalAuthDataUpload = authenDataUploadResult.second
        } else {
            inProgressSession!!.totalAuthDataUpload =
                inProgressSession!!.totalAuthen + authenDataUploadResult.third
        }
    }
    private fun countGPSDataUpload() {
        val gpsDataUploadResult = getGpsDataUploadResult()

        inProgressSession!!.totalGPSUploadSuccess = gpsDataUploadResult.first
        inProgressSession!!.totalGPSUpload = gpsDataUploadResult.second

        if (gpsDataUploadResult.first >= inProgressSession!!.totalGps) {
            inProgressSession!!.totalGPSUploadSuccess = gpsDataUploadResult.first
        } else {
            inProgressSession!!.totalGPSUploadSuccess = inProgressSession!!.totalGps
        }

        if (gpsDataUploadResult.second >= inProgressSession!!.totalGps) {
            inProgressSession!!.totalGPSUpload = gpsDataUploadResult.second
        } else {
            inProgressSession!!.totalGPSUpload =
                inProgressSession!!.totalGps + gpsDataUploadResult.third
        }
    }

    fun exportSessionReport(riderSessionEntity: RiderSessionEntity, channel: Channel<Any>){
        repository.exportSessionReport(riderSessionEntity, channel)
    }
    suspend fun getLocalListRiderSessionEntity(): List<RiderSessionEntity>{
        return repository.getListRiderSessionEntity()
    }
}

enum class RiderSessionAction {
    GET_LIST_CARS_STUDENT_SUCCESS,
    GET_LIST_CARS_STUDENT_FAIL,
    GET_LIST_CARS_STUDENT_FAIL_BY_INTERNET,
    GET_LIST_CARS_TEACHER_SUCCESS,
    GET_LIST_CARS_TEACHER_FAIL,
    GET_LIST_CARS_TEACHER_FAIL_BY_INTERNET,
    GET_SESSION_IN_PROGRESS_BY_STUDENT_SUCCESS,
    GET_SESSION_HISTORY_SUCCESS,
    GET_SESSION_HISTORY_FAIL,
    STUDENT_HAS_SESSION_IN_OTHER_DEVICE,
    STUDENT_AND_TEACHER_NOT_MATCH,
    GET_SESSION_IN_PROGRESS_BY_STUDENT_FAIL,
    LOCAL_STUDENT_IN_PROGRESSS_SESSION_NOT_MATCH,
    TEACHER_HAS_SESSION_IN_OTHER_DEVICE,
    CHECK_SESSION_IN_PROGRESS_BY_TEACHER_PASS,
    GET_SESSION_IN_PROGRESS_BY_TEACHER_FAIL,
    UPLOAD_IMAGE_CAPTURE_SUCCESS,
    UPLOAD_IMAGE_CAPTURE_FAIL,
    START_RIDER_SESSION_SUCCESS,
    START_RIDER_SESSION_FAIL,
    START_RIDER_SESSION_FAIL_BY_LOCATION,
    FINISH_RIDER_SESSION_SUCCESS,
    FINISH_RIDER_SESSION_SUCCESS_WITH_ERROR,
    FINISH_RIDER_SESSION_SUCCESS_OFFLINE,
    FINISH_RIDER_SESSION_FAIL,
    FINISH_RIDER_SESSION_FAIL_BY_LOCATION,
    PUSH_SESSION_TO_TC_FAIL,
    FETCH_CURRENT_SESSION_SUCCESS,
    FETCH_CURRENT_SESSION_FAIL,
    FORCE_LOGOUT_CURRENT_SESSION_BY_ADMIN,
    FETCH_CURRENT_SESSION_FAIL_BY_INTERNET,
    CONNECT_SERVICE_ERROR,
    CHECK_STUDENT_AVAILABLE_SUCCESS,
    CHECK_STUDENT_AVAILABLE_FAIL,
    CHECK_MISSING_DATA_SUCCESS,
    CHECK_MISSING_DATA_FAIL,
    PUSH_DATA_MISSING_SUCESS,
    PUSH_DATA_MISSING_FAIL,
    CHECK_DEVICE_DATE_TIME_FAIL,
    SENT_DATA_IMMEDIATELY,
}

enum class VerifyResult(val code: Int) {
    VERIFY_SUCCESS(1),
    VERIFY_FAIL(2)
}
