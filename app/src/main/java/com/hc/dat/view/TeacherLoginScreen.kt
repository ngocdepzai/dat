package com.hc.dat.view

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.*
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.findNavController
import com.hc.dat.model.CarInfo
import com.hc.dat.model.UserInfo
import com.hc.dat.model.database.entity.UserEntity
import com.hc.dat.model.database.entity.convertToModelEntity
import com.hc.dat.service.ServiceDefinition
import com.hc.dat.view.BaseDialog.dismissProgress
import com.hc.dat.view.adapter.DialogButtonClickListener
import com.hc.dat.view.dialog.FaceRecognizeDialog
import com.hc.dat.view.dialog.FaceRecognizeLoginAction
import com.hc.dat.view.dialog.NFCLoginDialog
import com.hc.dat.view.dialog.UserInfoDialog
import com.hc.dat.viewmodel.*
import com.lws.device.gps.GPSAction
import com.lws.device.gps.GPSEvent
import com.lws.device.nfc.NFCAction
import com.lws.device.nfc.NFCEvent
import com.lws.type.LogRecorder
import com.lws.type.Logger
import hc.manager.datapp.R
import hc.manager.datapp.databinding.ScreenTeacherLoginBinding
import hc.manager.datapp.utils.UserTypeContant
import kotlinx.android.synthetic.main.dat_activity_main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

class TeacherLoginScreen : DatBaseScreen() {
    private val REQUEST_CODE_READ_PHONE_STATE = 101

    private lateinit var viewBinding: ScreenTeacherLoginBinding
    private lateinit var riderSessionViewModel: RiderSessionViewModel
    private lateinit var faceRecognitionViewModel: FaceRecognitionViewModel

    private var nfcAvailable: Boolean = true // default true because must login teacher first
    private var byPassCheckSpeed = false

    private var teacherAuthInfo: UserEntity? = null
    private var cameraRotation = 0
    private val cameraFacingFront = true

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        viewBinding = ScreenTeacherLoginBinding.inflate(inflater, container, false)
        riderSessionViewModel =
            ViewModelProviders.of(
                requireActivity(),
                viewModelFactory
            )[RiderSessionViewModel::class.java]
        faceRecognitionViewModel =
            ViewModelProviders.of(
                requireActivity(),
                viewModelFactory
            )[FaceRecognitionViewModel::class.java]

        checkAndRequestPhoneStatePermission()

        nfcAvailable = appViewModel.checkNFCAvailable()

        initView()

        LogRecorder.d("", "Đăng nhập giảng viên")

