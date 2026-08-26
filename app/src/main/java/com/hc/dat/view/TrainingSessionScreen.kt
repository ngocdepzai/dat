package com.hc.dat.view

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.*
import android.hardware.usb.UsbDevice
import android.location.Location
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.view.*
import androidx.annotation.RequiresApi
import androidx.core.graphics.drawable.toBitmapOrNull
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import coil.request.ImageRequest
import com.hc.dat.model.CarInfo
import com.hc.dat.model.InProgressSession
import com.hc.dat.model.LoginType
import com.hc.dat.model.UserInfo
import com.hc.dat.model.database.entity.UserEntity
import com.hc.dat.model.database.entity.UserType
import com.hc.dat.model.database.entity.convertToModelEntity
import com.hc.dat.service.ServiceDefinition
import com.hc.dat.utils.ImageLoader
import com.hc.dat.utils.Utils
import com.hc.dat.view.adapter.DialogButtonClickListener
import com.hc.dat.view.dialog.*
import com.hc.dat.viewmodel.*
import com.lws.device.camerapreview.*
import com.lws.device.gps.GPSAction
import com.lws.device.gps.GPSEvent
import com.lws.device.nfc.NFCAction
import com.lws.device.nfc.NFCEvent
import com.lws.type.LogRecorder
import com.lws.type.Logger
import hc.manager.datapp.BuildConfig
import hc.manager.datapp.R
import hc.manager.datapp.databinding.ScreenTrainingSessionBinding
import hc.manager.datapp.service.MyPhoneStateListener
import hc.manager.datapp.utils.DateUtil
import kotlinx.android.synthetic.main.dat_activity_main.*
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.util.*
import java.util.concurrent.ArrayBlockingQueue
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.random.Random

class TrainingSessionScreen : DatBaseScreen() {
    private lateinit var applicationViewModel: ApplicationViewModel
    private lateinit var viewBinding: ScreenTrainingSessionBinding
    private lateinit var riderSessionViewModel: RiderSessionViewModel
    private lateinit var faceRecognitionViewModel: FaceRecognitionViewModel
    private var powerSaveReceiver: BroadcastReceiver? = null
    private lateinit var powerManager: PowerManager
    private var nfcAvailable: Boolean = true // default true because must login teacher first
    private var studentImageLogin: File? = null
    private var studentImageLogout: File? = null
    var studentAuthInfo: UserEntity? = null
    var isGpsAvailableShown = false
    private var hcImageFolder: File = File(Environment.getExternalStorageDirectory().toString() + "/HC_DAT_IMAGES")
    private lateinit var cameraPreviewDevice: CameraPreviewDevice
    private var studentSessionFolder: File? = null
    private var isStudentContinueSession: Boolean = false
    private var byPassCheckSpeed: Boolean = false
    private var loginRfid: Boolean? = null

    private var cameraPreviewDataQueue: ArrayBlockingQueue<Nv21ImageData> = ArrayBlockingQueue(CAMERA_DATA_QUEUE_SIZE)
    private var faceDetectedMessageQueue: ArrayBlockingQueue<FaceImageData> = ArrayBlockingQueue(FACE_DATA_QUEUE_SIZE)
    private var cameraRotation = 0
    private var notFace: Boolean = true
    private var notMask: Boolean = true
    private var isSuccessAtLast = true
    private var recognizeFirstTime = true
    private var countTimeCheckLastImageDetected = 0L
    private var faceMatching: Boolean = false
    private var searchScore: Float = 0F
    private var successScore: Float = 0F
    private var failScore: Float = 0F
    private var searchThreshold: Float = 65F
    @Volatile private var faceInGuideSeenThisCycle: Boolean = false
    @Volatile private var lastFaceOutsideGuideTime: Long = 0L
    private var isThreadRunningJob: Job? = null
    private var recognitionJob: Job? = null
    @Volatile
    private var timeCounterThread: Thread? = null
    private var nfcResultData: String? = null
    private var learningOverTimeDuration = 2L
    private var learningTimeOver10HoursDuration = 2L
    private var autoLogoutDuration = 2L
    private var sendAuthenDataDuration = TIME_FREQUENCY_SENT_DATA
    private var recognizeFaceTimeDuration = TIME_FREQUENCY_FACE_RECOGNITION
    private var authDataMissingTimeDuration = 2L
    private var updateSearchThresholdDuration = 1L * 60L
    private var myListener: MyPhoneStateListener? = null
    private var telephonyManager: TelephonyManager? = null
    // flag
    private var connectServiceFailCounter: Int = 0
    private var connectServiceFailFlag = false
    private var verifyFailCounter: Int = 0
    private var autoChangeNightMode: Boolean = true
    private var isSentGeneral: Boolean = true
    private var timeTrackingVelocity: Long = 30// seconds
    private var timeStartRecognition: Long = 0
    private var timeSendAuthData: Long = 0
    private var adminLogoutRequestCount: Int = 0
    // 1. Khai báo Callback ở cấp class
    private var connectivityManager: ConnectivityManager? = null
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            isWifiConnected = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
            isMobileConnected = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
            // Cách sửa: Sử dụng runOnUiThread của activity
            activity?.runOnUiThread {
                updateNetworkIconsUI()
            }
        }

        override fun onLost(network: Network) {
            // Khi mất kết nối hoàn toàn
            isWifiConnected = false
            isMobileConnected = false
            // Cách sửa: Sử dụng runOnUiThread của activity
            activity?.runOnUiThread {
                updateNetworkIconsUI()
            }
        }
    }
    private var isWifiConnected: Boolean = false
    private var isMobileConnected: Boolean = false

    var timeSecondCounter = 0L
    var deviceStatusSecondCounter = 0L
    var learningOverTimeSecondCounter = 0L
    var learningTimeOver10HoursCounter = 0L
    var autoLogoutCounter = 0L
    var collectFaceDetectedSecondCounter = 0L
    var recognizeUserFaceSecondCounter = 0L
    var trackingLastImageRecognizedCounter = 0L
    var trackingLastAuthenDataCounter = 0L
    var updateSearchThresholdCounter = 0L
    var trackingGPSCounter = 0L
    // Mốc gửi dữ liệu xác thực kế tiếp, tính bằng epoch millisecond.
    // Dùng mốc đồng hồ thực thay cho bộ đếm vòng lặp cũ vì mỗi vòng của
    // startTimeCounter() dài hơn 1000ms, đếm vòng sẽ làm chu kỳ 5 phút trôi dần.
    @Volatile
    private var nextSendAuthenDataTime = 0L
    @Volatile
    private var sendAuthenDataJob: Job? = null

    companion object {
        const val TIME_FREQUENCY_FACE_RECOGNITION: Long = 3 * 60L
        const val TIME_FREQUENCY_SENT_DATA: Long = 5 * 60L
        const val TIME_FREQUENCY_SENT_GPS: Long = 10L
        const val TIME_FREQUENCY_TRACKING_LAST_IMAGE_RECOGNIZED: Long = 1 * 60L
        const val TIME_FREQUENCY_TRACKING_LAST_AUTHEN_DATA: Long = 1 * 60L // second
        const val TIME_CHECKING_PARAM_NOT_DISPLAY: Long = 6 * 60L  // second
        const val TIME_CHECKING_MISSING_IMAGE_DETECTED: Long = 10 * 60L  // second
        const val TIME_CHECKING_MISSING_AUTHEN_DATA: Long = 10 * 60L // second
        const val TIME_CHECKING_SESSION_INTERRUPT: Long = 10 // minute
        const val TIME_AUTO_LOGOUT: Long = 60 // second
        const val FACE_RECOGNITION_CHECKING_TIME: Long = 30 * 1000
        const val GOOD_SUCCESS_PERCENTAGE = 80
        const val NORMAL_SUCCESS_PERCENTAGE = 75
        const val LOW_SUCCESS_PERCENTAGE = 74
        const val CAMERA_DATA_QUEUE_SIZE = 10
        const val FACE_DATA_QUEUE_SIZE = 10
        const val CONNECT_SERVICE_FAIL_WARNING = 5
        const val FREQUENCY_CHECK_DEVICE_STATUS_TIME = 3 * 10L
        const val FREQUENCY_CHECK_LEARNING_TIME = 2 * 60L
        const val CHECKING_GPS_TIME: Long = 3 * 60L  // second
        const val CHECKING_LAST_GPS_UPLOAD_TIME: Long = 2 * 60L  // second
        const val TRACKING_GPS_TIME = 1 * 60L
        const val TIME_WARNING_OVER = 5 * 60L
        const val TIME_ERROR_OVER = 1 * 60L
        const val LEARNING_TIME_OVER = 4 * 60 * 60
        const val AUTO_LOGOUT_TIME_IN_SESSION = 3 * 60 * 60 + 55 * 60
        const val AUTO_LOGOUT_TIME_IN_DAY = 9 * 60 * 60 + 50 * 60
        const val LEARNING_TIME_OVER_IN_24H = 10 * 60 * 60
        const val WARNING_TIME_REMAINING_TO_30_MINUTES = 30 * 60
        const val WARNING_TIME_REMAINING_TO_10_MINUTES = 10 * 60
    }

    init {
        if (!hcImageFolder.exists()) {
            hcImageFolder.mkdirs()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 2. Đăng ký trong onCreate
        connectivityManager = context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        connectivityManager!!.registerDefaultNetworkCallback(networkCallback)
    }

    /**
     * Neo mốc gửi dữ liệu xác thực kế tiếp về [delaySeconds] giây kể từ hiện tại,
     * rồi khởi động lại bộ định thời để nó ngủ theo mốc mới.
     *
     * Chỉ dùng cho ba mốc neo theo sự kiện đã có sẵn: lần gửi đầu phiên, tiếp tục
     * phiên, và ép gửi ngay khi ảnh nhận dạng về trễ. Nhịp 5 phút định kỳ do
     * [startSendAuthenDataScheduler] tự cộng dồn, không đi qua hàm này.
     *
     * Không được gọi từ bên trong [sendAuthenDataJob] vì hàm này huỷ chính job đó.
     */
    @RequiresApi(Build.VERSION_CODES.M)
    private fun scheduleNextSendAuthenData(delaySeconds: Long) {
        sendAuthenDataDuration = delaySeconds
        // Cắt phần lẻ mili giây để mọi mốc rơi đúng đầu giây. Nếu để phần lẻ ngẫu
        // nhiên, mốc có thể nằm sát ranh giới giây (vd .995) và chỉ vài ms đánh thức
        // coroutine cũng đủ đẩy giây ghi nhận sang +1.
        nextSendAuthenDataTime =
            (Calendar.getInstance().timeInMillis / 1000 + delaySeconds) * 1000
        startSendAuthenDataScheduler()
    }

    /**
     * Bộ định thời riêng cho việc gửi dữ liệu xác thực: ngủ thẳng tới mốc kế tiếp
     * rồi bắn, thay vì dò mỗi giây bên trong [startTimeCounter].
     *
     * Vòng lặp [startTimeCounter] chỉ kiểm tra một lần mỗi vòng, mà mỗi vòng luôn
     * dài hơn 1000ms, nên mốc có thể bị phát hiện muộn tới cả giây — giờ ghi nhận
     * vì thế lệch 1 giây so với chu kỳ. Ngủ thẳng tới mốc thì độ trễ chỉ còn thời
     * gian đánh thức coroutine.
     *
     * Mốc kế tiếp cộng dồn từ mốc cũ nên sai số không tích lũy qua các chu kỳ:
     * bắn lần đầu lúc 10:35:12 thì lần sau đúng 10:40:12.
     */
    @Synchronized
    @RequiresApi(Build.VERSION_CODES.M)
    private fun startSendAuthenDataScheduler() {
        sendAuthenDataJob?.cancel()
        sendAuthenDataJob = CoroutineScope(Dispatchers.Default).launch(
            CoroutineExceptionHandler { _, _ ->
                Logger.w("Error in sendAuthenDataScheduler!")
                startSendAuthenDataScheduler()
            }
        ) {
            while (isActive) {
                val currentTimeMillis = Calendar.getInstance().timeInMillis
                // Mốc bằng 0 nghĩa là chưa neo (khởi động lần đầu, hoặc vừa bị
                // resetTimeCounter() xoá) -> neo theo chu kỳ hiện hành.
                if (nextSendAuthenDataTime == 0L) {
                    nextSendAuthenDataTime =
                        (currentTimeMillis / 1000 + sendAuthenDataDuration) * 1000
                }
                val waitMillis = nextSendAuthenDataTime - currentTimeMillis
                if (waitMillis > 0) {
                    delay(waitMillis)
                    // Đọc lại mốc thay vì bắn ngay sau khi ngủ dậy: mốc có thể đã
                    // bị neo lại trong lúc đang ngủ.
                    continue
                }

                nextSendAuthenDataTime += TIME_FREQUENCY_SENT_DATA * 1000
                // Trễ quá một chu kỳ (máy ngủ, tiến trình bị treo) thì neo lại từ
                // hiện tại để không bắn dồn nhiều lần cho các mốc đã lỡ.
                if (nextSendAuthenDataTime <= currentTimeMillis) {
                    nextSendAuthenDataTime = currentTimeMillis + TIME_FREQUENCY_SENT_DATA * 1000
                }
                // Giữ đúng điều kiện của vị trí cũ: chỉ gửi khi phiên đang chạy
                if (riderSessionViewModel.inProgressSession != null) {
                    sendDataAuthenBlock()
                }
            }
        }
    }

    private fun logicBlockChecking(secondCounter: Long, secondDuration: Long, block: () -> Unit): Long {
        if (secondCounter >= secondDuration) {
            block()
            return 0
        }
        return secondCounter + 1
    }
    @RequiresApi(Build.VERSION_CODES.M)
    private fun startTimeCounter() {
        Logger.d("startTimeCounter")
        timeCounterThread?.interrupt()
        // Gắn vòng đời bộ định thời gửi dữ liệu vào đúng vòng đời timeCounterThread:
        // mọi đường resume đều đi qua đây, và pauseHandleProcess() huỷ cả hai.
        // Mốc nextSendAuthenDataTime là field nên khởi động lại không làm dịch lưới.
        startSendAuthenDataScheduler()
        timeCounterThread = Thread {
            try {
                while (timeCounterThread?.isAlive == true || timeCounterThread?.isInterrupted == false) {
                    // Time counter logic block checking
                    timeSecondCounter = logicBlockChecking(
                        secondCounter = timeSecondCounter,
                        secondDuration = 0, // set to 0 for update real time
                    ) { clockLogicBlock() }
                    // If the session is beginning -> call recognize at first time
                    if (riderSessionViewModel.inProgressSession?.totalVerifyCounter == 0 && recognizeFirstTime) {
                        recognizeFirstTime = false
                        recognizeFaceTimeDuration = 4 // second
                        // delay 20s for face recognition can be detect
                        scheduleNextSendAuthenData(20) // second
                        // time to calculate data validation time
                        timeSendAuthData = sendAuthenDataDuration
                        timeStartRecognition = Calendar.getInstance().timeInMillis / 1000
                    }
                    // Device status checking logic block
                    deviceStatusSecondCounter = logicBlockChecking(
                        secondCounter = deviceStatusSecondCounter,
                        secondDuration = FREQUENCY_CHECK_DEVICE_STATUS_TIME,
                    ) { deviceStatusCheckingBlock() }
                    // when camera is open
                    if (cameraPreviewDevice.isOpenCamera()) {
                        // collect face detected from camera block
                        collectFaceDetectedSecondCounter = logicBlockChecking(
                            secondCounter = collectFaceDetectedSecondCounter,
                            secondDuration = 0,
                        ) { collectFaceDetectedBlock() }
                    }
                    trackingGPSCounter = logicBlockChecking(
                        secondCounter = trackingGPSCounter,
                        secondDuration = TRACKING_GPS_TIME,
                    ) { trackingGPSTimeBlock() }

                    updateSearchThresholdCounter = logicBlockChecking(
                        secondCounter = updateSearchThresholdCounter,
                        secondDuration = updateSearchThresholdDuration,
                    ) { updateSearchThresholdBlock() }

                    // when inProgressSession is exist -> run rider session handle processes
                    riderSessionViewModel.inProgressSession?.also { _ ->
                        // Check learning time over logic block
                        learningOverTimeSecondCounter = logicBlockChecking(
                            secondCounter = learningOverTimeSecondCounter,
                            secondDuration = learningOverTimeDuration,
                        ) { checkLearningOver4HoursBlock() }

                        learningTimeOver10HoursCounter = logicBlockChecking(
                            secondCounter = learningTimeOver10HoursCounter,
                            secondDuration = learningTimeOver10HoursDuration,
                        ) { checkLearningOver10HoursBlock() }

                        autoLogoutCounter = logicBlockChecking(
                            secondCounter = autoLogoutCounter,
                            secondDuration = autoLogoutDuration,
                        ) { handleAutoLogout() }

                        // Recognize User Face logic block
                        recognizeUserFaceSecondCounter = logicBlockChecking(
                            secondCounter = recognizeUserFaceSecondCounter,
                            secondDuration = recognizeFaceTimeDuration,
                        ) { recognizeUserFaceBlock() }

                        // Việc gửi dữ liệu xác thực không nằm trong vòng lặp này nữa:
                        // nó chạy bằng startSendAuthenDataScheduler() để không chịu
                        // độ trễ tick.

                        // Check learning time over logic block
                        trackingLastImageRecognizedCounter = logicBlockChecking(
                            secondCounter = trackingLastImageRecognizedCounter,
                            secondDuration = TIME_FREQUENCY_TRACKING_LAST_IMAGE_RECOGNIZED,
                        ) { trackingImageDetectedTimeBlock() }

                        trackingLastAuthenDataCounter = logicBlockChecking(
                            secondCounter = trackingLastAuthenDataCounter,
                            secondDuration = authDataMissingTimeDuration,
                        ) { trackingLastAuthenTimeBlock() }
                    }
                    // Default count time by one second
                    Thread.sleep(1000)
                }

            } catch (e: InterruptedException) {
                Logger.w("Thread was interrupted")
            }
        }.apply {
            start()
        }
    }

    private fun updateSearchThresholdBlock() {
        CoroutineScope(Dispatchers.IO).launch(
            CoroutineExceptionHandler { _, _ -> Logger.w("Error in updateSearchThresholdBlock!")}
        ) {
            Logger.d("updateSearchThresholdBlock!")
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)
            Logger.i("vinhdt: $hour : $minute")
            if (hour == 17 && minute in 4..6) {
                appViewModel.updateSearchThreshold(){
                    searchThreshold = appViewModel.searchThreshold
                }
            } else {
                searchThreshold = appViewModel.searchThreshold
            }
        }
    }

    private fun handleAutoLogout() {
        autoLogoutDuration = TIME_AUTO_LOGOUT
        var autoLogoutTime = 0
        val totalTimeIn24h = riderSessionViewModel.inProgressSession!!.totalTime + (riderSessionViewModel.inProgressSession!!.timeIn24H ?: 0.0)

        CoroutineScope(Dispatchers.Default).launch {
            autoLogoutTime = riderSessionViewModel.getAutoLogoutTime() * 60
            if (totalTimeIn24h >= AUTO_LOGOUT_TIME_IN_DAY + autoLogoutTime) {
                withContext(Dispatchers.Main){
                    handleFinishRiderSession(sessionContinues = false, autoLogout = true)
                }
            }
        }
    }

    private fun checkTimeCounterThread() {
        Logger.d("startTimeCounter")
        isThreadRunningJob?.cancel()
        isThreadRunningJob = CoroutineScope(Dispatchers.Default).launch(
            CoroutineExceptionHandler { _, _ ->
                Logger.w("Error in show current time!")
                checkTimeCounterThread()
            }
        ) {
            while(isThreadRunningJob?.isActive == true){
                if(timeCounterThread?.isAlive == false || timeCounterThread == null || timeCounterThread?.isInterrupted == true){
                    startTimeCounter()
                }
                delay(10000)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun sendDataAuthenBlock() {
        // re-set sendAuthenDataDuration to default
        sendAuthenDataDuration = TIME_FREQUENCY_SENT_DATA
        CoroutineScope(Dispatchers.IO).launch(
            CoroutineExceptionHandler { _, _ ->
                Logger.w("Error in SentData!")
            }
        ) {
            Logger.d("Finish one round send data!")
            riderSessionViewModel.sessionVerificationInfo.timeAuth = Calendar.getInstance().timeInMillis
            handleSendDataAuthentication()
        }
    }
    private fun trackingGPSTimeBlock() {
        Logger.d("trackingGPSTimeBlock")
        CoroutineScope(Dispatchers.Main).launch() {
            val lat = riderSessionViewModel.sessionVerificationInfo.lat
            val long = riderSessionViewModel.sessionVerificationInfo.long
            val lastLocationUpdateTime = riderSessionViewModel.sessionVerificationInfo.lastLocationUpdateTime
            val currentTime = Utils.getRealTimeStamp() / 1000
            val lastUploadGPSTime = riderSessionViewModel.inProgressSession?.lastUploadGPSTime ?: 0L

            if (lastUploadGPSTime != 0L && (currentTime - lastUploadGPSTime) >= CHECKING_LAST_GPS_UPLOAD_TIME) {
                BaseNotification.showWarning(getString(R.string.upload_gps_error))
            }

            if (lat == 0.0 && long == 0.0) {
                BaseNotification.showError(getString(R.string.gps_error), priority = Priority.HIGH)
            }
            if(riderSessionViewModel.notifyDistanceNotChange){
                if ((currentTime - lastLocationUpdateTime) >= CHECKING_GPS_TIME && lastLocationUpdateTime != 0L) {
                    BaseNotification.showWarning(getString(R.string.distance_not_change))
                    LogRecorder.w("Thông báo: ",getString(R.string.distance_not_change))
                }
            }
        }
    }

    private fun updateNetworkIconsUI() {
        // Icon Wifi
        if (isWifiConnected) {
            viewBinding.ivWifiStatus.setImageResource(R.drawable.iconwifi)
        } else {
            viewBinding.ivWifiStatus.setImageResource(R.drawable.iconnonwifi)
        }

        // Icon 4G
        if (isMobileConnected) {
            viewBinding.ivWirelessStatus.setImageResource(R.drawable.iconwireless)
        } else {
            viewBinding.ivWirelessStatus.setImageResource(R.drawable.iconnonwireless)
        }
    }
    private fun trackingImageDetectedTimeBlock() {
        Logger.d("trackingImageDetectedTimeBlock")
        // handle checking time image detected
        CoroutineScope(Dispatchers.IO).launch(
//            CoroutineExceptionHandler { _, ex ->
//                Logger.e("trackingImageDetectedTimeBlock: Found an exception exception: ${ex.message}")
//            }
        ) {
            if (countTimeCheckLastImageDetected >= TIME_CHECKING_MISSING_IMAGE_DETECTED) {
                val interruptedTime: Int = (countTimeCheckLastImageDetected/60).toInt()
                val sessionContinues = riderSessionViewModel.canContinueSessionAfterDisruption(interruptedTime = interruptedTime)
                val message = getString(R.string.missing_image_detected_message, (countTimeCheckLastImageDetected / 60).toInt())
                LogRecorder.d("", getString(R.string.missing_image_detected_message, (countTimeCheckLastImageDetected / 60).toInt()))
                withContext(Dispatchers.Main) {
                    showDialog(
                        title = getString(R.string.title_notification),
                        message = message,
                        cancelable = false,
                        buttonList = if (sessionContinues) {
                            listOf(getString(R.string.quit_session), getString(R.string.deny_bt))
                        } else {
                            listOf(getString(R.string.quit_session))
                        },
                        listener = object : DialogButtonClickListener {
                            override fun onDialogButtonClick(position: Int) {
                                dismissDialog()
                                if (position == 0) {
                                    handleFinishRiderSession(sessionContinues = sessionContinues)
                                }
                            }
                        }
                    )
                }
                delay(1000)
                withContext(Dispatchers.Main) {
                    BaseNotification.showWarning(message, showToast = false)
                }
            }
        }
    }
    private fun trackingLastAuthenTimeBlock() {
        authDataMissingTimeDuration = TIME_FREQUENCY_TRACKING_LAST_AUTHEN_DATA
        Logger.d("trackingLastAuthenTimeBlock")
        // handle checking last authen time
        CoroutineScope(Dispatchers.IO).launch {
            val currentTime = Calendar.getInstance().timeInMillis / 1000
            if (riderSessionViewModel.sessionVerificationInfo.timeAuth != null) {
                var lastAuthenTime = riderSessionViewModel.sessionVerificationInfo.timeAuth!! / 1000
                val missingTime = currentTime - lastAuthenTime
                Logger.i("missingTimeAuthData: $missingTime")
                if (missingTime >= TIME_CHECKING_MISSING_AUTHEN_DATA) {
                    notifyMissingAuthenticationData(missingTime = missingTime)
                }
            } else if (riderSessionViewModel.getLastAuthTime() != null) {
                val lastAuthenTime = riderSessionViewModel.getLastAuthTime()!! / 1000
                val missingTime = currentTime - lastAuthenTime
                Logger.i("missingTimeAuthData: $missingTime")
                if (missingTime >= TIME_CHECKING_MISSING_AUTHEN_DATA) {
                    notifyMissingAuthenticationData(missingTime = missingTime)
                }
            } else {
                val startTime = riderSessionViewModel.getSessionStartTime() ?: Utils.getRealTimeStamp()
                val currentTime = Utils.getRealTimeStamp()
                val missingTimeAuthData = (currentTime - startTime)/1000
                Logger.i("missingTimeAuthData: $missingTimeAuthData")
                if (missingTimeAuthData >= TIME_CHECKING_MISSING_AUTHEN_DATA) {
                    notifyMissingAuthenticationData(missingTime = missingTimeAuthData)
                }
            }
        }
    }
    private fun notifyMissingAuthenticationData(missingTime: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            val interruptedTime: Int = (missingTime/60).toInt()
            val sessionContinues = riderSessionViewModel.canContinueSessionAfterDisruption(interruptedTime = interruptedTime)
            val message = getString(
                R.string.missing_image_detected_message,
                (missingTime / 60).toInt()
            )
            LogRecorder.i("", getString(
                R.string.missing_image_detected_message,
                (missingTime / 60).toInt()
            ))
            withContext(Dispatchers.Main) {
                showDialog(
                    title = getString(R.string.title_notification),
                    message = message,
                    cancelable = false,
                    buttonList = if (sessionContinues) {
                        listOf(getString(R.string.quit_session), getString(R.string.deny_bt))
                    } else {
                        listOf(getString(R.string.quit_session))
                    },
                    listener = object : DialogButtonClickListener {
                        override fun onDialogButtonClick(position: Int) {
                            dismissDialog()
                            if (position == 0) {
                                handleFinishRiderSession(sessionContinues = sessionContinues)
                            }
                        }
                    }
                )
            }
            delay(1000)
            withContext(Dispatchers.Main) {
                BaseNotification.showWarning(message, showToast = false)
            }
        }
    }

    private fun recognizeUserFaceBlock() {
        CoroutineScope(Dispatchers.IO).launch(
            CoroutineExceptionHandler { _, _ ->
                Logger.w("Error in progress -> handleRecognizeFaceDetected")
            }
        ) {
            Logger.d("start handleCollectFaceDetected")
            // reset recognizeFaceTimeDuration to deault
            recognizeFaceTimeDuration = TIME_FREQUENCY_FACE_RECOGNITION
            verifyFailCounter = 0
            faceInGuideSeenThisCycle = false

            notifyUse4G()
            notifyErrorVelocity()
            handleRecognizeFaceDetected1()
        }
    }
    private suspend fun notifyUse4G() {
        withContext(Dispatchers.Main) {
            val connManager = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)?.isConnected == true
            if (mWifi && riderSessionViewModel.notifyUse4G) {
                BaseNotification.showWarning(getString(R.string.only_use_4g_message), showToast = false)
            }
        }
    }
    private fun notifyUserSessionParamNotDisplayed() {
        CoroutineScope(Dispatchers.Default).launch {
            var counter = 0
            val currentTime = Calendar.getInstance().timeInMillis / 1000
            var sessionStartTime = riderSessionViewModel.getSessionStartTime()?.div(1000)
            while (viewBinding.tvLastCheck.text == "-/-" || viewBinding.tvVerifySuccessPercentage.text == "-/-%") {
                if (sessionStartTime != null) {
                    if (currentTime - sessionStartTime >= TIME_CHECKING_PARAM_NOT_DISPLAY) {
                        while (counter < 3) {
                            counter++
                            withContext(Dispatchers.Main) {
                                BaseNotification.showWarning(getString(R.string.param_session_not_display))
                            }
                            delay(1000)
                        }
                        LogRecorder.i("Thông báo:", getString(R.string.param_session_not_display))
                    }
                }
                // delay for check param in session
                delay(10000)
            }
        }
    }

    private fun notifyErrorVelocity() {
        var zeroVelocityCount = 1
        CoroutineScope(Dispatchers.Default).launch {
            while (zeroVelocityCount in 1 until timeTrackingVelocity && riderSessionViewModel.notifyErrorVelocity) {
                if (riderSessionViewModel.sessionVerificationInfo.getSpeed() < 3) {
                    zeroVelocityCount += 1
                } else {
                    zeroVelocityCount = 0
                }
                if (zeroVelocityCount >= timeTrackingVelocity) {
                    if (isAdded) {
                        withContext(Dispatchers.Main) {
                            BaseNotification.showWarning(getString(R.string.velocity_error))
                        }
                        LogRecorder.w("Thông báo: ", getString(R.string.velocity_error))
                    }
                }
                delay(1000)
            }
        }
    }

    private suspend fun handleRecognizeFaceDetected1() {
        // check in 30s
        val checkingTime = FACE_RECOGNITION_CHECKING_TIME
        val startTime = Utils.getRealTimeStamp()
        var resultCheck = false
        var recognizeFail = false
        var notFaceCounter = 0
        var notFaceMatchingCounter = 0
        var wearMaskCounter = 0
        var lastFaceRecognized: FaceImageData? = null
        var pendingFrame: FaceImageData? = null
        var pendingCaptureTime = 0L
        val temporalConfirmDelay = 1000L
        faceMatching = false
        searchScore = 0f
        recognitionJob?.cancel()
        recognitionJob = CoroutineScope(Dispatchers.Default).launch {
            while ((Utils.getRealTimeStamp() - startTime < checkingTime) && !resultCheck && isActive) {
                //reset recognition score
                if (faceDetectedMessageQueue.isEmpty()) {
                    notFaceCounter++
                    if (notFaceCounter >= 20 && !FinishSessionDialog.isShowing() && !ConFirmLogoutDialog.isShowing()) {
                        notFaceCounter = 0
                        val outsideGuideRecent = Utils.getRealTimeStamp() - lastFaceOutsideGuideTime < 3000
                        val messageRes = if (outsideGuideRecent) R.string.student_not_in_recognition_frame else R.string.student_not_in_camera
                        Logger.w("Not face detected in guide! outsideGuideRecent=$outsideGuideRecent")
                        withContext(Dispatchers.Main) {
                            BaseNotification.showWarning(
                                getString(messageRes),
                                priority = Priority.HIGH,
                                showToast = false
                            )
                            LogRecorder.w("Thông báo: ", getString(messageRes))
                        }
                    }
                    delay(500)
                } else {
                    notFaceCounter = 0
                    val faceDetectedMessage: FaceImageData? = faceDetectedMessageQueue.take()
                    LogRecorder.i("Kiểm tra: ", "Dữ liệu ảnh: ${faceDetectedMessage != null}")
                    if (faceDetectedMessage != null) {
                        val frameBitmap = faceRecognitionViewModel.cameraPreviewDataToBitmap(faceDetectedMessage.nv21ImageData)
                        faceRecognitionViewModel.facialAnalysis(
                            frameBitmap,
                            studentAuthInfo?.userCode
                        )
                        lastFaceRecognized = faceDetectedMessage
                        val result = searchScore >= searchThreshold
                        Logger.i("handleRecognizeFaceDetected result: $result")
                        if (!notMask) {
                            pendingFrame = null
                            ++wearMaskCounter
                            if (wearMaskCounter > 10) {
                                wearMaskCounter = 0
                                withContext(Dispatchers.Main) {
                                    BaseNotification.showWarning(getString(
                                        R.string.no_wearing_mask
                                    ))
                                }
                            }
                        } else if (result) {
                            if (pendingFrame == null) {
                                pendingFrame = faceDetectedMessage
                                pendingCaptureTime = Utils.getRealTimeStamp()
                                Logger.i("Nhận dạng lần 1 pass, chụp và chờ xác nhận. Score: $searchScore")
                            } else if (Utils.getRealTimeStamp() - pendingCaptureTime >= temporalConfirmDelay) {
                                Logger.i("Verify face success (temporal confirmed)")
                                LogRecorder.i("NHẬN DIỆN THÀNH CÔNG", studentAuthInfo?.fullName)
                                notFaceMatchingCounter = 0
                                if (verifyFailCounter > 0 || recognizeFail) {
                                    withContext(Dispatchers.Main) {
                                        BaseNotification.showMessage(getString(
                                            R.string.face_verify_success
                                        ))
                                    }
                                }
                                lastFaceRecognized = pendingFrame
                                resultCheck = true
                                verifyFailCounter = 0
                                faceMatching = true
                            }
                        } else {
                            if (pendingFrame != null) {
                                Logger.w("Xác nhận thất bại (frame trước pass, frame này fail). Reset pending.")
                                pendingFrame = null
                            }
                            // notify not face matching
                            ++notFaceMatchingCounter
                            if (notFaceMatchingCounter > 10) {
                                Logger.w("Not face matching!")
                                recognizeFail = true
                                notFaceMatchingCounter = 0
                                withContext(Dispatchers.Main) {
                                    BaseNotification.showWarning(
                                        getString(R.string.warning_verify_fail_counter),
                                        priority = Priority.HIGH,
                                        showToast = false
                                    )
                                    LogRecorder.i("Thông báo: ","${getString(R.string.warning_verify_fail_counter)}: $searchScore ")
                                }
                            }
                        }
                    }
                }
            }
            if (!resultCheck) ++verifyFailCounter // increase flag
            // try 3 times if fail
            if (!resultCheck && verifyFailCounter <= 3) {
                withContext(Dispatchers.Main) {
                    BaseNotification.showWarning(getString(
                        R.string.warning_verify_fail_counter
                    ))
                    LogRecorder.i("Thông báo: ", getString(
                        R.string.warning_verify_fail_counter
                    ))
                }
                handleRecognizeFaceDetected1()
            } else {
                recognitionJob?.cancel()
                // reset flag
                verifyFailCounter = 0
                handleRecognizeResult(
                    imageFaceRecognized = lastFaceRecognized,
                    resultCheck = resultCheck
                )
            }
        }
    }

    private fun collectFaceDetectedBlock() {
        CoroutineScope(Dispatchers.IO).launch(
            CoroutineExceptionHandler { _, ex ->
                Logger.w("Error in progress -> collectFaceDetectedBlock ex: ${ex.message}")
            }
        ) {
            Logger.d("collectFaceDetectedBlock")
            if (cameraPreviewDataQueue.isNotEmpty()) {
                val previewData = cameraPreviewDataQueue.take()
                clearImageQueue(previewData = previewData)
                // Convert preview camera to image file
                previewData?.also { previewData ->
                    faceRecognitionViewModel.faceDetect(previewData) { rect ->
                        CoroutineScope(Dispatchers.IO).launch() {
                            val insideGuide = showFacePassFace(rect)
                            if (rect != null && insideGuide) {
                                faceDetectedMessageQueue.offer(FaceImageData(previewData, previewData.nv21Data))
                            } else if (rect == null) {
                                withContext(Dispatchers.Main) {
                                    viewBinding.faceView.clear()
                                    viewBinding.faceView.setShowGuide(true)
                                    viewBinding.faceView.invalidate()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    private fun clearImageQueue(previewData: Nv21ImageData) {
        var nv21ImageData: Nv21ImageData = previewData
        cameraPreviewDataQueue.forEach {
            it
            nv21ImageData = it
        }
        cameraPreviewDataQueue.clear()
        cameraPreviewDataQueue.offer(nv21ImageData)
    }

    private fun updatePinStatusIcon() {
        val iconRes = if (powerManager.isPowerSaveMode) {
            R.drawable.iconunpin  // icon khi bật tiết kiệm pin
        } else {
            R.drawable.iconpin          // icon bình thường
        }
        viewBinding.ivPinStatus.setImageResource(iconRes)
    }

    private fun registerPowerSaveReceiver() {
        Logger.i("PIN_DEBUG registerPowerSaveReceiver called")
        powerSaveReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                Logger.i("PIN_DEBUG Power save broadcast received!")
                if (intent.action == PowerManager.ACTION_POWER_SAVE_MODE_CHANGED) {
                    updatePinStatusIcon()
                }
            }
        }
        val filter = IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
        requireContext().registerReceiver(powerSaveReceiver, filter)
    }

    private fun checkLearningOver10HoursBlock() {
        CoroutineScope(Dispatchers.Main).launch {
            val additionalTime = 15 * 60
            val totalTimeIn24h = riderSessionViewModel.inProgressSession!!.totalTime + (riderSessionViewModel.inProgressSession!!.timeIn24H ?: 0.0)
            val remainingTimeIn24H = LEARNING_TIME_OVER_IN_24H - totalTimeIn24h
            Logger.i("totalTimeIn24h: ${DateUtil.ConvertHms(totalTimeIn24h)}")
            Logger.i("remainingTime: ${DateUtil.ConvertHms(remainingTimeIn24H)}")
            if (remainingTimeIn24H <= 0) {
                LogRecorder.e("Phiên học", getString(
                    R.string.over_time_24h_error, DateUtil.ConvertHms(totalTimeIn24h)
                ))
                BaseNotification.showError(getString(
                    R.string.over_time_24h_error, DateUtil.ConvertHms(totalTimeIn24h)
                ))
                learningTimeOver10HoursDuration = TIME_ERROR_OVER
                delay(TIME_ERROR_OVER)
            } else if (remainingTimeIn24H <= WARNING_TIME_REMAINING_TO_10_MINUTES) {
                LogRecorder.w("Phiên học", getString(
                    R.string.over_time_24h_warning, DateUtil.ConvertHms(totalTimeIn24h)
                ))
                BaseNotification.showWarning(getString(
                    R.string.over_time_24h_warning, DateUtil.ConvertHms(totalTimeIn24h)
                ))
                learningTimeOver10HoursDuration = TIME_ERROR_OVER
            } else if (remainingTimeIn24H <= (WARNING_TIME_REMAINING_TO_30_MINUTES - additionalTime)) {
                LogRecorder.w("Phiên học", getString(
                    R.string.over_time_24h_warning, DateUtil.ConvertHms(totalTimeIn24h)
                ))
                BaseNotification.showWarning(getString(
                    R.string.over_time_24h_warning, DateUtil.ConvertHms(totalTimeIn24h)
                ))
                learningTimeOver10HoursDuration = TIME_WARNING_OVER
            } else {
                learningTimeOver10HoursDuration = FREQUENCY_CHECK_LEARNING_TIME
            }
        }
    }

    private fun checkLearningOver4HoursBlock() {
        learningOverTimeDuration = FREQUENCY_CHECK_LEARNING_TIME
        val additionalTime = 15 * 60
        val remainingTime = LEARNING_TIME_OVER - riderSessionViewModel.inProgressSession!!.totalTime
        Logger.i("totalTime: ${riderSessionViewModel.inProgressSession!!.totalTime}")
        Logger.i("remainingTime: ${DateUtil.ConvertHms(remainingTime)}")
        // If learn more than 3 hours and 55 minutes, then will auto log out
        if (riderSessionViewModel.inProgressSession!!.totalTime >= AUTO_LOGOUT_TIME_IN_SESSION) {
            CoroutineScope(Dispatchers.Main).launch {
                handleFinishRiderSession(sessionContinues = false, autoLogout = true)
            }
        }
        CoroutineScope(Dispatchers.Main).launch {
            if (remainingTime <= 0) {
                LogRecorder.e("Phiên học", getString(
                    R.string.over_time_error,
                    DateUtil.ConvertHms(riderSessionViewModel.inProgressSession!!.totalTime)
                ))
                BaseNotification.showError(getString(
                    R.string.over_time_error,
                    DateUtil.ConvertHms(riderSessionViewModel.inProgressSession!!.totalTime)
                ))
                learningOverTimeDuration = TIME_ERROR_OVER
            } else if (remainingTime <= WARNING_TIME_REMAINING_TO_10_MINUTES) {
                LogRecorder.w("Phiên học", getString(
                    R.string.over_time_warning,
                    DateUtil.ConvertHms(riderSessionViewModel.inProgressSession!!.totalTime)
                ))
                BaseNotification.showWarning(getString(
                    R.string.over_time_warning,
                    DateUtil.ConvertHms(riderSessionViewModel.inProgressSession!!.totalTime)
                ))
                learningOverTimeDuration = TIME_ERROR_OVER
            } else if (remainingTime <= (WARNING_TIME_REMAINING_TO_30_MINUTES - additionalTime)) {
                LogRecorder.w("Phiên học", getString(
                    R.string.over_time_warning,
                    DateUtil.ConvertHms(riderSessionViewModel.inProgressSession!!.totalTime)
                ))
                BaseNotification.showWarning(getString(
                    R.string.over_time_warning,
                    DateUtil.ConvertHms(riderSessionViewModel.inProgressSession!!.totalTime)
                ))
                learningOverTimeDuration = TIME_WARNING_OVER
            } else {
                learningOverTimeDuration = FREQUENCY_CHECK_LEARNING_TIME
            }
        }
    }

    private fun clockLogicBlock() {
        CoroutineScope(Dispatchers.Main).launch {
            viewBinding.tvDateNow.text = Utils.getCurrentDateTime()
            riderSessionViewModel.inProgressSession?.also { inProgressSession ->
                val startTime = riderSessionViewModel.getSessionStartTime() ?: Utils.getRealTimeStamp()
                //convert to seconds
                inProgressSession.totalTime = (Utils.getRealTimeStamp() - startTime).toDouble() / 1000
                viewBinding.tvTotalTime.text = DateUtil.ConvertHms(inProgressSession.totalTime)
                viewBinding.tvTotalTimeInDay.text = inProgressSession.timeIn24H?.let { it1 -> DateUtil.ConvertHms(it1 + inProgressSession.totalTime) }
            }
        }
    }

    private fun deviceStatusCheckingBlock() {
        CoroutineScope(Dispatchers.IO + Job()).launch(
            CoroutineExceptionHandler { _, _ ->
                Logger.w("Error in deviceStatusCheckingBlog!")
                deviceStatusCheckingBlock()
            }
        ) {
            var messageError: String = ""
            withContext(Dispatchers.Main) {
                // 1. Cập nhật icon (lấy từ biến đã có, không gọi API hệ thống nên KHÔNG CRASH)
                updateNetworkIconsUI()
                // 2. Ghi Log theo đúng nghiệp vụ của bạn
                LogRecorder.i("Trạng thái kết nối wifi: ", if(isWifiConnected) "bật" else "tắt")
                LogRecorder.i("Trạng thái kết nối 4G: ", if(isMobileConnected) "bật" else "tắt")

                if (!isWifiConnected && !isMobileConnected) {
                    LogRecorder.e("Hệ thống", "Không có kết nối network")
                    BaseNotification.showWarning(getString(R.string.network_not_available), showToast = false)

                    // Tăng biến đếm lỗi kết nối server vì không có mạng thì không thể kết nối server
                    connectServiceFailCounter++

                    if (connectServiceFailCounter >= CONNECT_SERVICE_FAIL_WARNING) {
                        // Không reset counter ở đây để giữ trạng thái lỗi cho đến khi có mạng lại và gọi service thành công
                        connectServiceFailFlag = true
                        viewBinding.ivServerStatus.setImageResource(R.drawable.iconnonserver)
                    }
                }
                if (Environment.MEDIA_MOUNTED == Environment.getExternalStorageState()) {
                    // Do whatever
                    val bImage = BitmapFactory.decodeResource(resources, R.drawable.iconstorage)
                    viewBinding.ivStorage.setImageBitmap(bImage)
                } else {
                    val bImage = BitmapFactory.decodeResource(resources, R.drawable.iconnonstorage)
                    viewBinding.ivStorage.setImageBitmap(bImage)
                    LogRecorder.e("Hệ thống", "Bộ nhớ lưu trũ full")
                }
                // Tìm cách kiểm tra camera trước có hoạt động không
                if (messageError.isNotBlank()) {
                    BaseNotification.showWarning(messageError, showToast = false)
                    LogRecorder.e("Thông báo: ","$messageError")
                }
                updatePinStatusIcon()
                if (powerManager.isPowerSaveMode) {
                    LogRecorder.i("Trạng thái pin", "Chế độ tiết kiệm pin: bật")
                } else {
                    LogRecorder.i("Trạng thái pin", "Chế độ tiết kiệm pin: tắt")
                }
            }
        }
    }

    private fun checkGPSAvailable() {
        val startTime = Utils.getRealTimeStamp()
        val checkingTime = 20000 // 20 giây (mili giây)
        CoroutineScope(Dispatchers.Default).launch {
            var isGPSAvailable = riderSessionViewModel.checkGPSAvailable(requireActivity())
            withContext(Dispatchers.Main) {
                viewBinding.ivGpsStatus.setImageResource(
                    if (isGPSAvailable) R.drawable.icongps else R.drawable.iconnongps
                )
            }

            while (!isGPSAvailable && Utils.getRealTimeStamp() - startTime < checkingTime) {
                delay(1000)
                isGPSAvailable = riderSessionViewModel.checkGPSAvailable(requireActivity())
                if (isGPSAvailable) {
                    return@launch
                }
            }

            withContext(Dispatchers.Main) {
                if (!isGPSAvailable) {
                    viewBinding.ivGpsStatus.setImageResource(R.drawable.iconnongps)
                    BaseNotification.showWarning(getString(R.string.gps_not_available))
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        // Inflate the layout for this fragment
        viewBinding = ScreenTrainingSessionBinding.inflate(inflater, container, false)
        applicationViewModel = ViewModelProviders.of(requireActivity(), viewModelFactory)[ApplicationViewModel::class.java]
        riderSessionViewModel = ViewModelProviders.of(requireActivity(), viewModelFactory)[RiderSessionViewModel::class.java]
        faceRecognitionViewModel = ViewModelProviders.of(requireActivity(), viewModelFactory)[FaceRecognitionViewModel::class.java]
        cameraPreviewDevice = faceRecognitionViewModel.getCameraPreviewDevice()!!
        viewBinding.teacherInfo = riderSessionViewModel.teacherAuthInfo
        searchThreshold = appViewModel.searchThreshold
        riderSessionViewModel.teacherAuthInfo?.avatarId?.also {
            val request = ImageRequest.Builder(requireContext())
                .data("${ServiceDefinition.IMAGE_FULL_SIZE_URL}$it")
                .setHeader("User-Agent", "Mozilla/5.0")
                .crossfade(true)
                .placeholder(R.drawable.ic_loading)
                .allowHardware(false)
                .target(viewBinding.ivTeacherAvatar)
                .build()
            ImageLoader.imageLoader?.enqueue(request)
        }
            ?: also { viewBinding.ivTeacherAvatar.setImageDrawable(requireContext().getDrawable(R.drawable.nonavatar)) }
        nfcAvailable = appViewModel.checkNFCAvailable()
        initView()
        powerManager = requireContext().getSystemService(Context.POWER_SERVICE) as PowerManager
        registerPowerSaveReceiver()
        updatePinStatusIcon() // kiểm tra trạng thái ban đầu
        checkStudentContinueSession()
        LogRecorder.d("", "Màn hình phiên học")
        return viewBinding.root
    }

    private fun initView() {
        viewBinding.tvSerialNumber.text = getString(R.string.serial_value, riderSessionViewModel.getImeiDevice(requireContext()))
        viewBinding.tvVehiclePlate.text = getString(R.string.vehicle_value, appViewModel.getPlateSlug())
        viewBinding.tvTrainingCenterName.text = appViewModel.getTrainingCenterName() ?: "-/-"
        viewBinding.faceView.setGuideMarginTop(0.02f)
        viewBinding.faceView.setGuideMarginHorizontal(0.02f)
        viewBinding.faceView.setShowGuide(true)
        viewBinding.rgNightMode.setOnCheckedChangeListener { _, checked ->
            autoChangeNightMode = false
            LogRecorder.d("", "Bật chế độ ban đêm")
            if (checked) {
                viewBinding.nightMode = true
            }
        }
        viewBinding.rgDayMode.setOnCheckedChangeListener { _, checked ->
            LogRecorder.d("", "Bật chế độ ban ngày")
            autoChangeNightMode = false
            if (checked) {
                viewBinding.nightMode = false
            }
        }

        viewBinding.navigationBt.setOnClickListener {
            requireActivity().drawerLayout?.openDrawer(Gravity.LEFT)
        }

        viewBinding.btLoginRfid.setOnClickListener {
            LogRecorder.d("", "Đăng nhập học viên bằng thẻ")
            loginRfid = true
            if(appViewModel.allowOfflineStartSession || riderSessionViewModel.isConnectionAvailable()){
                if (byPassCheckSpeed || riderSessionViewModel.checkCarPause()) {
                    // Goto login by rfid
                    startNFCReading()
                    NFCLoginDialog.showDialog(
                        requireActivity(),
                        isTeacher = false,
                        cameraRotation = cameraRotation,
                        faceRecognitionViewModel = faceRecognitionViewModel,
                        callback = avatarLoginCaptureCallback
                    )
                } else {
                    LogRecorder.e("", getString(R.string.login_method_not_ready))
                    Logger.w("Warning: Login progress is not ready!")
                    showDialog(
                        title = getString(R.string.title_notification),
                        message = getString(R.string.login_method_not_ready),
                        buttonList = listOf(getString(R.string.ok)),
                        listener = object : DialogButtonClickListener {
                            override fun onDialogButtonClick(position: Int) {
                                dismissDialog()
                            }
                        }
                    )
                }
            }else{
                showDialog(
                    title = getString(R.string.title_notification),
                    message = getString(R.string.start_session_method_not_ready),
                    buttonList = listOf(getString(R.string.ok)),
                    listener = object : DialogButtonClickListener {
                        override fun onDialogButtonClick(position: Int) {
                            dismissDialog()
                        }
                    }
                )
            }
        }

        viewBinding.btLoginFace.setOnClickListener {
            LogRecorder.d("", "Đăng nhập học viên bằng khuôn mặt")
            loginRfid = false
            if(!appViewModel.allowOfflineStartSession){
//                if(appViewModel.allowOfflineStartSession || riderSessionViewModel.isConnectionAvailable()){
                if (byPassCheckSpeed || riderSessionViewModel.checkCarPause()) {
                    if (faceRecognitionViewModel.getCameraPreviewDevice() != null) {
                        // Goto login by face recognize
                        FaceRecognizeDialog.showDialog(
                            requireActivity(),
                            isTeacherLogin = false,
                            cameraRotation,
                            appViewModel,
                            faceRecognitionViewModel.getCameraPreviewDevice()!!,
                            faceRecognitionViewModel,
                            faceRecognizeLoginCallback
                        )
                    } else {
                        Logger.e("Can not get camera preview!!!")
                    }
                } else {
                    LogRecorder.e("", getString(R.string.login_method_not_ready))
                    Logger.w("Warning: Login progress is not ready!")
                    showDialog(
                        title = getString(R.string.title_notification),
                        message = getString(R.string.login_method_not_ready),
                        buttonList = listOf(getString(R.string.ok)),
                        listener = object : DialogButtonClickListener {
                            override fun onDialogButtonClick(position: Int) {
                                dismissDialog()
                            }
                        }
                    )
                }
            } else{
                showDialog(
                    title = getString(R.string.title_notification),
                    message = getString(R.string.start_session_method_not_ready),
                    buttonList = listOf(getString(R.string.ok)),
                    listener = object : DialogButtonClickListener {
                        override fun onDialogButtonClick(position: Int) {
                            dismissDialog()
                        }
                    }
                )
            }
        }

        viewBinding.btLogOutTeacher.setOnClickListener {
            LogRecorder.d("", "Đăng xuất giảng viên")
            if (byPassCheckSpeed || riderSessionViewModel.checkCarPause()) {
                if (studentAuthInfo != null) {
                    LogRecorder.w("", getString(R.string.teacher_can_not_logout))
                    showDialog(
                        title = getString(R.string.title_notification),
                        message = getString(R.string.teacher_can_not_logout),
                        buttonList = listOf(getString(R.string.ok)),
                        listener = object : DialogButtonClickListener {
                            override fun onDialogButtonClick(position: Int) {
                                dismissDialog()
                            }
                        }
                    )
                } else {
                    handleLogoutTeacher()
                }
            } else {
                Logger.w("Warning: Login progress is not ready!")
                LogRecorder.e("", getString(R.string.logout_method_not_ready))
                showDialog(
                    title = getString(R.string.title_notification),
                    message = getString(R.string.logout_method_not_ready),
                    buttonList = listOf(getString(R.string.ok)),
                    listener = object : DialogButtonClickListener {
                        override fun onDialogButtonClick(position: Int) {
                            dismissDialog()
                        }
                    }
                )
            }
        }

        viewBinding.btFinishRiderSession.setOnClickListener {
            LogRecorder.d("", "Kết thúc phiên học")
            if(appViewModel.allowOfflineFinishSession || riderSessionViewModel.isConnectionAvailable()){
                if (byPassCheckSpeed || riderSessionViewModel.checkCarPause()) {
                    handleFinishRiderSession()
                } else {
                    Logger.w("Warning: Login progress is not ready!")
                    LogRecorder.e("", getString(R.string.logout_method_not_ready))
                    showDialog(
                        title = getString(R.string.title_notification),
                        message = getString(R.string.logout_method_not_ready),
                        buttonList = listOf(getString(R.string.ok)),
                        listener = object : DialogButtonClickListener {
                            override fun onDialogButtonClick(position: Int) {
                                dismissDialog()
                            }
                        }
                    )
                }
            }else{
                showDialog(
                    title = getString(R.string.title_notification),
                    message = getString(R.string.finish_session_method_not_ready),
                    buttonList = listOf(getString(R.string.ok)),
                    listener = object : DialogButtonClickListener {
                        override fun onDialogButtonClick(position: Int) {
                            dismissDialog()
                        }
                    }
                )
            }
        }

        viewBinding.rlCurrentSpeed.setOnClickListener{
            animateSpeedChange(
                startValue = viewBinding.tvCurrentSpeed.text.trim().toString().toIntOrNull() ?: 0,
                stopValue = 0
            )
            byPassCheckSpeed = true
            // After 5 seconds without logout or login, byPassCheckSpeed = false
            GlobalScope.launch {
                delay(5000)
                byPassCheckSpeed = false
            }
        }

        viewBinding.ivAvatarStudent.setOnClickListener {
            showProgressDialog()
            riderSessionViewModel.inProgressSession?.studentCode?.let { it1 ->
                appViewModel.getUserInfoByUserCode(
                    it1, false, updateFaceSampleCallback)
            }
        }
    }

    private fun handleLogoutTeacher() {
        riderSessionViewModel.dropTeachOutWorking()
        viewBinding.teacherInfo = null
        requireActivity().onBackPressed()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun handleFinishRiderSession(sessionContinues: Boolean = true, autoLogout: Boolean = false) {
        pauseHandleProcess()
        FinishSessionDialog.showDialog(
            requireActivity(),
            cameraRotation = cameraRotation,
            userEntity = studentAuthInfo!!,
            inProgressSession = riderSessionViewModel.inProgressSession!!,
            faceRecognitionViewModel = faceRecognitionViewModel,
            riderSessionViewModel = riderSessionViewModel,
            applicationViewModel = applicationViewModel,
            callback = finishSessionConfirmCallback,
            sessionContinues = sessionContinues,
            autoLogout = autoLogout
        )
    }

    private fun checkStudentContinueSession() {
        if (appViewModel.studentAutoLogin(appCallback)) {
            showProgressDialog()
        }
    }

    private val faceRecognizeLoginCallback: (action: FaceRecognizeLoginAction, data: Any?, imageFile: File?)
    -> Any = { action: FaceRecognizeLoginAction, data: Any?, imageFile: File? ->
        Logger.d("faceRecognizeLoginCallback action: $action | data: $data")
        CoroutineScope(Dispatchers.Main).launch {
            FaceRecognizeDialog.dismiss()
            when (action) {
                FaceRecognizeLoginAction.FACE_LOGIN_SUCCESS -> {
                    studentImageLogin = imageFile
                    if (studentImageLogin == null) {
                        LogRecorder.e("Đăng nhập học viên thất bại", getString(R.string.student_image_login_not_found))
                        showDialog(
                            title = getString(R.string.title_notification),
                            message = getString(R.string.student_image_login_not_found),
                            buttonList = listOf(getString(R.string.ok)),
                            listener = object : DialogButtonClickListener {
                                override fun onDialogButtonClick(position: Int) {
                                    dismissDialog()
                                }
                            }
                        )
                    } else {
                        isSentGeneral = true
                        val userEntity = data as UserEntity
                        BaseNotification.showWarning(getString(R.string.confirm_user_info_message))
                        Logger.i("Hoang1: " + getString(R.string.confirm_user_info_message))
                        LogRecorder.i("Thông báo: ", getString(R.string.confirm_user_info_message))
                        UserInfoDialog.showDialog(
                            requireContext(),
                            userEntity,
                            captureImagePath = studentImageLogin?.path
                        ) { confirm, data ->
                            showProgressDialog()
                            if (confirm) {
                                if (appViewModel.allowOfflineStartSession || riderSessionViewModel.isConnectionAvailable()) {// Set student info and next to Rider Session screen
                                    studentAuthInfo = userEntity
                                    studentAuthInfo?.lastLoginType = LoginType.FACE.code
                                    riderSessionViewModel.pushStudentInSession(studentAuthInfo!!)
                                    studentAuthInfo?.courseId?.also {
                                        // check has car linked device existed in course or not
                                        riderSessionViewModel.getCarsByImeiAndCourse(
                                            idCourse = it,
                                            callback = riderSessionCallback
                                        )
                                    } ?: run {
                                        dismissProgress()
                                        // clear student Info
                                        studentAuthInfo = null
                                        riderSessionViewModel.dropStudentOutSession()
                                        LogRecorder.e(
                                            "Đăng nhập học viên thất bại",
                                            getString(R.string.student_empty_course)
                                        )
                                        dismissProgress()
                                        showDialog(
                                            title = getString(R.string.title_notification),
                                            message = getString(R.string.student_empty_course),
                                            buttonList = listOf(getString(R.string.ok)),
                                            listener = object : DialogButtonClickListener {
                                                override fun onDialogButtonClick(position: Int) {
                                                    dismissDialog()
                                                }
                                            }
                                        )
                                    }
                                } else {
                                    dismissProgress()
                                    showDialog(
                                        title = getString(R.string.title_notification),
                                        message = getString(R.string.start_session_method_not_ready),
                                        buttonList = listOf(getString(R.string.ok)),
                                        listener = object : DialogButtonClickListener {
                                            override fun onDialogButtonClick(position: Int) {
                                                dismissDialog()
                                            }
                                        }
                                    )
                                }
                            } else {
                                dismissProgress()
                                riderSessionViewModel.dropStudentOutSession()
                            }
                        }
                    }
                }
                FaceRecognizeLoginAction.FACE_IMAGE_SAMPLE_NOT_QUALITY,
                FaceRecognizeLoginAction.FACE_IMAGE_SAMPLE_NOT_EXIST -> {
                    LogRecorder.e("Đăng nhập học viên thất bại", getString(R.string.student_not_have_sample_image))
                    showDialog(
                        title = getString(R.string.title_notification),
                        message = getString(R.string.student_not_have_sample_image),
                        buttonList = listOf(getString(R.string.skip_bt), getString(R.string.setup_face_bt)),
                        listener = object : DialogButtonClickListener {
                            override fun onDialogButtonClick(position: Int) {
                                dismissDialog()
                                if (position == 1) {
                                    val userEntity = data as UserEntity
                                    val bundle = bundleOf(
                                        "user_info" to userEntity
                                    )
                                    viewBinding.root.findNavController().navigate(
                                        R.id.action_trainingSessionScreen_to_registerFaceRecognizeScreen,
                                        bundle
                                    )
                                }
                            }
                        }
                    )
                }
                FaceRecognizeLoginAction.USER_INFO_NOT_EXIST -> {
                    LogRecorder.e("Đăng nhập học viên thất bại", getString(R.string.student_not_found))
                    showDialog(
                        title = getString(R.string.title_notification),
                        message = getString(R.string.student_not_found),
                        buttonList = listOf(getString(R.string.ok)),
                        listener = object : DialogButtonClickListener {
                            override fun onDialogButtonClick(position: Int) {
                                dismissDialog()
                            }
                        }
                    )
                }
                FaceRecognizeLoginAction.REQUIRED_LOGIN_STUDENT -> {
                    LogRecorder.e("Đăng nhập học viên thất bại", getString(R.string.login_student_type_required))
                    showDialog(
                        title = getString(R.string.login_wrong_type),
                        message = getString(R.string.login_student_type_required),
                        buttonList = listOf(getString(R.string.ok)),
                        listener = object : DialogButtonClickListener {
                            override fun onDialogButtonClick(position: Int) {
                                dismissDialog()
                            }
                        }
                    )
                }
                else -> {
                    Logger.w("Action $action not yet handle!")
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun setupRiderSession() {
        Logger.d("setupRiderSession")
        riderSessionViewModel.saveInProgressSession(riderSessionViewModel.inProgressSession!!.id)
        // make folder save images recognition student face
        val studentFolder = File(hcImageFolder, studentAuthInfo?.userCode ?: "unknown-user")
        if (!studentFolder.exists()) {
            studentFolder.mkdirs()
        }
        studentSessionFolder = File(studentFolder, riderSessionViewModel.inProgressSession?.id ?: "unknown-session")
        if (studentSessionFolder?.exists() == false) {
            studentSessionFolder?.mkdirs()
        }
        viewBinding.studentInfo = studentAuthInfo
        viewBinding.tvCourseCode.text = getString(R.string.course_value, studentAuthInfo?.courseCode)
        viewBinding.tvDriverLicense.text = getString(R.string.level_value, studentAuthInfo?.courseLicense)
        val totalDistanceInCourse: Float = studentAuthInfo?.totalCourseDistance ?: 0.0F
        var totalDistanceDone: Float = studentAuthInfo?.totalDistanceRode ?: 0.0F
        var totalDistanceRemaining: Int = ((totalDistanceInCourse - totalDistanceDone) / 1000.0).toInt()
        if (totalDistanceRemaining < 0) totalDistanceRemaining = 0
        val totalDistanceDoneValue: Int = (totalDistanceDone / 1000.0).toInt()
        val totalTimeInCourse: Double = ((studentAuthInfo?.totalCourseTime ?: 0.0) * 60)
        val totalTimeDone: Double = ((studentAuthInfo?.totalTimeStudied ?: 0.0) * 60)
        LogRecorder.i("thời gian và quãng đường đã học","quãng đường: ${studentAuthInfo?.totalDistanceRode}km, thời gian: ${studentAuthInfo?.totalTimeStudied}")
        var totalTimeRemaining = totalTimeInCourse - totalTimeDone
        if (totalTimeRemaining < 0) totalTimeRemaining = 0.0
        viewBinding.tvTotalDistanceRemaining.text = getString(R.string.counter_distance_value, totalDistanceRemaining.toString())
        viewBinding.tvTotalDistanceComplete.text = getString(R.string.counter_distance_value, totalDistanceDoneValue.toString())
        viewBinding.tvTotalTimeRemaining.text = DateUtil.ConvertHms(totalTimeRemaining)
        viewBinding.tvTotalTimeComplete.text = DateUtil.ConvertHms(totalTimeDone)
        riderSessionViewModel.inProgressSession?.also {
            viewBinding.tvTotalAutoTime.text = String.format("%.2f GIỜ", (((it.automaticTransmissionTime ?: 0.0) / 3600 * 100).toInt() / 100.0))
            viewBinding.tvTotalNightTime.text = String.format("%.2f GIỜ", (((it.nightTime ?: 0.0) / 3600 * 100).toInt() / 100.0))
        }

        loadImageAvatar()
        openCamera()
        updateVerifyResultCounter()
        notifyUserSessionParamNotDisplayed()
    }

    private fun loadImageAvatar() {
        studentAuthInfo?.avatarId?.also {
            val request = ImageRequest.Builder(requireContext())
                .data("${ServiceDefinition.IMAGE_FULL_SIZE_URL}$it")
                .setHeader("User-Agent", "Mozilla/5.0")
                .crossfade(true)
                .placeholder(R.drawable.ic_loading)
                .allowHardware(false)
                .target(
                    onStart = { placeholder ->
                        Logger.d("onStart: ${ServiceDefinition.IMAGE_FULL_SIZE_URL}$it")
                    },
                    onSuccess = { result ->
                        Logger.d("onSuccess: ${ServiceDefinition.IMAGE_FULL_SIZE_URL}$it")
                        val bitmap: Bitmap? = result.toBitmapOrNull()
                        if (bitmap != null) {
                            viewBinding.ivAvatarStudent.setImageBitmap(bitmap)
                        } else {
                            showDialog(
                                title = getString(R.string.title_notification),
                                message = getString(R.string.can_not_get_face_image),
                                buttonList = listOf(getString(R.string.ok)),
                                listener = object : DialogButtonClickListener {
                                    override fun onDialogButtonClick(position: Int) {
                                        dismissDialog()
                                    }
                                }
                            )
                        }
                    },
                    onError = { error ->
                        Logger.e("onError studentAuthInfo?.authenImages ${ServiceDefinition.IMAGE_FULL_SIZE_URL}$it Error!!")
                        viewBinding.ivAvatarStudent.setImageDrawable(requireContext().getDrawable(R.drawable.nonavatar))
                        Logger.e("Call reload authenImages ${ServiceDefinition.IMAGE_FULL_SIZE_URL}$it Error!!")
                        loadImageAvatar()
                    }
                ).build()
            ImageLoader.imageLoader?.enqueue(request)
        }
            ?: also { viewBinding.ivAvatarStudent.setImageDrawable(requireContext().getDrawable(R.drawable.nonavatar)) }
    }

    private val cameraPreviewEvent = object : CameraPreviewEvent {
        override fun onTakenPicture(
            handleStatus: CameraHandlerStatus,
            imageData: Nv21ImageData?
        ) {
            imageData?.also {
                cameraPreviewDataQueue.offer(imageData)
            }
        }

        override fun extraCameraChangeState(state: ExtraCameraState, device: UsbDevice?) {
            Logger.d("extraCameraChangeState state: $state | device: $device")
            when (state) {
                ExtraCameraState.CAMERA_OPEN -> {
                    // must call start preview for extra camera
                    LogRecorder.d("Thiết bị", "Camera ngoài đang mở")
                    cameraPreviewDevice.stopCameraPreview()
                    viewBinding.extraCamera = true
                    cameraPreviewDevice.startExtraCamera(
                        aspectRatioSurfaceView = viewBinding.surfaceView,
                        event = this
                    )
                }
                ExtraCameraState.CAMERA_DEVICE_ATTACH -> {
                    LogRecorder.d("Thiết bị", "Camera ngoài đang mở")
                }
                ExtraCameraState.CAMERA_DEVICE_DETACH -> {
                    LogRecorder.d("Thiết bị", "Camera ngoài ngắt kết nối")
                    viewBinding.extraCamera = false
                    cameraPreviewDevice.startInternalCamera(
                        internalCameraPreview = viewBinding.previewCamera,
                        windowManager = requireActivity().windowManager,
                        event = this
                    )
                }
                else -> {
                    Logger.w("Not handle state: $state")
                }
            }
        }
    }

    private fun openCamera() {
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
        startRecognize()
    }

    private fun pauseHandleProcess() {
        faceRecognitionViewModel.stopRecognition()
        isThreadRunningJob?.cancel(cause = CancellationException("Cancel by pause process!"))
        timeCounterThread?.interrupt()
        sendAuthenDataJob?.cancel(cause = CancellationException("Cancel by pause process!"))
        recognitionJob?.cancel()
        cameraPreviewDevice.stopCameraPreview()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private val finishSessionConfirmCallback: (action: ActionFinishSession, isNotSendTC: Boolean, imageFile: File?)
    -> Unit = { action: ActionFinishSession, isNotSendTC: Boolean, imageFile: File? ->
        Logger.i("finishSessionConfirmCallback action: $action | isNotSendTC: $isNotSendTC")
        CoroutineScope(Dispatchers.Main).launch {
            if(timeCounterThread?.isAlive == false || timeCounterThread == null || timeCounterThread?.isInterrupted == true){
                startTimeCounter()
            }
            if(isThreadRunningJob?.isActive == false || isThreadRunningJob == null){
                checkTimeCounterThread()
            }
            when (action) {
                ActionFinishSession.AUTO_FINISH_SESSION -> {
                    studentImageLogout = imageFile
                    handleCallFinishRiderSession(isNotSendTC)
                }

                ActionFinishSession.FINISH_SESSION -> {
                    studentImageLogout = imageFile
                    if (studentImageLogout == null) {
                        LogRecorder.e("Kết thúc phiên", getString(R.string.student_image_login_not_found))
                        showDialog(
                            title = getString(R.string.title_notification),
                            message = getString(R.string.student_image_login_not_found),
                            buttonList = listOf(getString(R.string.ok)),
                            listener = object : DialogButtonClickListener {
                                override fun onDialogButtonClick(position: Int) {
                                    dismissDialog()
                                }
                            }
                        )
                    }
//                    else {
//                        riderSessionViewModel.inProgressSession?.id?.let { sessionId ->
//                            riderSessionViewModel.checkMissingDataSession(
//                                    sessionId = sessionId,
//                                    callback = { action, data ->
//                                        when (action) {
//                                            RiderSessionAction.CHECK_MISSING_DATA_SUCCESS -> {
//                                                // Không thiếu dữ liệu
//                                                if (data == null) {
//                                                    BaseNotification.showWarning(getString(R.string.confirm_logout_info_message))
//
//                                                    ConFirmLogoutDialog.showDialog(
//                                                            activity = requireActivity(),
//                                                            imageLogin = studentImageLogin ?: riderSessionViewModel.getImageLogin(),
//                                                            imageLogout = studentImageLogout!!,
//                                                            inProgressSession = riderSessionViewModel.inProgressSession!!,
//                                                    ) { confirm, notSendTC ->
//                                                        if (confirm) {
//                                                            FinishSessionDialog.dismiss()
//                                                            handleCallFinishRiderSession(isNotSendTC || notSendTC)
//                                                        }
//                                                    }
//                                                } else {
//                                                    // Có thiếu dữ liệu
//                                                    val missingData = data as Triple<
//                                                            String,
//                                                            List<DateMissing>,
//                                                            List<DateMissing>
//                                                            >
//
//                                                    val missAuthen = missingData.second
//                                                    val missGps = missingData.third
//
//                                                    Logger.d("Thiếu authen: $missAuthen | thiếu gps: $missGps")
//
//                                                    BaseNotification.showWarning(
//                                                            "Phiên học đang thiếu dữ liệu. Không thể xác nhận kết thúc phiên."
//                                                    )
//                                                }
//                                            }
//
//                                            RiderSessionAction.CHECK_MISSING_DATA_FAIL -> {
//                                                BaseNotification.showError(
//                                                        data?.toString() ?: "Không kiểm tra được dữ liệu thiếu"
//                                                )
//                                            }
//
//                                            else -> {}
//                                        }
//                                    }
//                            )
//                        }
//                    }
                    else {
                        riderSessionViewModel.inProgressSession?.id?.let { riderSessionViewModel.checkMissingDataSession(sessionId = it, callback = riderSessionCallback) }
                        LogRecorder.i("LOGIN_IMAGE", "--- Kiểm tra ảnh đăng nhập ---")
                        LogRecorder.i("LOGIN_IMAGE", "studentImageLogin (biến tạm thời): ${studentImageLogin?.absolutePath ?: "NULL"}")
                        LogRecorder.i("LOGIN_IMAGE", "getImageLogin (từ ViewModel/DB): ${riderSessionViewModel.getImageLogin()?.absolutePath ?: "NULL"}")

                        ConFirmLogoutDialog.showDialog(
                            activity = requireActivity(),
                            imageLogin = studentImageLogin ?: riderSessionViewModel.getImageLogin(),
                            imageLogout = studentImageLogout!!,
                            inProgressSession = riderSessionViewModel.inProgressSession!!,
                            faceRecognitionViewModel = faceRecognitionViewModel,
                        ) { confirm, notSendTC ->
                            if (confirm && isAdded) {
                                FinishSessionDialog.dismiss()
                                handleCallFinishRiderSession(isNotSendTC || notSendTC)
                            }
                        }
                    }
                }
                ActionFinishSession.CONTINUES_SESSION -> {
                    if (riderSessionViewModel.teacherAuthInfo != null &&
                        studentAuthInfo != null &&
                        riderSessionViewModel.getSessionInProgress() != null
                    ) {
                        openCamera()
                    }
                }
            }
        }
    }

    private val avatarLoginCaptureCallback: (imageFile: File?)
    -> Unit = { imageFile: File? ->
        Logger.i("avatarLoginCaptureCallback imageFile: $imageFile")
        CoroutineScope(Dispatchers.Main).launch {
            dismissProgress()
            studentImageLogin = imageFile
            if (studentImageLogin == null) {
                showDialog(
                    title = getString(R.string.title_notification),
                    message = getString(R.string.student_image_login_not_found),
                    buttonList = listOf(getString(R.string.ok)),
                    listener = object : DialogButtonClickListener {
                        override fun onDialogButtonClick(position: Int) {
                            dismissDialog()
                        }
                    }
                )
            } else {
                nfcResultData?.also {
                    showProgressDialog()
                    appViewModel.getUserInfoByRfidCode(cardData = it, callback = appCallback)
                }
            }
        }
    }
    private val startRecognition: (
        searchScore: Int,
        faceBitmap: Bitmap?,
        rect: Rect?,
        notFace: Boolean,
        notMask: Boolean
    )
    -> Unit = {searchScore,_, rect , notFace, notMask ->
        this.notFace = notFace
        this.notMask = notMask
        this.searchScore = searchScore.toFloat()
        if (searchScore >= searchThreshold) {
            this.successScore = searchScore.toFloat()
        }
        else this.failScore = searchScore.toFloat()
        Logger.i("startRecognition searchScore:$searchScore, notFace:$notFace, notMask:$notMask")
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        myListener = MyPhoneStateListener(requireActivity())
        telephonyManager = requireContext().getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        telephonyManager?.listen(myListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS)
        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun startNFCReading() {
        Logger.d("startNFCReading")
        appViewModel.startNFCCard(
            nfcEvent = object : NFCEvent {
                override fun onNFCDataDetected(nfcAction: NFCAction, data: String?) {
                    when (nfcAction) {
                        NFCAction.NFC_DISABLE -> {
                            LogRecorder.e("Đăng nhập học viên thất bại", getString(R.string.login_by_nfc_method_note_ready))
                            showDialog(
                                title = getString(R.string.title_notification),
                                message = getString(R.string.login_by_nfc_method_note_ready),
                                buttonList = listOf(getString(R.string.go_setting_enable_nfc_bt)),
                                listener = object : DialogButtonClickListener {
                                    override fun onDialogButtonClick(position: Int) {
                                        dismissDialog()
                                        val intent =
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                                Intent(Settings.Panel.ACTION_NFC)
                                            } else {
                                                Intent(Settings.ACTION_NFC_SETTINGS)
                                            }
                                        startActivity(intent)
                                    }
                                }
                            )
                        }
                        NFCAction.NFC_DATA_INCORRECT -> {
                            LogRecorder.e("Đăng nhập học viên thất bại", getString(R.string.data_nfc_incorrect))
                            showDialog(
                                title = getString(R.string.title_notification),
                                message = getString(R.string.data_nfc_incorrect),
                                buttonList = listOf(getString(R.string.ok)),
                                listener = object : DialogButtonClickListener {
                                    override fun onDialogButtonClick(position: Int) {
                                        dismissDialog()
                                    }
                                }
                            )
                        }
                        NFCAction.NFC_DATA_DETECTED -> {
                            val result: String = data as String
                            LogRecorder.i("Đăng nhập học viên thành công băng thẻ", result)
                            Logger.i("DETECTED_TAG_DATA result: $result")
                            NFCLoginDialog.dismiss()
                            appViewModel.stopNFCCard()
                            showProgressDialog()
                            nfcResultData = data
                        }
                        else -> {}
                    }
                }
            }
        )
    }

    private val gpsEventListener = object : GPSEvent {
        var quantityGPSSatellite = ""
        var gpsChecking: Boolean = false
        var timestampCheckGPS: Int = 0
        var enableGps: Boolean = true
        var statusGps: Boolean = false

        @SuppressLint("SetTextI18n")
        override fun onGPSUpdate(action: GPSAction, data: Any?) {
            Logger.i("onGPSUpdate: action: $action")
            when (action) {
                GPSAction.GPS_DISABLE -> {
                    LogRecorder.e("Hệ thống", getString(R.string.gps_not_available))
                    Logger.w("GPS_DISABLE")
                    BaseNotification.showWarning(getString(R.string.gps_not_available), showToast = false)
                }
                GPSAction.GPS_SETTING_CHANGED -> {
                    enableGps = data as Boolean
                    Logger.i("GPS_SETTING_CHANGED enableGps: $enableGps")
                    LogRecorder.i("Hệ thống", getString(if (enableGps) R.string.gps_available else R.string.gps_not_available))

                    if (!gpsChecking) {
                        gpsChecking = true
                        CoroutineScope(Dispatchers.Default).launch {
                            while (timestampCheckGPS < 30) {
                                ++timestampCheckGPS
                                if (timestampCheckGPS >= 30) {
                                    gpsChecking = false
                                    timestampCheckGPS = 0
                                    withContext(Dispatchers.Main) {
                                        if (!enableGps) {
                                            statusGps = true
                                            isGpsAvailableShown = false
                                            LogRecorder.e("Thông báo", getString(R.string.gps_not_available))
                                            BaseNotification.showWarning(
                                                getString(R.string.gps_not_available),
                                                showToast = false
                                            )
                                            viewBinding.ivGpsStatus.setImageResource(R.drawable.iconnongps)
                                        } else {
                                            if (!isGpsAvailableShown) {
                                                isGpsAvailableShown = true
                                                LogRecorder.i("Thông báo", getString(R.string.gps_available))
                                                BaseNotification.showMessage(getString(R.string.gps_available))
                                                viewBinding.ivGpsStatus.setImageResource(R.drawable.icongps)
                                            }
                                        }
                                    }
                                }

                                if (enableGps) {
                                    gpsChecking = false
                                    timestampCheckGPS = 0
                                    return@launch
                                }
                                delay(1000)
                            }
                        }
                    }
                    if (statusGps) {
                        statusGps = false
                        if (!isGpsAvailableShown) {
                            isGpsAvailableShown = true
                            LogRecorder.i("Thông báo", getString(R.string.gps_available))
                            BaseNotification.showMessage(getString(R.string.gps_available))
                            viewBinding.ivGpsStatus.setImageResource(R.drawable.icongps)
                        }
                    }
                }
                GPSAction.SATELLITE_COUNT_UPDATED -> {
                    quantityGPSSatellite = data as String
                    viewBinding.tvQuantityGPSSatellite.text = quantityGPSSatellite
                }
                GPSAction.LOCATION_UPDATED -> {
                    val location = data as Location
                    Logger.i("accuracy: ${location.accuracy} | elapsedRealtimeNanos: ${location.elapsedRealtimeNanos}  | provider: ${location.provider}")
                    Logger.i("location speed: ${location.speed} | latitude: ${location.latitude} | longitude: ${location.longitude}  | time: ${location.time}")
                    LogRecorder.i("Lấy GPS thành công", "speed: ${location.speed} | latitude: ${location.latitude} | longitude: ${location.longitude}  | time: ${location.time}")
                    LogRecorder.i("Số lượng vệ tinh GPS được sử dụng: ", quantityGPSSatellite)

                    // KIỂM TRA FAKE GPS TẠI ĐÂY
                    if (SecurityUtils.isLocationMock(location)) {
                        SecurityUtils.logFakeGpsToSentry("Vị trí giả lập", "isFromMockProvider = true", studentAuthInfo?.userCode)
                        showFakeGpsBlockDialog("Tọa độ giả lập", "")
                        return
                    }

                    riderSessionViewModel.sessionVerificationInfo.apply {
                        setLastLocation(location)
                    }
                    val info = riderSessionViewModel.sessionVerificationInfo

                    // --- ĐOẠN CODE THAY ĐỔI MÀU SẮC TẠI ĐÂY ---
                    val lat = info.lat
                    val lng = info.long

                    if (lat == 0.0 && lng == 0.0) {
                        viewBinding.tvLastLocation.setTextColor(Color.RED)
                        viewBinding.imgLastLocation.setColorFilter(Color.RED)
                    } else {
                        val normalColor = if (viewBinding.nightMode == true) Color.BLACK else Color.WHITE
                        viewBinding.tvLastLocation.setTextColor(normalColor)
                        viewBinding.imgLastLocation.setColorFilter(normalColor)
                    }

                    viewBinding.tvLastLocation.text = "${info.lat}, ${info.long}"
                    animateSpeedChange(
                        startValue = viewBinding.tvCurrentSpeed.text.trim().toString().toIntOrNull() ?: 0,
                        stopValue = riderSessionViewModel.sessionVerificationInfo.getSpeed().toInt()
                    )
                    ObjectAnimator.ofInt(
                        viewBinding.progressBarSpeed,"progress", riderSessionViewModel.sessionVerificationInfo.getSpeed().toInt()
                    ).setDuration(2000).start()
                    riderSessionViewModel.inProgressSession?.also { inProgressSession ->
                        viewBinding.tvUploadResultImage.text = "${inProgressSession.totalAuthDataUploadSuccess}/${inProgressSession.totalAuthDataUpload}(${1 + Math.floor(inProgressSession.totalTime).toInt()/300})"
                        viewBinding.tvUploadResultImage.setTextColor(if (inProgressSession.totalAuthDataUploadSuccess == inProgressSession.totalAuthDataUpload) Color.GREEN else Color.RED)
                        viewBinding.tvUploadResultGPS.text = "${inProgressSession.totalGPSUploadSuccess}/${inProgressSession.totalGPSUpload}(${1 + Math.floor(inProgressSession.totalTime).toInt()/10})"
                        viewBinding.tvUploadResultGPS.setTextColor(if (inProgressSession.totalGPSUploadSuccess == inProgressSession.totalGPSUpload) Color.GREEN else Color.RED)
                        viewBinding.tvTotalTime.text = DateUtil.ConvertHms(inProgressSession.totalTime)
                        inProgressSession.totalDis += riderSessionViewModel.sessionVerificationInfo.distance
                        val totalDistance: Float = inProgressSession.totalDis / 1000f
                        viewBinding.tvTotalDistance.text = getString(R.string.total_distance_value, (totalDistance * 100).toInt() / 100.0)

                        riderSessionViewModel.pushGPSData(
                            gpsStatus = myListener?.signalNum ?: -1,
                            gsmStatus = myListener?.signalNum ?: -1,
                            callback = riderSessionCallback
                        )

                        riderSessionViewModel.updateDrivingProgress(
                            totalTime = inProgressSession.totalTime,
                            totalDistance = inProgressSession.totalDis
                        )
                    } ?: run {
                        viewBinding.tvTotalTime.text = "-/-"
                        viewBinding.tvTotalDistance.text = "-/-"
                    }
                }
                else -> {
                    Logger.w("GPSAction [$action] not handle")
                }
            }
        }
    }

    fun animateSpeedChange(startValue: Int, stopValue: Int) {
        val valueAnimator: ValueAnimator = ValueAnimator.ofInt(startValue, stopValue)
        valueAnimator.duration = 2000
        valueAnimator.addUpdateListener { valueAnimator ->
            viewBinding.tvCurrentSpeed.text = valueAnimator.animatedValue.toString()
        }
        valueAnimator.start()
    }
    private fun startRecognize(){
        CoroutineScope(Dispatchers.IO).launch {
            riderSessionViewModel.inProgressSession?.studentCode?.let { faceRecognitionViewModel.startRecognition(faceGroupName = it, isFromDialog = false, resultCallback = startRecognition) }
        }
    }
    private val updateFaceSampleCallback: (action: AppAction, data: Any?)
    -> Unit = { action: AppAction, data: Any? ->
        Logger.d("updateFaceSampleCallback action: $action | data: $data")
        dismissProgress()
        when (action) {
            AppAction.GET_USER_INFO_SUCCESS -> {
                LogRecorder.d("Đổi ảnh mẫu: ","")
                val userInfo = data as UserInfo
                CoroutineScope(Dispatchers.Default).launch {
                    faceRecognitionViewModel.addNewUser(userInfo)
                    startRecognize()
                }
                if (userInfo.avatarId != null) {
                    studentAuthInfo?.avatarId = userInfo.avatarId
                    loadImageAvatar()
                }
            }
            else -> {
                Logger.w("Action not handle!")
            }
        }
    }

    private val appCallback: (action: AppAction, data: Any?)
    -> Unit = { action: AppAction, data: Any? ->
        Logger.d("loginCallback action: $action | data: $data")
        dismissProgress()
        when (action) {
            AppAction.GET_USER_INFO_AUTO_SUCCESS -> {
                isStudentContinueSession = true
                val userEntity = (data as UserInfo).convertToModelEntity()
                if (userEntity.userType != UserType.STUDENT.code) {
                    showDialog(
                        title = getString(R.string.login_wrong_type),
                        message = getString(R.string.login_student_type_required),
                        buttonList = listOf(getString(R.string.ok)),
                        listener = object : DialogButtonClickListener {
                            override fun onDialogButtonClick(position: Int) {
                                dismissDialog()
                            }
                        }
                    )
                } else {
                    // Set student info and next to Rider Session screen
                    studentAuthInfo = userEntity
                    riderSessionViewModel.pushStudentInSession(studentAuthInfo!!)
                    studentAuthInfo?.courseId?.also {
                        // check has car linked device existed in course or not
                        riderSessionViewModel.getCarsByImeiAndCourse(
                            idCourse = it,
                            callback = riderSessionCallback
                        )
                    } ?: run {
                        showDialog(
                            title = getString(R.string.title_notification),
                            message = getString(R.string.student_empty_course),
                            buttonList = listOf(getString(R.string.ok)),
                            listener = object : DialogButtonClickListener {
                                override fun onDialogButtonClick(position: Int) {
                                    dismissDialog()
                                }
                            }
                        )
                    }
                }
            }
            AppAction.GET_USER_INFO_SUCCESS -> {
                isStudentContinueSession = false
                val userEntity = (data as UserInfo).convertToModelEntity()
                // expect -> add face sample when login by nfc
                CoroutineScope(Dispatchers.Default).launch {
                    faceRecognitionViewModel.addNewUser(data as UserInfo)
                }

                if (userEntity.userType != UserType.STUDENT.code) {
                    showDialog(
                        title = getString(R.string.login_wrong_type),
                        message = getString(R.string.login_student_type_required),
                        buttonList = listOf(getString(R.string.ok)),
                        listener = object : DialogButtonClickListener {
                            override fun onDialogButtonClick(position: Int) {
                                dismissDialog()
                            }
                        }
                    )
                } else {
                    if (userEntity.avatarId.isNullOrEmpty()) {
                        showDialog(
                            title = getString(R.string.title_notification),
                            message = getString(R.string.student_not_have_sample_image),
                            buttonList = listOf(getString(R.string.skip_bt), getString(R.string.setup_face_bt)),
                            listener = object : DialogButtonClickListener {
                                override fun onDialogButtonClick(position: Int) {
                                    dismissDialog()
                                    if (position == 1) {
                                        val userEntity = data as UserEntity
                                        val bundle = bundleOf("user_info" to userEntity)
                                        viewBinding.root.findNavController().navigate(
                                            R.id.action_trainingSessionScreen_to_registerFaceRecognizeScreen,
                                            bundle
                                        )
                                    }
                                }
                            }
                        )
                    } else {
                        BaseNotification.showWarning(getString(R.string.confirm_user_info_message))
                        Logger.i("Hoang2: " + getString(R.string.confirm_user_info_message))
                        LogRecorder.i("Thông báo: ", getString(R.string.confirm_user_info_message))
                        UserInfoDialog.showDialog(
                            requireContext(),
                            userEntity,
                            captureImagePath = studentImageLogin?.path
                        ) { confirm, data ->
                            UserInfoDialog.dismiss()
                            if (confirm) {
                                if (appViewModel.allowOfflineStartSession || riderSessionViewModel.isConnectionAvailable()) {
                                    // Set student info and next to Rider Session screen
                                    studentAuthInfo = data
                                    riderSessionViewModel.pushStudentInSession(studentAuthInfo!!)
                                    studentAuthInfo?.courseId?.also {
                                        // check has car linked device existed in course or not
                                        riderSessionViewModel.getCarsByImeiAndCourse(
                                            idCourse = it,
                                            callback = riderSessionCallback
                                        )
                                    } ?: run {
                                        dismissProgress()
                                        showDialog(
                                            title = getString(R.string.title_notification),
                                            message = getString(R.string.student_empty_course),
                                            buttonList = listOf(getString(R.string.ok)),
                                            listener = object : DialogButtonClickListener {
                                                override fun onDialogButtonClick(position: Int) {
                                                    dismissDialog()
                                                }
                                            }
                                        )
                                    }
                                } else {
                                    dismissProgress()
                                    showDialog(
                                        title = getString(R.string.title_notification),
                                        message = getString(R.string.start_session_method_not_ready),
                                        buttonList = listOf(getString(R.string.ok)),
                                        listener = object : DialogButtonClickListener {
                                            override fun onDialogButtonClick(position: Int) {
                                                dismissDialog()
                                            }
                                        }
                                    )
                                }
                            } else {
                                riderSessionViewModel.dropStudentOutSession()
                            }
                        }
                    }
                }
            }
            AppAction.GET_USER_INFO_FAIL -> {
                studentAuthInfo = null
                riderSessionViewModel.dropStudentOutSession()
                showDialog(
                    title = getString(R.string.title_notification),
                    message = data as String,
                    buttonList = listOf(getString(R.string.ok)),
                    listener = object : DialogButtonClickListener {
                        override fun onDialogButtonClick(position: Int) {
                            dismissDialog()
                        }
                    }
                )
            }
            else -> {
                Logger.w("Action not handle!")
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    val riderSessionCallback: (action: RiderSessionAction, data: Any?)
    -> Unit = { action: RiderSessionAction, data: Any? ->
        Logger.d("riderSessionCallback action: $action | data: $data")
        LogRecorder.i("RiderSessionCallback: ","action: $action | data: $data")
        when (action) {
            RiderSessionAction.CHECK_STUDENT_AVAILABLE_SUCCESS -> {
                // check teach has in session
                handleGetTeacherInProgressSession(riderSessionViewModel.teacherAuthInfo!!.userCode)
            }
            RiderSessionAction.CHECK_STUDENT_AVAILABLE_FAIL -> {
                dismissProgress()
                // clear student Info
                studentAuthInfo = null
                riderSessionViewModel.dropStudentOutSession()
                val message = data as String
                Logger.i("riderSessionCallback message: $message")
                if (message.isNotBlank()) {
                    BaseNotification.showError(message)
                    LogRecorder.w("Thông báo: ","$message")
                }
            }
            RiderSessionAction.CHECK_SESSION_IN_PROGRESS_BY_TEACHER_PASS -> {
                handlePrepareStartSession()
            }
            RiderSessionAction.CONNECT_SERVICE_ERROR -> {
//                / Check đoạn này xem đã trả về error code tương ứng chưa
                Logger.w("CONNECT_SERVICE_ERROR")
                LogRecorder.i("CONNECT_SERVICE_ERROR: ","action: $action | data: $data")
                dismissProgress()
                ++connectServiceFailCounter
                if (connectServiceFailCounter >= CONNECT_SERVICE_FAIL_WARNING) {
                    // reset counter
                    connectServiceFailCounter = 0
                    connectServiceFailFlag = true
                    BaseNotification.showWarning(getString(R.string.connect_server_error))
                    LogRecorder.e("Thông báo: ", getString(R.string.connect_server_error))
                    viewBinding.ivServerStatus.setImageResource(R.drawable.iconnonserver)
                }
            }
            RiderSessionAction.FETCH_CURRENT_SESSION_FAIL -> {
                // check condition for fix issue show error dialog after current session finished
                if (riderSessionViewModel.inProgressSession != null) {
                    dismissProgress()
                    showDialog(
                        title = getString(R.string.error_title_dialog),
                        message = data as String,
                        cancelable = false,
                        buttonList = listOf(getString(R.string.ok)),
                        listener = object : DialogButtonClickListener {
                            override fun onDialogButtonClick(position: Int) {
                                dismissDialog()
                            }
                        }
                    )
                }
            }
            RiderSessionAction.FORCE_LOGOUT_CURRENT_SESSION_BY_ADMIN -> {
                adminLogoutRequestCount++
                if (riderSessionViewModel.inProgressSession != null) {
                    BaseNotification.showMessage(getString(R.string.logout_by_admin_message))
                    dismissProgress()
                    if (adminLogoutRequestCount >= 3) {
                        handleLogoutByAdmin()
                    } else {
                        showDialog(
                            title = getString(R.string.error_title_dialog),
                            message = data as String,
                            cancelable = false,
                            buttonList = listOf(getString(R.string.ok)),
                            listener = object : DialogButtonClickListener {
                                override fun onDialogButtonClick(position: Int) {
                                    dismissDialog()
                                    handleLogoutByAdmin()
                                }
                            }
                        )
                    }
                }
            }
            RiderSessionAction.FETCH_CURRENT_SESSION_SUCCESS -> {
                val pairData: Pair<Float, Double> = data as Pair<Float, Double>
                Logger.i(
                    "Fetch data session from server totalDistance: ${
                        getString(
                            R.string.total_distance_value,
                            (pairData.first / 1000f)
                        )
                    } | totalTime: ${DateUtil.ConvertHms(pairData.second)}"
                )
                riderSessionViewModel.inProgressSession?.also { inProgressSession ->
                    inProgressSession.totalDis = pairData.first
                    val totalDistance: Float = inProgressSession.totalDis / 1000f
                    viewBinding.tvTotalDistance.text = getString(R.string.total_distance_value, (totalDistance * 100).toInt() / 100.0)
                    inProgressSession.totalTime = pairData.second
                    viewBinding.tvTotalTime.text = DateUtil.ConvertHms(inProgressSession.totalTime)
                    // [DAT CER]: only use for get DAT certification
                    riderSessionViewModel.updateDrivingProgress(
                        totalTime = inProgressSession.totalTime,
                        totalDistance = inProgressSession.totalDis
                    )
                }
                if (connectServiceFailFlag) {
                    // reset counter
                    connectServiceFailCounter = 0
                    connectServiceFailFlag = false
                    BaseNotification.showMessage(getString(R.string.connect_server_success))
                    LogRecorder.i("Thông báo: ", getString(R.string.connect_server_success))
                    viewBinding.ivServerStatus.setImageResource(R.drawable.iconserver)
                }
            }
            RiderSessionAction.PUSH_SESSION_TO_TC_FAIL -> {
                dismissProgress()
                showDialog(
                    title = getString(R.string.error_title_dialog),
                    message = getString(R.string.send_tc_error_message),
                    cancelable = false,
                    buttonList = listOf(getString(R.string.finish_with_out_send_tc)),
                    listener = object : DialogButtonClickListener {
                        override fun onDialogButtonClick(position1: Int) {
                            dismissDialog()
                            if (position1 == 0) {
                                handleCallFinishRiderSession(true)
                            }
                        }
                    }
                )
            }
            RiderSessionAction.FINISH_RIDER_SESSION_FAIL_BY_LOCATION,
            RiderSessionAction.FINISH_RIDER_SESSION_FAIL -> {
                dismissProgress()
//                showDialog(
//                    title = getString(R.string.error_title_dialog),
//                    message = data as String,
//                    cancelable = false,
//                    buttonList = listOf(getString(R.string.retry_button)),
//                    listener = object : DialogButtonClickListener {
//                        override fun onDialogButtonClick(position: Int) {
//                            dismissDialog()
//                            if (riderSessionViewModel.teacherAuthInfo != null &&
//                                studentAuthInfo != null &&
//                                riderSessionViewModel.getSessionInProgress() != null
//                            ) {
//                                openCamera()
//                            }
//                        }
//                    }
//                )
                showDialog(
                    title = getString(R.string.title_notification),
                    message = getString(R.string.finish_session_offline),
                    buttonList = listOf(getString(R.string.ok)),
                    cancelable = false,
                    listener = object : DialogButtonClickListener {
                        override fun onDialogButtonClick(position: Int) {
                            dismissDialog()
                            // handle cancel all job running
                            clearSessionHandler()
                            BaseNotification.showMessage(getString(R.string.finish_session_success))
                            requireActivity().onBackPressed()
                        }
                    }
                )
            }
            // [DAT CER]: only use for get DAT certification
            RiderSessionAction.FINISH_RIDER_SESSION_SUCCESS_OFFLINE -> {
                dismissProgress()
                showDialog(
                    title = getString(R.string.title_notification),
                    message = getString(R.string.finish_session_offline),
                    buttonList = listOf(getString(R.string.ok)),
                    cancelable = false,
                    listener = object : DialogButtonClickListener {
                        override fun onDialogButtonClick(position: Int) {
                            dismissDialog()
                            // handle cancel all job running
                            clearSessionHandler()
                            BaseNotification.showMessage(getString(R.string.finish_session_success))
                            requireActivity().onBackPressed()
                        }
                    }
                )
            }
            // [DAT CER]
            RiderSessionAction.FINISH_RIDER_SESSION_SUCCESS -> {
                handleSessionCompletionEvent()
                val sessionId = data as String
                riderSessionViewModel.checkMissingDataSession1(
                    sessionId = sessionId
                )
            }
            RiderSessionAction.FINISH_RIDER_SESSION_SUCCESS_WITH_ERROR -> {
                val message: String  = data as String
                if (message.isNotEmpty()) {
                    showDialog(
                        title = getString(R.string.title_notification),
                        message = message,
                        cancelable = false,
                        buttonList = listOf(getString(R.string.ok)),
                        listener = object : DialogButtonClickListener {
                            override fun onDialogButtonClick(position: Int) {
                                dismissDialog()
                                if (position == 0) {
                                    handleSessionCompletionEvent()
                                }
                            }
                        }
                    )
                } else {
                    handleSessionCompletionEvent()
                }
            }
            RiderSessionAction.GET_SESSION_IN_PROGRESS_BY_STUDENT_SUCCESS -> {
                val inProgressSession = data as InProgressSession?
                handleCheckSessionInProgress(inProgressSession)
            }
            // [DAT CER]: only use for get DAT certification
            RiderSessionAction.LOCAL_STUDENT_IN_PROGRESSS_SESSION_NOT_MATCH -> {
                dismissProgress()
                showDialog(
                    title = getString(R.string.title_notification),
                    message = getString(R.string.student_not_match_local_inprogress_session),
                    buttonList = listOf(getString(R.string.ok)),
                    listener = object : DialogButtonClickListener {
                        override fun onDialogButtonClick(position: Int) {
                            dismissDialog()
                        }
                    }
                )
            }
            // [DAT CER]
            RiderSessionAction.UPLOAD_IMAGE_CAPTURE_FAIL,
            RiderSessionAction.START_RIDER_SESSION_FAIL_BY_LOCATION,
            RiderSessionAction.START_RIDER_SESSION_FAIL -> {
                dismissProgress()
                val message: String = data as String? ?: getString(R.string.login_student_fail)
                showDialog(
                    title = getString(R.string.error_title_dialog),
                    message = message,
                    cancelable = false,
                    buttonList = listOf(getString(R.string.retry_button)),
                    listener = object : DialogButtonClickListener {
                        override fun onDialogButtonClick(position: Int) {
                            dismissDialog()
                        }
                    }
                )
            }
            RiderSessionAction.GET_SESSION_IN_PROGRESS_BY_TEACHER_FAIL,
            RiderSessionAction.GET_SESSION_IN_PROGRESS_BY_STUDENT_FAIL -> {
                dismissProgress()
                showDialog(
                    title = getString(R.string.error_title_dialog),
                    message = getString(R.string.login_student_fail),
                    cancelable = false,
                    buttonList = listOf(getString(R.string.retry_button)),
                    listener = object : DialogButtonClickListener {
                        override fun onDialogButtonClick(position: Int) {
                            dismissDialog()
                        }
                    }
                )
            }
            RiderSessionAction.STUDENT_HAS_SESSION_IN_OTHER_DEVICE -> {
                dismissProgress()
                // clear student Info
                studentAuthInfo = null
                riderSessionViewModel.dropStudentOutSession()
                showDialog(
                    title = getString(R.string.error_title_dialog),
                    message = getString(R.string.student_has_session_in_other_device),
                    cancelable = false,
                    buttonList = listOf(getString(R.string.confirm_title)),
                    listener = object : DialogButtonClickListener {
                        override fun onDialogButtonClick(position: Int) {
                            dismissDialog()
                        }
                    }
                )
            }
            RiderSessionAction.TEACHER_HAS_SESSION_IN_OTHER_DEVICE -> {
                dismissProgress()
                // clear student Info
                studentAuthInfo = null
                riderSessionViewModel.dropStudentOutSession()
                showDialog(
                    title = getString(R.string.error_title_dialog),
                    message = getString(R.string.teacher_has_session_in_other_device),
                    cancelable = false,
                    buttonList = listOf(getString(R.string.confirm_title)),
                    listener = object : DialogButtonClickListener {
                        override fun onDialogButtonClick(position: Int) {
                            dismissDialog()
                        }
                    }
                )
            }
            RiderSessionAction.STUDENT_AND_TEACHER_NOT_MATCH -> {
                dismissProgress()
                // clear student Info
                studentAuthInfo = null
                riderSessionViewModel.dropStudentOutSession()
                showDialog(
                    title = getString(R.string.error_title_dialog),
                    message = getString(R.string.student_and_teacher_not_match),
                    cancelable = false,
                    buttonList = listOf(getString(R.string.confirm_title)),
                    listener = object : DialogButtonClickListener {
                        override fun onDialogButtonClick(position: Int) {
                            dismissDialog()
                        }
                    }
                )
            }
            RiderSessionAction.START_RIDER_SESSION_SUCCESS -> {
                val loginTypeSuffix = when (loginRfid) {
                    true -> "_RFID"
                    false -> "_MA"
                    else -> ""
                }
                val loginStatus = "${loginTypeSuffix}_${riderSessionViewModel.getInternetStatus()}"
                riderSessionViewModel.updateLoginStatus(loginStatus = loginStatus)

                // create log file with session id
                LogRecorder.createLogFile(
                    sessionId = riderSessionViewModel.inProgressSession?.id,
                    studentCode = riderSessionViewModel.inProgressSession?.studentCode,
                    versionApp = BuildConfig.VERSION_NAME,
                    startTime = riderSessionViewModel.getSessionStartTime(),
                    loginStatus = loginStatus,
                    timeLogin = riderSessionViewModel.getSessionStartTime()?.let { Utils.convertTimeStampToTime(it) }
                )
                // start session success -> push student info to rider session
                setupRiderSession()
                dismissProgress()
                BaseNotification.showMessage(
                    getString(
                        R.string.start_session_success,
                        riderSessionViewModel.teacherAuthInfo?.fullName,
                        appViewModel.getPlateSlug(),
                        studentAuthInfo?.fullName,
                        studentAuthInfo?.courseLicense,
                        riderSessionViewModel.inProgressSession?.timeIn24H?.let {
                            DateUtil.ConvertHms(
                                it
                            )
                        }
                    ),
                    showToast = false
                )
            }
            RiderSessionAction.GET_LIST_CARS_STUDENT_SUCCESS -> {
                val listCars: List<CarInfo> = data as List<CarInfo>
                if (listCars.isEmpty()) {
                    dismissProgress()
                    // clear student Info
                    studentAuthInfo = null
                    riderSessionViewModel.dropStudentOutSession()
                    showDialog(
                        title = getString(R.string.title_notification),
                        message = getString(R.string.car_not_in_course),
                        buttonList = listOf(getString(R.string.ok)),
                        listener = object : DialogButtonClickListener {
                            override fun onDialogButtonClick(position: Int) {
                                dismissDialog()
                            }
                        }
                    )
                } else {
                    handleGetStudentInProgressSession(studentAuthInfo!!.userCode)
                }
            }
            // [DAT CER]: only use for get DAT certification
            RiderSessionAction.GET_LIST_CARS_STUDENT_FAIL_BY_INTERNET -> {
                dismissProgress()
                showDialog(
                    title = getString(R.string.title_notification),
                    message = getString(R.string.can_not_check_car_in_course_by_internet),
                    buttonList = listOf(getString(R.string.ok)),
                    listener = object : DialogButtonClickListener {
                        override fun onDialogButtonClick(position: Int) {
                            dismissDialog()
                        }
                    }
                )
                handleGetStudentInProgressSession(studentAuthInfo!!.userCode)
            }
            // [DAT CER]
            RiderSessionAction.GET_LIST_CARS_STUDENT_FAIL -> {
                dismissProgress()
//                // clear student Info
//                studentAuthInfo = null
//                riderSessionViewModel.dropStudentOutSession()
//                showDialog(
//                    title = getString(R.string.error_title_dialog),
//                    message = data as String,
//                    buttonList = listOf(getString(R.string.ok)),
//                    listener = object : DialogButtonClickListener {
//                        override fun onDialogButtonClick(position: Int) {
//                            dismissDialog()
//                        }
//                    }
//                )
                showDialog(
                    title = getString(R.string.title_notification),
                    message = getString(R.string.can_not_check_car_in_course_by_internet),
                    buttonList = listOf(getString(R.string.ok)),
                    listener = object : DialogButtonClickListener {
                        override fun onDialogButtonClick(position: Int) {
                            dismissDialog()
                        }
                    }
                )
                handleGetStudentInProgressSession(studentAuthInfo!!.userCode)
            }
            RiderSessionAction.CHECK_DEVICE_DATE_TIME_FAIL -> {
                dismissProgress()
                showDialog(
                    title = getString(R.string.error_title_dialog),
                    message = getString(R.string.local_device_date_time_wrong),
                    cancelable = false,
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

    private fun handleCallFinishRiderSession(notSendTC: Boolean) {
        if (!isAdded) return
        showProgressDialog(message = getString(R.string.transmitting_data_to_server))
        BaseNotification.showMessage(message = getString(R.string.transmitting_data_to_server), muteSpeak = true)
//        CoroutineScope(Dispatchers.Default).launch {
//            // Delay 1s for camera restate
//            delay(1000)
//            riderSessionViewModel.finishRiderSession(
//                notSendTC = notSendTC,
//                studentLogoutImage = studentImageLogout!!,
//                callback = riderSessionCallback
//            )
//        }
        // Sử dụng lifecycleScope của Fragment thay vì GlobalScope
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                delay(1000) // Chờ một chút để các hiệu ứng UI trước đó ổn định
                riderSessionViewModel.finishRiderSession(
                    notSendTC = notSendTC,
                    studentLogoutImage = studentImageLogout!!,
                    callback = riderSessionCallback
                )
            } catch (e: Exception) {
                Logger.e("Error during finishRiderSession: ${e.message}")
                withContext(Dispatchers.Main) { dismissProgress() }
            }
        }
    }
    private fun handleLogoutByAdmin(){
        CoroutineScope(Dispatchers.Default).launch {
            val lastAuthenImage = riderSessionViewModel.sessionVerificationInfo.faceImageFile
            lastAuthenImage?.let {
                riderSessionViewModel.adminLogoutHandler(studentLogoutImage = lastAuthenImage)
            }
            withContext(Dispatchers.Main) {
                handleSessionCompletionEvent()
            }
        }
    }
    private fun handleSessionCompletionEvent(){
        // 1. Kiểm tra an toàn: Nếu Fragment đã thoát thì không làm gì cả
        if (!isAdded || activity == null) return
//        dismissProgress()
//        // handle cancel all job running
//        clearSessionHandler()
//        BaseNotification.showMessage(getString(R.string.finish_session_success))
//        requireActivity().onBackPressed()
        // 2. Thực thi trên Main Thread để đảm bảo UI không crash
        activity?.runOnUiThread {
            dismissProgress()
            clearSessionHandler()

            // Hiển thị thông báo an toàn
            context?.let {
                BaseNotification.showMessage(it.getString(R.string.finish_session_success))
            }

            // Thoát màn hình một cách an toàn
            activity?.onBackPressed()
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun handleGetStudentInProgressSession(userCode: String) {
        // check exist session in-progress by student
        Logger.d("handleGetStudentInProgressSession by userCode: $userCode")
        riderSessionViewModel.getInProgressSessionByStudent(userCode, riderSessionCallback)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun handleGetTeacherInProgressSession(userCode: String) {
        // check exist session in-progress by student
        Logger.d("handleGetTeacherInProgressSession by userCode: $userCode")
        riderSessionViewModel.getInProgressSessionByTeacher(userCode, riderSessionCallback)
    }

    private fun handlePrepareStartSession() {
        // Check student have data of image face recognition
        if (studentAuthInfo?.avatarId.isNullOrEmpty()) {
            dismissProgress()
            showDialog(
                title = getString(R.string.title_notification),
                message = getString(R.string.student_face_recognition_null),
                cancelable = false,
                buttonList = listOf(getString(R.string.reject_make_face_recog_bt), getString(R.string.make_student_face_recog_bt)),
                listener = object : DialogButtonClickListener {
                    override fun onDialogButtonClick(position: Int) {
                        dismissDialog()
                        if (position == 1) {
                            // Todo go to create student's face recognition screen
                        }
                    }
                }
            )
        } else {
            // check current GPS signal
            if (!riderSessionViewModel.checkGPSAvailable(requireActivity())) {
                dismissProgress()
                showDialog(
                    title = getString(R.string.title_notification),
                    message = getString(R.string.gps_turn_off_error),
                    cancelable = false,
                    buttonList = listOf(getString(R.string.ok), getString(R.string.go_setting_enable_gps_bt)),
                    listener = object : DialogButtonClickListener {
                        override fun onDialogButtonClick(position: Int) {
                            dismissDialog()
                            if (position == 1) {
                                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                                startActivity(intent)
                            }
                        }
                    }
                )
            } else {
                if (studentImageLogin != null) {
                    riderSessionViewModel.startRiderSession(
                        studentAuthInfo!!.userCode,
                        studentAuthInfo!!.lastLoginType,
                        appViewModel.getPlateSlug()!!,
                        studentImageLogin!!,
                        riderSessionCallback
                    )
                } else {
                    dismissProgress()
                    showDialog(
                        title = getString(R.string.title_notification),
                        message = getString(R.string.student_image_login_not_found),
                        buttonList = listOf(getString(R.string.ok)),
                        listener = object : DialogButtonClickListener {
                            override fun onDialogButtonClick(position: Int) {
                                dismissDialog()
                            }
                        }
                    )
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun handleCheckSessionInProgress(inProgressSession: InProgressSession?) {
        Logger.i("inProgressSession: $inProgressSession")
        if (inProgressSession == null) {
            // in case auto login for continues if don't have in progress session -> request student login again
            Logger.i("handleCheckSessionInProgress isStudentContinueSession: $isStudentContinueSession")
            if (isStudentContinueSession) {
                dismissProgress()
                // reset data for new session
                isStudentContinueSession = false
                studentAuthInfo = null
                riderSessionViewModel.dropStudentOutSession()
            } else {
                Logger.d("Start new rider session")
                if (studentImageLogin != null) {
                    riderSessionViewModel.checkStudentAvailable(
                        studentCode = studentAuthInfo!!.userCode,
                        callback = riderSessionCallback
                    )
                } else {
                    dismissProgress()
                    showDialog(
                        title = getString(R.string.title_notification),
                        message = getString(R.string.student_image_login_not_found),
                        buttonList = listOf(getString(R.string.ok)),
                        listener = object : DialogButtonClickListener {
                            override fun onDialogButtonClick(position: Int) {
                                dismissDialog()
                            }
                        }
                    )
                }
            }
        } else {
            dismissProgress()
            Logger.d("Check session info has same DAT, same teacher? for continues session or reject.")
            if (inProgressSession.seri != riderSessionViewModel.getImeiDevice(requireContext())) {
                LogRecorder.e("Mở phiên học không thành công", getString(R.string.student_busy_in_other_device))
                showDialog(
                    title = getString(R.string.title_notification),
                    message = getString(R.string.student_busy_in_other_device),
                    buttonList = listOf(getString(R.string.ok)),
                    listener = object : DialogButtonClickListener {
                        override fun onDialogButtonClick(position: Int) {
                            dismissDialog()
                        }
                    }
                )
            } else if (inProgressSession.teacherCode != riderSessionViewModel.teacherAuthInfo?.userCode) {
                LogRecorder.e("Mở phiên học không thành công", getString(R.string.student_busy_with_other_teacher))
                showDialog(
                    title = getString(R.string.title_notification),
                    message = getString(R.string.student_busy_with_other_teacher),
                    buttonList = listOf(getString(R.string.ok)),
                    listener = object : DialogButtonClickListener {
                        override fun onDialogButtonClick(position: Int) {
                            dismissDialog()
                        }
                    }
                )
            } else {
                CoroutineScope(Dispatchers.Default).launch(
                ) {
                    riderSessionViewModel.continueInProgressSession(inProgressSession)
                    resetTimeCounter()
                    scheduleNextSendAuthenData(
                        riderSessionViewModel.calculateAuthenticationPeriod(
                            timeFrequencySentData = TIME_FREQUENCY_SENT_DATA,
                        )
                    )
                    // time to calculate data validation time
                    timeSendAuthData = sendAuthenDataDuration
                    timeStartRecognition = Calendar.getInstance().timeInMillis / 1000

                    withContext(Dispatchers.Main){
                        setupRiderSession()
                    }
                }
                LogRecorder.i("Tiếp tục phiên học", inProgressSession.toString())
                if (checkSessionInterrupt()) {
                    BaseNotification.showMessage(
                        getString(
                            R.string.continue_session_success,
                            riderSessionViewModel.teacherAuthInfo?.fullName,
                            appViewModel.getPlateSlug(),
                            studentAuthInfo?.fullName,
                            studentAuthInfo?.courseLicense
                        ),
                        showToast = false
                    )
                }
            }
        }
    }
    private fun resetTimeCounter(){
        // reset flag
        nextSendAuthenDataTime = 0L
        recognizeUserFaceSecondCounter = 0L
        recognizeFaceTimeDuration = 4 // second
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private suspend fun handleSendDataAuthentication() {
        Logger.d("handleSendDataAuthentication")
        // call checking resend data upload failed
        CoroutineScope(Dispatchers.Default).launch{
            riderSessionViewModel.checkAndReUploadData()
        }

        // save image authenticate to local and upload to server
        riderSessionViewModel.sessionVerificationInfo.faceImageFile?.also {
            riderSessionViewModel.inProgressSession?.also { inProgressSession ->
                ++inProgressSession.totalVerifyCounter
                if (riderSessionViewModel.sessionVerificationInfo.verifyResult == VerifyResult.VERIFY_SUCCESS) {
                    ++inProgressSession.successVerifyCounter
                    isSuccessAtLast = true
                } else {
                    isSuccessAtLast = false
                }
                withContext(Dispatchers.Main) {
                    notifyAuthenticationFail()
                }
                handlePushAuthData(it)
            }
        }
    }

    private fun setVerifyResult(result: VerifyResult) {
        Logger.d("setVerifyResult")
        riderSessionViewModel.sessionVerificationInfo.verifyResult = result
        // reset countTimeCheckLastImageDetected to 0
        countTimeCheckLastImageDetected = 0
    }

    private fun handleRecognizeResult(imageFaceRecognized: FaceImageData?, resultCheck: Boolean) {
        Logger.d("handleRecognizeResult")
        var faceImageData: FaceImageData? = null
        // save image authenticate to local and upload to server
        imageFaceRecognized?.also {
            // update progress status
            setVerifyResult(result = if (resultCheck) VerifyResult.VERIFY_SUCCESS else VerifyResult.VERIFY_FAIL)
            riderSessionViewModel.sessionVerificationInfo.searchScore = successScore
            LogRecorder.i("Thông báo: ","Giá trị ảnh nhận dạng: $successScore")
            faceImageData = imageFaceRecognized
            // clear queue
            cameraPreviewDataQueue.clear()
            faceDetectedMessageQueue.clear()
        } ?: also {
            // get latest data image from cameraPreviewDataQueue
            val nv21ImageData = cameraPreviewDataQueue.take()
            riderSessionViewModel.sessionVerificationInfo.searchScore = failScore
            LogRecorder.i("Thông báo: ","Giá trị ảnh nhận dạng: $failScore")
            setVerifyResult(result = VerifyResult.VERIFY_FAIL)
            nv21ImageData?.also {
                faceImageData = FaceImageData(it, it.nv21Data)
            }
        }
        faceImageData?.also {
            val fileImageAuth = File(studentSessionFolder, "student_auth_${Utils.getRealTimeStamp()}.png")
            LogRecorder.i("","Chụp ảnh thành công: ${fileImageAuth.name}")
            fileImageAuth.apply {
                if (exists()) delete()
                createNewFile()
                val fos = FileOutputStream(this)
                val yuvImage = YuvImage(
                    it.nv21ImageData.nv21Data,
                    ImageFormat.NV21,
                    it.nv21ImageData.width,
                    it.nv21ImageData.height,
                    null
                )
                val imageRatio: Float = 800.0F / it.nv21ImageData.width.toFloat()
                val quality = (100F * imageRatio).toInt().coerceIn(0, 100)
                yuvImage.compressToJpeg(
                    Rect(0, 0, it.nv21ImageData.width, it.nv21ImageData.height),
                    quality,
                    fos
                )
                fos.flush()
                fos.close()
                Logger.d("Save image temporary success")
                riderSessionViewModel.sessionVerificationInfo.faceImageFile = fileImageAuth
                CoroutineScope(Dispatchers.Main).launch {
                    viewBinding.vLastImageAuthen.visibility = View.VISIBLE
                    val request = ImageRequest.Builder(requireContext())
                        .data(fileImageAuth.path)
                        .crossfade(true)
                        .allowHardware(false)
                        .target(viewBinding.ivLastImageAuthen)
                        .build()
                    ImageLoader.imageLoader?.enqueue(request)
                }
            }
            handleTimeSendAuthData()
        } ?: also {
            LogRecorder.i("","Chụp ảnh thất bại")
        }
    }
    private fun handleTimeSendAuthData() {
        Logger.d("handleTimeSendAuthData")
        if (timeStartRecognition != 0L && timeSendAuthData != 0L) {
            val currentTime = Calendar.getInstance().timeInMillis / 1000
            if (currentTime - timeStartRecognition >= timeSendAuthData) {
                scheduleNextSendAuthenData(0)
            }
            // reset flag
            timeStartRecognition = 0
            timeSendAuthData = 0
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun handlePushAuthData(fileImageAuth: File) {
        riderSessionViewModel.uploadImageInSession(imageFile = fileImageAuth, userCode = studentAuthInfo!!.userCode) { action: RiderSessionAction, data: Any? ->
            Logger.d("handlePushAuthData uploadImageStartSession action: $action | data: $data")
            when (action) {
                RiderSessionAction.UPLOAD_IMAGE_CAPTURE_SUCCESS -> {
                    // keep student authenticate image to session verify
                    riderSessionViewModel.sessionVerificationInfo.studentImageAuthUrl = data as String
                    riderSessionViewModel.sessionVerificationInfo.studentImageAuthPath = fileImageAuth.path
                    riderSessionViewModel.pushAuthenticateData(){
                        updateVerifyResultCounter()
                    }
                }
                RiderSessionAction.UPLOAD_IMAGE_CAPTURE_FAIL -> {
                    // save authentication to local db in case upload image fail
                    riderSessionViewModel.sessionVerificationInfo.studentImageAuthPath =
                        fileImageAuth.path
                    // fail to upload image to server -> save data to local then try again late
                    riderSessionViewModel.handleSaveLocalAuthentication(isSaveLocal = true){
                        updateVerifyResultCounter()
                    }
                    BaseNotification.showWarning(getString(R.string.upload_authen_image_fail_2))
                    LogRecorder.e("Thông báo: ", getString(R.string.upload_authen_image_fail_2))
                }
                else -> {
                    Logger.w("Action $action not handle!")
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    @SuppressLint("SetTextI18n")
    private fun updateVerifyResultCounter() {
        riderSessionViewModel.inProgressSession?.also { inProgressSession ->
            val successCounter = inProgressSession.successVerifyCounter.toString()
            val failCounter = (inProgressSession.totalVerifyCounter - inProgressSession.successVerifyCounter).toString()
            viewBinding.tvSuccessCounter.text = successCounter
            viewBinding.tvFailCounter.text = failCounter

            val authCountByTime: Int = (1 + floor(inProgressSession.totalTime).toInt() / 300)

            // Thêm .coerceAtMost(100) vào cuối kết quả tính toán
            val successPercentage: Int =
                ((inProgressSession.successVerifyCounter.toDouble() / authCountByTime) * 100).roundToInt().coerceAtMost(100)
            Logger.i("| Tỷ lệ thực tế: $successPercentage")

            Logger.i("|${appViewModel.getSearchThreshold()} |${(inProgressSession.successVerifyCounter / authCountByTime).toDouble() * 100} | totalVerifyCounter: ${inProgressSession.totalVerifyCounter} | successVerifyCounter: ${inProgressSession.successVerifyCounter} | successPercentage: $successPercentage")
            if (inProgressSession.totalVerifyCounter == 0) {
                viewBinding.tvVerifySuccessPercentage.text = "-/-%"
                viewBinding.tvLastCheck.text = "-/-"
            } else {
                viewBinding.tvVerifySuccessPercentage.text = "$successPercentage%"
                val color = if (successPercentage >= GOOD_SUCCESS_PERCENTAGE) Color.GREEN
                else if (successPercentage in NORMAL_SUCCESS_PERCENTAGE until GOOD_SUCCESS_PERCENTAGE) Color.YELLOW
                else if (successPercentage in LOW_SUCCESS_PERCENTAGE until NORMAL_SUCCESS_PERCENTAGE) requireContext().getColor(R.color.orange)
                else Color.RED
                val lastCheck = if (isSuccessAtLast) getString(R.string.verify_success_result) else getString(R.string.verify_fail_result)
                viewBinding.tvVerifySuccessPercentage.setTextColor(color)
                viewBinding.tvLastCheck.text = lastCheck
                viewBinding.tvLastCheck.setTextColor(if (isSuccessAtLast) Color.GREEN else Color.RED)
                viewBinding.tvUploadResultImage.text = "${inProgressSession.totalAuthDataUploadSuccess}/${inProgressSession.totalAuthDataUpload}(${1 + Math.floor(inProgressSession.totalTime).toInt()/300})"
                viewBinding.tvUploadResultImage.setTextColor(if (inProgressSession.totalAuthDataUploadSuccess == inProgressSession.totalAuthDataUpload) Color.GREEN else Color.RED)
                viewBinding.tvUploadResultGPS.text = "${inProgressSession.totalGPSUploadSuccess}/${inProgressSession.totalGPSUpload}(${1 + Math.floor(inProgressSession.totalTime).toInt()/10})"
                viewBinding.tvUploadResultGPS.setTextColor(if (inProgressSession.totalGPSUploadSuccess == inProgressSession.totalGPSUpload) Color.GREEN else Color.RED)
                LogRecorder.i("","Kết quả xác thực phiên: Thành công: $successCounter lần, Thất bại: $failCounter lần, Lần cuối: $lastCheck, Tỷ lệ xác thực: ${successPercentage}%, giá trị nhận dạng: ${appViewModel.getSearchThreshold() ?: searchThreshold}")
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
            faceRecognitionRate.append(if (faceMatching) successScore.toInt() else failScore.toInt()).append("/").append(searchThreshold.toInt())
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
                    mat.postScale(w.toFloat() / cameraWidth.toFloat(),h.toFloat() / cameraHeight.toFloat())
                }
                90 -> {
                    mat.setScale((if (mirror) -1.0f else 1.0f), 1f)
                    mat.postTranslate(if (mirror) cameraHeight.toFloat() else 0f, 0f)
                    mat.postScale(w.toFloat() / cameraHeight.toFloat(),h.toFloat() / cameraWidth.toFloat())
                    left = rect.top.toFloat()
                    top = (cameraWidth - rect.right).toFloat()
                    right = rect.bottom.toFloat()
                    bottom = (cameraWidth - rect.left).toFloat()
                }
                180 -> {
                    mat.setScale(1f, 1f)
                    mat.postTranslate(0f, 0f)
                    mat.postScale(w.toFloat() / cameraWidth.toFloat(),h.toFloat() / cameraHeight.toFloat())
                    left = rect.right.toFloat()
                    top = rect.bottom.toFloat()
                    right = rect.left.toFloat()
                    bottom = rect.top.toFloat()
                }
                270 -> {
                    mat.setScale((if (mirror) -1.0f else 1.0f), 1f)
                    mat.postTranslate(if (mirror) cameraHeight.toFloat() else 0f, 0f)
                    mat.postScale(w.toFloat() / cameraHeight.toFloat(),h.toFloat() / cameraWidth.toFloat())
                    left = (cameraHeight - rect.bottom).toFloat()
                    top = rect.left.toFloat()
                    right = (cameraHeight - rect.top).toFloat()
                    bottom = rect.right.toFloat()
                }
            }
            val drect = RectF()
            val srect = RectF(left, top, right, bottom)
            mat.mapRect(drect, srect)
            val insideGuide = viewBinding.faceView.isInsideGuide(drect)
            viewBinding.faceView.addRect(drect)
            viewBinding.faceView.addId(faceIdString.toString())
            viewBinding.faceView.addRoll(faceRollString.toString())
            viewBinding.faceView.addPitch(facePitchString.toString())
            viewBinding.faceView.addYaw(faceYawString.toString())
            viewBinding.faceView.addBlur(faceBlurString.toString())
            viewBinding.faceView.addSmile(smileString.toString())
            viewBinding.faceView.addRate(faceRecognitionRate.toString())
            if (insideGuide) {
                faceInGuideSeenThisCycle = true
                viewBinding.faceView.setShowGuide(false)
            } else {
                lastFaceOutsideGuideTime = Utils.getRealTimeStamp()
                viewBinding.faceView.setShowGuide(true)
            }
            withContext(Dispatchers.Main){
                viewBinding.faceView.invalidate()
            }
            return insideGuide
        }
        return false
    }

    private fun notifyAuthenticationFail() {
        if (!isSuccessAtLast) {
            BaseNotification.showWarning(
                getString(
                    R.string.verify_fail_message
                ), muteSpeak = true
            )
            BaseNotification.showWarning(
                getString(
                    R.string.verify_fail_voice
                ), priority = Priority.HIGH,
                showToast = false
            )
            LogRecorder.w("Thông báo: ", getString(R.string.verify_fail_message))
        }
    }

    override fun onResume() {
        Logger.d("onResume")
        super.onResume()

        // --- KHỐI KIỂM TRA BẢO MẬT ---
        val mockAppPackage = SecurityUtils.getActiveMockApp(requireContext())
        val unauthorizedApps = SecurityUtils.checkNonWhitelistApps(requireContext())

        // Nếu ĐÃ SẠCH (Không có app mock và không có app lạ)
        if (mockAppPackage.isNullOrEmpty() && unauthorizedApps.isEmpty()) {
            // Đóng dialog cảnh báo nếu nó đang hiển thị
            dismissDialog()

            // TIẾP TỤC CHẠY LOGIC BÌNH THƯỜNG CỦA APP
            if(timeCounterThread?.isAlive == false || timeCounterThread == null || timeCounterThread?.isInterrupted == true){
                startTimeCounter()
            }
            if(isThreadRunningJob?.isActive == false || isThreadRunningJob == null){
                checkTimeCounterThread()
            }
            riderSessionViewModel.startGPSEventListener(gpsEventListener)

            if (riderSessionViewModel.teacherAuthInfo != null &&
                studentAuthInfo != null &&
                riderSessionViewModel.getSessionInProgress() != null
            ) {
                openCamera()
                CoroutineScope(Dispatchers.Default).launch{
                    resetTimeCounter()
                    scheduleNextSendAuthenData(
                        riderSessionViewModel.calculateAuthenticationPeriod(
                            timeFrequencySentData = TIME_FREQUENCY_SENT_DATA,
                        )
                    )
                }
            }
            checkSessionInterrupt()

            if (checkSessionInterrupt()) {
                BaseNotification.showMessage(
                    getString(
                        R.string.continue_session_success,
                        riderSessionViewModel.teacherAuthInfo?.fullName,
                        appViewModel.getPlateSlug(),
                        studentAuthInfo?.fullName,
                        studentAuthInfo?.courseLicense
                    ),
                    showToast = false
                )
            }
        } else {
            // NẾU VẪN CÒN VI PHẠM
            if (!mockAppPackage.isNullOrEmpty()) {
                val appLabel = getAppLabel(mockAppPackage)
                showFakeGpsBlockDialog(appLabel, mockAppPackage)
            } else {
                val firstApp = unauthorizedApps[0]
                val appLabel = firstApp.loadLabel(requireContext().packageManager).toString()
                showFakeGpsBlockDialog(appLabel, firstApp.packageName)
            }
            return // THOÁT KHÔNG CHO CHẠY TIẾP CÁC LOGIC DƯỚI
        }
    }

    private fun checkSessionInterrupt(): Boolean {
        val interruptedTime = riderSessionViewModel.getInProgressSessionInterruptInMinutes()
        Logger.i("checkSessionInterrupt interruptedTime: $interruptedTime")
        LogRecorder.i("", "checkSessionInterrupt interruptedTime:  $interruptedTime")
        return if (interruptedTime >= TIME_CHECKING_SESSION_INTERRUPT) {
            CoroutineScope(Dispatchers.IO + Job()).launch(
                CoroutineExceptionHandler { _, _ ->
                    Logger.w("Error in SentData!")
                }
            ) {
                val sessionContinues = riderSessionViewModel.canContinueSessionAfterDisruption(interruptedTime = interruptedTime)
                val message = getString(R.string.session_interrupted_long_time_message, interruptedTime)
                withContext(Dispatchers.Main) {
                    showDialog(
                        title = getString(R.string.title_notification),
                        message = message,
                        cancelable = false,
                        buttonList = if (sessionContinues) {
                            listOf(getString(R.string.quit_session), getString(R.string.deny_bt))
                        } else {
                            listOf(getString(R.string.quit_session))
                        },
                        listener = object : DialogButtonClickListener {
                            override fun onDialogButtonClick(position: Int) {
                                dismissDialog()
                                if (position == 0) {
                                    handleFinishRiderSession(sessionContinues = sessionContinues)
                                }
                            }
                        }
                    )
                }
                delay(1000)
                withContext(Dispatchers.Main) {
                    BaseNotification.showWarning(message, showToast = false)
                }
            }
            false
        } else true
    }

    override fun onPause() {
        Logger.d("onPause")
        LogRecorder.i("Trạng thái","onPause" )
        super.onPause()
        riderSessionViewModel.stopGPSEventListener(gpsEventListener)
        pauseHandleProcess()
        riderSessionViewModel.saveInProgressSessionInterruptTime()
    }

    override fun onDestroy() {
        Logger.d("onDestroy")
        super.onDestroy()
        clearSessionHandler()
        // 3. Hủy đăng ký trong onDestroy
        connectivityManager?.unregisterNetworkCallback(networkCallback)
    }

    override fun onBackPressed(): Boolean {
        requireActivity().finish()
        return true
    }

    private fun clearSessionHandler() {
        studentImageLogin = null
        studentImageLogout = null
        // cancel job checking time over learning
        appViewModel.stopNFCCard()
        pauseHandleProcess()
        riderSessionViewModel.stopGPSEventListener(gpsEventListener)
        faceRecognitionViewModel.stopRecognition()
        // fix issue show error dialog after session finish success
        dismissDialog()
        // Thêm dòng này
        powerSaveReceiver?.let { requireContext().unregisterReceiver(it) }
        powerSaveReceiver = null
    }

    // Hàm hỗ trợ lấy tên App từ Package Name
    private fun getAppLabel(packageName: String): String {
        return try {
            val pm = requireContext().packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
        } catch (e: Exception) {
            packageName
        }
    }

    private fun showFakeGpsBlockDialog(appLabel: String, packageName: String) {
        CoroutineScope(Dispatchers.Main).launch {
            pauseHandleProcess()

            showDialog(
                title = "PHÁT HIỆN VI PHẠM",
                message = "Ứng dụng không hợp lệ: $appLabel\n\nBạn phải gỡ bỏ ứng dụng này để tiếp tục phiên học DAT theo quy định.",
                cancelable = false,
                buttonList = listOf("Gỡ cài đặt", "Thoát ứng dụng"),
                listener = object : DialogButtonClickListener {
                    override fun onDialogButtonClick(position: Int) {
                        if (position == 0) {
                            // MỞ THẲNG TRANG QUẢN LÝ (APP INFO) ĐỂ GỠ CÀI ĐẶT
                            try {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                val uri = android.net.Uri.fromParts("package", packageName, null)
                                intent.data = uri
                                startActivity(intent)
                            } catch (e: Exception) {
                                // Nếu lỗi thì mở danh sách app chung
                                startActivity(Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS))
                            }
                        } else {
                            requireActivity().finishAffinity()
                        }
                    }
                }
            )
        }
    }
}

data class FaceImageData(
    val nv21ImageData: Nv21ImageData,
    val imageData: ByteArray
)