        return viewBinding.root
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    private fun checkAndRequestPhoneStatePermission() {

        if (ContextCompat.checkSelfPermission(
                requireActivity(),
                Manifest.permission.READ_PHONE_STATE
            )
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.READ_PHONE_STATE),
                REQUEST_CODE_READ_PHONE_STATE
            )
        } else {
            Logger.i("vinhdt:")
            appViewModel.getDeviceConfig()
            appViewModel.getAPIPathUploadImage()
            viewBinding.tvSerialNumber.text =
                getString(
                    R.string.serial_number_info,
                    riderSessionViewModel.getImeiDevice(requireContext())
                )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_READ_PHONE_STATE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                appViewModel.getDeviceConfig()
                appViewModel.getAPIPathUploadImage()
                viewBinding.tvSerialNumber.text =
                    getString(
                        R.string.serial_number_info,
                        riderSessionViewModel.getImeiDevice(requireContext())
                    )
            } else {
                showPermissionDeniedDialog()
            }
        }
    }

    private fun showPermissionDeniedDialog() {
        showDialog(
            title = getString(R.string.title_notification),
            message = getString(R.string.require_permission),
            cancelable = false,
            buttonList = listOf(
                getString(R.string.ok),
                getString(R.string.go_setting)
            ),
            listener = object : DialogButtonClickListener {
                override fun onDialogButtonClick(position: Int) {
                    dismissDialog()
                    if (position == 1) {
                        val intent =
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        startActivity(intent)
                    }
                }
            }
        )
    }
    private fun checkURLUploadImageAvailable(errorMessage: String? = null) {
        if (ServiceDefinition.UPLOAD_IMAGE_AUTHEN_PROGRESS_URL.isEmpty()) {
            CoroutineScope(Dispatchers.Main).launch {
                showDialog(
                    title = getString(R.string.title_notification),
                    cancelable = false,
                    message = errorMessage ?: getString(R.string.get_url_upload_image_error),
                    buttonList = listOf(getString(R.string.retry_button)),
                    listener = object : DialogButtonClickListener {
                        override fun onDialogButtonClick(position: Int) {
                            dismissDialog()
                            showProgressDialog()
                            appViewModel.getAPIPathUploadImage(callback = appCallback)
                        }
                    }
                )
            }
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
//        loadDatDeviceConfig()

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

    }

    private fun loadDatDeviceConfig() {
        // get Dat device config
        showProgressDialog()
        // check issue white screen when remove app then reinstall
        CoroutineScope(Dispatchers.Default).launch {
            delay(1000)
            appViewModel.getObjectsLinkedDat(appCallback)
        }
    }

    private fun initView() {

        viewBinding.tvVehiclePlate.text =
            getString(R.string.vehicle_plate_info, appViewModel.getPlateSlug() ?: "-/-")
        viewBinding.navigationBt.setOnClickListener {
            requireActivity().drawerLayout?.openDrawer(Gravity.LEFT)
        }

        viewBinding.btLoginRfid.setOnClickListener {
            LogRecorder.d("", "Đăng nhập giảng viên bằng thẻ")
                if (byPassCheckSpeed || riderSessionViewModel.checkCarPause()) {
                    // Goto login by rfid
                    startNFCReading()
                    NFCLoginDialog.showDialog(
                        requireActivity(),
                        true,
                        cameraRotation = cameraRotation,
                        faceRecognitionViewModel = faceRecognitionViewModel
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
        }

        viewBinding.btLoginFace.setOnClickListener {
            LogRecorder.d("", "Đăng nhập giảng viên bằng khuôn mặt")
            if (byPassCheckSpeed || riderSessionViewModel.checkCarPause()) {
                if (faceRecognitionViewModel.getCameraPreviewDevice() != null) {
                    // Goto login by face recognize
                    FaceRecognizeDialog.showDialog(
                        requireActivity(),
                        isTeacherLogin = true,
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
                Logger.w("Warning: Login progress is not ready!")
                LogRecorder.e("", getString(R.string.login_method_not_ready))
                showDialog(
                    title = getString(R.string.title_notification),
                    message = getString(R.string.login_method_not_ready),
                    buttonList = listOf(getString(R.string.ok)),
                    listener = object : DialogButtonClickListener {
                        override fun onDialogButtonClick(position: Int) {
                            dismissDialog()
                        }
                    })
            }
        }
        viewBinding.tvSerialNumber.setOnClickListener{
            byPassCheckSpeed = true
        }
        viewBinding.btGuide.setOnClickListener{
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(ServiceDefinition.GUIDE_URL)
            startActivity(intent)
        }
    }

    private fun showAppVersionLockedMessage(){
            showDialog(title = getString(R.string.title_notification),
                message = getString(R.string.version_locked_message),
                buttonList = listOf(getString(R.string.ok)),
                listener = object : DialogButtonClickListener {
                    override fun onDialogButtonClick(position: Int) {
                        dismissDialog()
                    }
                })
    }
    private fun startNFCReading() {
        Logger.d("startNFCReading")
        appViewModel.startNFCCard(
            nfcEvent = object : NFCEvent {
                override fun onNFCDataDetected(nfcAction: NFCAction, data: String?) {
                    when (nfcAction) {
                        NFCAction.NFC_DISABLE -> {
                            LogRecorder.e("Quẹt thẻ RFID", getString(R.string.login_by_nfc_method_note_ready))
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
                            LogRecorder.e("Quẹt thẻ RFID", "NFC_DATA_INCORRECT")
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
                            LogRecorder.i("Quẹt thẻ RFID", result)
                            Logger.i("DETECTED_TAG_DATA result: $result")
                            NFCLoginDialog.dismiss()
                            showProgressDialog()
                            appViewModel.getUserInfoByRfidCode(
                                cardData = result,
                                callback = appCallback
                            )
                        }
                        else -> {}
                    }
                }
            }
        )
    }

    private val faceRecognizeLoginCallback: (action: FaceRecognizeLoginAction, data: Any?, imageFile: File?)
    -> Any? = { action: FaceRecognizeLoginAction, data: Any?, _: File? ->
        Logger.d("faceRecognizeLoginCallback action: $action | data: $data")
        CoroutineScope(Dispatchers.Main).launch {
            FaceRecognizeDialog.dismiss()
            when (action) {
                FaceRecognizeLoginAction.FACE_LOGIN_SUCCESS -> {
                    val userEntity = data as UserEntity
                    UserInfoDialog.showDialog(
                        requireContext(),
                        userEntity
                    ) { confirm, data ->
//                        UserInfoDialog.dismiss()
                        if (confirm) {
                            if (ServiceDefinition.UPLOAD_IMAGE_AUTHEN_PROGRESS_URL.isEmpty()) {
                                checkURLUploadImageAvailable()
                            } else {
                                if (appViewModel.isAppVersionActive) {
                                    // Set teacher info and next to Rider Session screen
                                    teacherAuthInfo = data
                                    // check has car linked device existed in Training Center or not
                                    teacherAuthInfo?.trainingCenterId?.also {
                                        showProgressDialog()
                                        riderSessionViewModel.getCarsByImeiAndTrainingCenter(
                                            idTrainingCenter = it,
                                            callback = riderSessionCallback
                                        )
                                    } ?: run {
                                        // clear teeacher Info
                                        teacherAuthInfo = null
                                        riderSessionViewModel.dropTeachOutWorking()
                                        LogRecorder.e(
                                            "Đăng nhập giảng viên thất bại",
                                            getString(R.string.teacher_not_in_any_training_center)
                                        )
                                        showDialog(
                                            title = getString(R.string.can_not_continues),
                                            message = getString(R.string.teacher_not_in_any_training_center),
                                            buttonList = listOf(getString(R.string.ok)),
                                            listener = object : DialogButtonClickListener {
                                                override fun onDialogButtonClick(position: Int) {
                                                    dismissDialog()
                                                }
                                            }
                                        )
                                    }
                                } else {
                                    showAppVersionLockedMessage()
                                }
                            }

                        } else {
                            LogRecorder.e("Đăng nhập giảng viên thất bại", "Người dùng hủy tiếp tục phiên đăng nhập hiện có của giáo viên")
                            riderSessionViewModel.dropTeachOutWorking()
                        }
                    }
                }
                FaceRecognizeLoginAction.FACE_IMAGE_SAMPLE_NOT_QUALITY,
                FaceRecognizeLoginAction.FACE_IMAGE_SAMPLE_NOT_EXIST -> {
                    LogRecorder.e("Đăng nhập giảng viên thất bại", "Giáo viên chưa có ảnh nhận diện mẫu")
                    showDialog(
                        title = getString(R.string.title_notification),
                        message = getString(R.string.teacher_not_have_sample_image),
                        buttonList = listOf(
                            getString(R.string.skip_bt),
                            getString(R.string.setup_face_bt)
                        ),
                        listener = object : DialogButtonClickListener {
                            override fun onDialogButtonClick(position: Int) {
                                dismissDialog()
                                if (position == 1) {
                                    val userEntity = data as UserEntity
                                    val bundle = bundleOf(
                                        "user_info" to userEntity
                                    )
                                    viewBinding.root.findNavController().navigate(
                                        R.id.action_loginMenuScreen_to_registerFaceRecognizeScreen,
                                        bundle
                                    )
                                }
                            }
                        }
                    )
                }
                FaceRecognizeLoginAction.USER_INFO_NOT_EXIST -> {
                    LogRecorder.e("Đăng nhập giảng viên thất bại", getString(R.string.teacher_not_found))
                    showDialog(
                        title = getString(R.string.title_notification),
                        message = getString(R.string.teacher_not_found),
                        buttonList = listOf(getString(R.string.ok)),
                        listener = object : DialogButtonClickListener {
                            override fun onDialogButtonClick(position: Int) {
                                dismissDialog()
                            }
                        }
                    )
                }
                FaceRecognizeLoginAction.REQUIRED_LOGIN_TEACHER -> {
                    LogRecorder.e("Đăng nhập giảng viên thất bại", getString(R.string.login_teacher_type_required))
                    showDialog(
                        title = getString(R.string.login_wrong_type),
                        message = getString(R.string.login_teacher_type_required),
                        buttonList = listOf(getString(R.string.ok)),
                        listener = object : DialogButtonClickListener {
                            override fun onDialogButtonClick(position: Int) {
                                dismissDialog()
                            }
                        }
                    )
                }
//                FaceRecognizeLoginAction.FACE_LOGIN_FAIL -> {
//                    showDialog(
//                        title = getString(R.string.title_notification),
//                        message = getString(R.string.login_face_fail),
//                        buttonList = listOf(getString(R.string.ok),),
//                        listener = object : DialogButtonClickListener {
//                            override fun onDialogButtonClick(position: Int) {
//                                dismissDialog()
//                            }
//                        }
//                    )
//                }
                else -> {
                    Logger.w("Action $action not yet handle!")
                }
            }
        }
    }

    val appCallback: (action: AppAction, data: Any?)
    -> Unit = { action: AppAction, data: Any? ->
        Logger.d("loginCallback action: $action | data: $data")
        dismissProgress()
        when (action) {
            AppAction.INIT_CONFIG_DATA_SUCCESS -> {
                if (appViewModel.datDevice == null) {
                    LogRecorder.e("Đăng nhập giảng viên thất bại", getString(R.string.app_version_deprecate))
                    showDialog(
                        title = getString(R.string.error_title_dialog),
                        message = getString(R.string.app_version_deprecate),
                        cancelable = false,
                        buttonList = listOf(getString(R.string.retry_button)),
                        listener = object : DialogButtonClickListener {
                            override fun onDialogButtonClick(position: Int) {
                                dismissDialog()
                                loadDatDeviceConfig()
                            }
                        }
                    )
                } else if (appViewModel.trainingCenter == null) {
                    LogRecorder.e("Đăng nhập giảng viên thất bại", getString(R.string.dat_not_link_training_center))
                    showDialog(
                        title = getString(R.string.error_title_dialog),
                        message = getString(R.string.dat_not_link_training_center),
                        cancelable = false,
                        buttonList = listOf(getString(R.string.retry_button)),
                        listener = object : DialogButtonClickListener {
                            override fun onDialogButtonClick(position: Int) {
                                dismissDialog()
                                loadDatDeviceConfig()
                            }
                        }
                    )
                } else if (appViewModel.vehicleInfo == null || appViewModel.vehicleInfo?.plateSlug.isNullOrEmpty()) {
                    LogRecorder.e("Đăng nhập giảng viên thất bại", getString(R.string.device_not_link_any_car))
                    showDialog(
                        title = getString(R.string.error_title_dialog),
                        message = getString(R.string.device_not_link_any_car),
                        cancelable = false,
                        buttonList = listOf(getString(R.string.retry_button)),
                        listener = object : DialogButtonClickListener {
                            override fun onDialogButtonClick(position: Int) {
                                dismissDialog()
                                loadDatDeviceConfig()
                            }
                        }
                    )
                } else {
                    viewBinding.tvVehiclePlate.text = getString(R.string.vehicle_plate_info,appViewModel.vehicleInfo?.plateSlug ?: "-/-")
//                    checkTeacherReLogin()
                    showProgressDialog(message = getString(R.string.loading_user_assigned_in_device))
                    faceRecognitionViewModel.getListUserAssignInDevice(
                        riderSessionViewModel.getImeiDevice(requireContext()),
                        faceRecognitionCallback
                    )
                    // Time out 8s dismiss dialog
                    Handler(Looper.getMainLooper()).postDelayed({
                        dismissProgress()
                    }, 8000)
                }
            }
            AppAction.INIT_CONFIG_DATA_FAIL_BY_INTERNET -> {
                checkTeacherReLogin()
            }
            AppAction.INIT_CONFIG_DATA_FAIL -> {
                LogRecorder.e("Config Error", getString(R.string.load_dat_device_config_error))
                showDialog(
                    title = getString(R.string.error_title_dialog),
                    message = getString(R.string.load_dat_device_config_error),
                    buttonList = listOf(getString(R.string.retry_button)),
                    listener = object : DialogButtonClickListener {
                        override fun onDialogButtonClick(position: Int) {
                            dismissDialog()
                            loadDatDeviceConfig()
                        }
                    }
                )
            }
            AppAction.GET_USER_INFO_AUTO_SUCCESS,
            AppAction.GET_USER_INFO_SUCCESS -> {
                val userInfo = data as UserInfo
                if (userInfo.userType != UserTypeContant.TEACHER) {
                    LogRecorder.e("Đăng nhập giảng viên thất bại", getString(R.string.login_teacher_type_required))
                    showDialog(
                        title = getString(R.string.login_wrong_type),
                        message = getString(R.string.login_teacher_type_required),
                        buttonList = listOf(getString(R.string.ok)),
                        listener = object : DialogButtonClickListener {
                            override fun onDialogButtonClick(position: Int) {
                                dismissDialog()
                            }
                        }
                    )
                } else {
                    UserInfoDialog.showDialog(
                        requireContext(),
                        userInfo.convertToModelEntity()
                    ) { confirm, data ->
                        if (confirm) {
                            // check current GPS signal
                            Logger.i(
                                "Check GPS: ${
                                    riderSessionViewModel.checkGPSAvailable(
                                        requireActivity()
                                    )
                                }"
                            )
                            if (ServiceDefinition.UPLOAD_IMAGE_AUTHEN_PROGRESS_URL.isEmpty()) {
                                checkURLUploadImageAvailable()
                            } else {
                                if (appViewModel.isAppVersionActive) {
                                    if (riderSessionViewModel.checkGPSAvailable(requireActivity())) {
                                        UserInfoDialog.dismiss()
                                        // Set teacher info and next to Rider Session screen
                                        teacherAuthInfo = data
                                        // check has car linked device existed in Training Center or not
                                        teacherAuthInfo?.trainingCenterId?.also {
                                            showProgressDialog()
                                            riderSessionViewModel.getCarsByImeiAndTrainingCenter(
                                                idTrainingCenter = it,
                                                callback = riderSessionCallback
                                            )
                                        } ?: run {
                                            LogRecorder.e(
                                                "Đăng nhập giảng viên thất bại",
                                                getString(R.string.teacher_not_in_any_training_center)
                                            )
                                            showDialog(
                                                title = getString(R.string.can_not_continues),
                                                message = getString(R.string.teacher_not_in_any_training_center),
                                                buttonList = listOf(getString(R.string.ok)),
                                                listener = object : DialogButtonClickListener {
                                                    override fun onDialogButtonClick(position: Int) {
                                                        dismissDialog()
                                                    }
                                                }
                                            )
                                        }
                                    } else {
                                        showDialog(
                                            title = getString(R.string.title_notification),
                                            message = getString(R.string.gps_turn_off_error),
                                            cancelable = false,
                                            buttonList = listOf(
                                                getString(R.string.ok),
                                                getString(R.string.go_setting_enable_gps_bt)
                                            ),
                                            listener = object : DialogButtonClickListener {
                                                override fun onDialogButtonClick(position: Int) {
                                                    dismissDialog()
                                                    if (position == 1) {
                                                        val intent =
                                                            Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                                                        startActivity(intent)
                                                    }
                                                }
                                            }
                                        )
                                    }
                                } else {
                                    UserInfoDialog.dismiss()
                                    showAppVersionLockedMessage()
                                }

                            }
                        } else {
                            riderSessionViewModel.dropTeachOutWorking()
                        }
                    }
                }
            }
            AppAction.GET_USER_INFO_FAIL -> {
                LogRecorder.e("Đăng nhập giảng viên thất bại", data as String)
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
            AppAction.CHECK_DEVICE_DATE_TIME_FAIL -> {
                LogRecorder.e("Đăng nhập giảng viên thất bại", getString(R.string.local_device_date_time_wrong))
                showDialog(
                    title = getString(R.string.error_title_dialog),
                    message = getString(R.string.local_device_date_time_wrong),
                    cancelable = false,
                    buttonList = listOf(getString(R.string.retry_button)),
                    listener = object : DialogButtonClickListener {
                        override fun onDialogButtonClick(position: Int) {
                            dismissDialog()
                            loadDatDeviceConfig()
                        }
                    }
                )
            }
            AppAction.GET_PATH_API_IMAGE_UPLOAD_FAIL -> {
                val errorMessage: String = data as String
                checkURLUploadImageAvailable("Lấy đường dẫn lưu ảnh thất bại. \n$errorMessage")
            }
            AppAction.GET_PATH_API_IMAGE_UPLOAD_FAIL_BY_INTERNET -> {
                val errorMessage: String =
                    "Lấy đường dẫn lưu ảnh thất bại do không có kết nối mạng.\nVui lòng kiểm tra lại kết nối!"
                checkURLUploadImageAvailable(errorMessage)

            }
            else -> {}
        }
    }

    private val faceRecognitionCallback: (action: FaceRecognitionAction, data: Any?)
    -> Unit = { action: FaceRecognitionAction, data: Any? ->
        Logger.d("faceRecognitionCallback action: $action | data: $data")
        dismissProgress()
        when (action) {
            FaceRecognitionAction.GET_LIST_USER_ASSIGN_IN_DEVICE_SUCCESS -> {
                checkTeacherReLogin()
            }
            FaceRecognitionAction.GET_LIST_USER_ASSIGN_IN_DEVICE_FAIL -> {
                // Nothing
            }
            else -> {
                Logger.w("Action $action not handle!")
            }
        }
    }

    private fun checkTeacherReLogin() {
        if (appViewModel.teacherAutoLogin(appCallback)) {
            showProgressDialog()
        }
    }

    val riderSessionCallback: (action: RiderSessionAction, data: Any?)
    -> Unit = { action: RiderSessionAction, data: Any? ->
        Logger.d("riderSessionCallback action: $action | data: $data")
        dismissProgress()
        when (action) {
//            RiderSessionAction.GET_LIST_CARS_STUDENT_SUCCESS -> {
//                val listCars: List<CarInfo> = data as List<CarInfo>
//                if (listCars.isEmpty()) {
//                    showDialog(
//                        title = getString(R.string.title_notification),
//                        message = getString(R.string.car_not_in_course),
//                        buttonList = listOf(getString(R.string.ok)),
//                        listener = object : DialogButtonClickListener {
//                            override fun onDialogButtonClick(position: Int) {
//                                dismissDialog()
//                            }
//                        }
//                    )
//                } else {
//                    // Todo
//                }
//            }
//            RiderSessionAction.GET_LIST_CARS_STUDENT_FAIL,
            RiderSessionAction.GET_LIST_CARS_TEACHER_FAIL -> {
                LogRecorder.e("Đăng nhập giảng viên thất bại", data as String)
                // clear teacher info
                teacherAuthInfo = null
                riderSessionViewModel.dropTeachOutWorking()
                showDialog(
                    title = getString(R.string.error_title_dialog),
                    message = data as String,
                    buttonList = listOf(getString(R.string.ok)),
                    listener = object : DialogButtonClickListener {
                        override fun onDialogButtonClick(position: Int) {
                            dismissDialog()
                        }
                    }
                )
            }
            RiderSessionAction.GET_LIST_CARS_TEACHER_SUCCESS -> {
                val listCars: List<CarInfo> = data as List<CarInfo>
                if (listCars.isEmpty()) {
                    LogRecorder.e("Đăng nhập giảng viên thất bại", getString(R.string.dat_not_link_training_center))
                    showDialog(
                        title = getString(R.string.title_notification),
                        message = getString(R.string.dat_not_link_training_center),
                        buttonList = listOf(getString(R.string.ok)),
                        listener = object : DialogButtonClickListener {
                            override fun onDialogButtonClick(position: Int) {
                                dismissDialog()
                            }
                        }
                    )
                } else {
                    LogRecorder.i("Đăng nhập giảng viên thành công", teacherAuthInfo?.fullName)
                    Logger.d("Success login teacher -> goto Training Session Screen")
                    teacherAuthInfo?.also {
                        riderSessionViewModel.pushTeacherInWorking(it)
                    }
                    viewBinding.root.findNavController()
                        .navigate(R.id.action_loginMenuScreen_to_trainingSessionScreen)
                }
            }
            RiderSessionAction.GET_LIST_CARS_TEACHER_FAIL_BY_INTERNET -> {
                LogRecorder.e("Đăng nhập giảng viên thất bại", getString(R.string.can_not_check_dat_in_center_by_internet))
                showDialog(
                    title = getString(R.string.title_notification),
                    message = getString(R.string.can_not_check_dat_in_center_by_internet),
                    buttonList = listOf(getString(R.string.ok)),
                    listener = object : DialogButtonClickListener {
                        override fun onDialogButtonClick(position: Int) {
                            dismissDialog()
                        }
                    }
                )
                teacherAuthInfo?.also {
                    riderSessionViewModel.pushTeacherInWorking(it)
                }
                viewBinding.root.findNavController()
                    .navigate(R.id.action_loginMenuScreen_to_trainingSessionScreen)
            }
            else -> {
                Logger.w("Action not handle!")
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // load config when crete screen or app comeback to front ground
        loadDatDeviceConfig()
//        startNFCReading()
        appViewModel.stopNFCCard()
        riderSessionViewModel.startGPSEventListener(gpsEventListener)
//        activity?.drawerLayout?.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
    }

    override fun onPause() {
        super.onPause()
        byPassCheckSpeed = false
        riderSessionViewModel.stopGPSEventListener(gpsEventListener)
    }
    override fun onBackPressed(): Boolean {
        requireActivity().finish()
        return true
    }
    private val gpsEventListener = object : GPSEvent {
        @SuppressLint("SetTextI18n")
        override fun onGPSUpdate(action: GPSAction, data: Any?) {
            Logger.i("onGPSUpdate: action: $action")
            when (action) {
                GPSAction.LOCATION_UPDATED -> {
                    val location = data as Location
                    Logger.i("location speed: ${location.speed} | latitude: ${location.latitude} | longitude: ${location.longitude}  | time: ${location.time}")
                    Logger.i("GPSAction [$byPassCheckSpeed] ")
                }
                else -> {
                    Logger.w("GPSAction [$action] not handle")
                }
            }
        }
    }

}
