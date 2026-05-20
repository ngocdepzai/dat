package com.hc.dat

import android.Manifest
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.nfc.NfcAdapter
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.provider.Settings
import android.view.MenuItem
import android.view.MotionEvent
import androidx.annotation.RequiresApi
import androidx.core.view.GravityCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.findNavController
import com.hc.dat.di.component.AppComponent
import com.hc.dat.service.ServiceDefinition.GUIDE_URL
import com.hc.dat.utils.Countdown
import com.hc.dat.view.BaseDialog
import com.hc.dat.view.BaseNotification
import com.hc.dat.view.CommonAppUI
import com.hc.dat.view.NavigationEventHandler
import com.hc.dat.view.adapter.DialogButtonClickListener
import com.hc.dat.viewmodel.FaceRecognitionViewModel
import com.lws.device.Device
import com.lws.type.LogRecorder
import com.lws.type.Logger
import hc.manager.datapp.R
import hc.manager.datapp.databinding.DatActivityMainBinding
import kotlinx.android.synthetic.main.dat_activity_main.*
import kotlinx.coroutines.*
import java.io.File
import javax.inject.Inject

class DatMainActivity : BaseActivity(), CommonAppUI {
    private lateinit var app: DatApplication
    private lateinit var viewBinding: DatActivityMainBinding
    private var navigationEventHandler: NavigationEventHandler? = null
    private val navController by lazy { findNavController(R.id.app_frag) }

    @Inject
    lateinit var device: Device

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    lateinit var faceRecognitionViewModel: FaceRecognitionViewModel

    private lateinit var pendingIntent: PendingIntent
    private var nfcAdapter: NfcAdapter? = null

    // Permission old logic
    private val PERMISSIONS_REQUEST: Int = 1
    private val PERMISSION_CAMERA = Manifest.permission.CAMERA
    private val PERMISSION_READ_PHONE_STATE = Manifest.permission.READ_PHONE_STATE
    private val PERMISSION_WRITE_STORAGE = Manifest.permission.WRITE_EXTERNAL_STORAGE
    private val PERMISSION_READ_STORAGE = Manifest.permission.READ_EXTERNAL_STORAGE
    private val PERMISSION_INTERNET = Manifest.permission.INTERNET
    private val PERMISSION_ACCESS_NETWORK_STATE = Manifest.permission.ACCESS_NETWORK_STATE

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST) {
            var granted = true
            for (result in grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) granted = false
            }
            if (!granted) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) if (!shouldShowRequestPermissionRationale(PERMISSION_CAMERA) ||
                    !shouldShowRequestPermissionRationale(PERMISSION_READ_STORAGE) ||
                    !shouldShowRequestPermissionRationale(PERMISSION_WRITE_STORAGE) ||
                    !shouldShowRequestPermissionRationale(PERMISSION_INTERNET) ||
                    !shouldShowRequestPermissionRationale(PERMISSION_ACCESS_NETWORK_STATE)
                ) {
                    if (!hasPermission()) {
                        showDialogCheckPermission()
                        return
                    }
                }
            } else {
                if (checkSelfPermission(PERMISSION_READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(
                        arrayOf(PERMISSION_READ_PHONE_STATE),
                        PERMISSIONS_REQUEST
                    )
                }
            }
        }
    }

    private fun hasPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkSelfPermission(PERMISSION_CAMERA) == PackageManager.PERMISSION_GRANTED
                    && checkSelfPermission(PERMISSION_READ_STORAGE) == PackageManager.PERMISSION_GRANTED
                    && checkSelfPermission(PERMISSION_WRITE_STORAGE) == PackageManager.PERMISSION_GRANTED
                    && checkSelfPermission(PERMISSION_INTERNET) == PackageManager.PERMISSION_GRANTED
                    && checkSelfPermission(PERMISSION_ACCESS_NETWORK_STATE) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun showDialogCheckPermission() {
        AlertDialog.Builder(this@DatMainActivity)
            .setMessage("VUI LÒNG CẤP QUYỂN TRUY CẬP BỘ NHỚ, CAMERA, VỊ TRÍ TRƯỚC KHI SỬ DỤNG!")
            .setPositiveButton("Đi đến cài đặt") { paramDialogInterface, paramInt ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                val uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivity(intent)
            }
            .setNegativeButton("Hủy bỏ", null)
            .show()
    }

    companion object {
        private val NFC_ACTION_FILTER = arrayOf(
            IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED),
            IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED),
            IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewBinding = DatActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        app = application as DatApplication
        KeyboardManager.setConfig(this, viewBinding.root)

        navigationView.setNavigationItemSelectedListener { menuItem ->
            Logger.d("Menu item checked: $menuItem")
            drawerLayout.closeDrawer(GravityCompat.START)
            Handler().postDelayed(
                {
                    handleMenuItemSelection(menuItem)
                },
                500
            )
            true
        }

        navController.setGraph(R.navigation.dat_nav_graph)
        AppComponent.getInstance().inject(this)

        // start device connection
        device.connectDevice(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                val uri = Uri.fromParts("package",packageName, null)
                intent.data = uri
                startActivity(intent)

            }}
        // Add observer activity event for camera and printer handler
        onActivityResultObserver.add(device.getCurrentCamera()!!.activityResultCallback)
        onReqPermissionResultObserver.addAll(
            listOf(device.getCurrentCamera()!!.requestPermissionResultCallback)
        )
//        onActivityResultObserver.add(device.getCurrentGPS()!!.activityResultCallback)
        onReqPermissionResultObserver.addAll(
            listOf(device.getCurrentGPS()!!.requestPermissionResultCallback)
        )
        onReqPermissionResultObserver.addAll(
            listOf(device.getCurrentNetworkConnection()!!.requestPermissionResultCallback)
        )
        onNewIntentObserver.add(device.getCurrentNFC()!!.newIntentCallback)

//        // call getFacePassHandler for createFacePassHandler first time
//        device.getFacePassHandler()
        faceRecognitionViewModel = ViewModelProviders.of(
            this, viewModelFactory
        )[FaceRecognitionViewModel::class.java]

        pendingIntent = PendingIntent.getActivity(
            this,
            Device.NFC_READER_REQUEST_CODE,
            Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        hasPermission()
    }

    private fun handleMenuItemSelection(menuItem: MenuItem) {
        when (menuItem.itemId) {
            R.id.menuCompare -> {
                navController.navigate(R.id.action_loginMenuScreen_to_sessionHistoryScreen)
            }
            R.id.nfcManage -> {
                navController.navigate(R.id.action_loginMenuScreen_to_nfcScreen)
            }
            R.id.clearData -> {
                BaseDialog.showDialog(
                    context = this,
                    title = getString(R.string.confirm_title),
                    message = getString(R.string.confirm_delete_date),
                    buttonList = listOf(getString(R.string.skip_bt), getString(R.string.delete_bt)),
                    cancelable = true,
                    listener = object : DialogButtonClickListener {
                        override fun onDialogButtonClick(position: Int) {
                            BaseDialog.dismiss()
                            if (position == 1) {
                                CoroutineScope(Dispatchers.IO).launch {
                                    handleDeleteImageFolder()
                                    withContext(Dispatchers.Main) {
                                        BaseNotification.showMessage("xóa bộ nhớ thành công", muteSpeak = true)
                                    }
                                }
//                                handleDeleteImageFolder()
//                                BaseNotification.showMessage("xóa bộ nhớ thành công", muteSpeak = true)
                            }
                        }
                    }
                )
//                val intent = Intent(this@DatMainActivity, MenuActivity::class.java)
//                startActivity(intent)
            }
            R.id.clearCacheData -> {
                BaseDialog.showDialog(
                    context = this,
                    title = getString(R.string.confirm_title),
                    message = "Xóa bộ nhớ đệm của DAT",
                    buttonList = listOf("xóa", "không xóa"),
                    cancelable = true,
                    listener = object : DialogButtonClickListener {
                        override fun onDialogButtonClick(position: Int) {
                            BaseDialog.dismiss()
                            if (position == 0) {
                                clearAppCache(context = this@DatMainActivity)
                            }
                        }
                    }
                )
            }
//            R.id.addFaceRecognition -> {
// //                val intent = Intent(this@DatMainActivity, CompareSession::class.java)
// //                startActivity(intent)
//            }
            R.id.registerFaceRecognize -> {
                navController.navigate(R.id.action_goto_registerFaceRecognizeScreen)
            }
            R.id.activeLicense -> {
                faceRecognitionViewModel.showDialogCertificationVerifyTimeOut = true
//                faceRecognitionViewModel.downloadLicense()
            }
            R.id.setupNetwork -> {
                val intent = Intent(Settings.ACTION_WIFI_SETTINGS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }
            R.id.appInfo -> {
                navController.navigate(R.id.action_loginMenuScreen_to_infoDeviceScreen)
                overridePendingTransition(R.anim.in_from_right, R.anim.out_to_left)
            }
            R.id.appGuide -> {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse(GUIDE_URL)
                startActivity(intent)
            }
            R.id.restartApp -> {
                val intent = Intent(this, DatMainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                this.startActivity(intent)
                this.finish()

                Runtime.getRuntime().exit(0)
            }
        }
    }

    private fun handleDeleteImageFolder() {
        val filepath = Environment.getExternalStorageDirectory()
        val dirImage = File(filepath.absolutePath + "/HC_DAT_IMAGE")
        val dirImages = File(filepath.absolutePath + "/HC_DAT_IMAGES")
        val dirBackup = File(filepath.absolutePath + "/HC_DAT_BACKUP")
        val dirReport = File(filepath.absolutePath + "/HC_DAT_REPORT")
        deleteRecursive(dirImage)
        deleteRecursive(dirImages)
        deleteRecursive(dirBackup)
        deleteRecursive(dirReport)
    }
    private fun clearAppCache(context: Context) {
        try {
            val dir = context.cacheDir
            if (dir != null && dir.isDirectory) {
                deleteDir(dir)
                BaseNotification.showMessage("xóa bộ nhớ đệm thành công", muteSpeak = true)
            }
        } catch (e: Exception) {
            Logger.i("clear error cache: $e")
        }
    }

    private fun deleteDir(dir: File): Boolean {
        if (dir.isDirectory) {
            val children = dir.list()
            children?.forEach {
                val success = deleteDir(File(dir, it))
                if (!success) {
                    return false
                }
            }
        }
        return dir.delete()
    }
    private fun deleteRecursive(fileOrDirectory: File) {
        if (fileOrDirectory.isDirectory) {
            for (child in fileOrDirectory.listFiles()) {
                deleteRecursive(child)
            }
        }
        fileOrDirectory.delete()
    }

    override fun onBackPressed() {
        if (!navController.popBackStack()) {
            finish()
        }
    }

    override fun onResume() {
        super.onResume()

        handleStartGPS()
        device.getCurrentGPS()?.startSatelliteCounter(this)
        BaseNotification.init(this)
    }

    private fun handleStartGPS() {
        CoroutineScope(Dispatchers.Main).launch(
            CoroutineExceptionHandler { _, _ ->
                Logger.w("Error in gps service listener --->> recall handleStartGPS")
                handleStartGPS()
            }
        ) {
            // Delay 1000 for activity finish start
            delay(2000)
            device.getCurrentGPS()?.startGPSService(this@DatMainActivity)
            nfcAdapter?.enableForegroundDispatch(
                this@DatMainActivity,
                pendingIntent,
                NFC_ACTION_FILTER,
                null
            ) ?: run { throw RuntimeException("You must call initNFCReadCard first") }
        }
    }

    override fun onDestroy() {
        device.disconnectDevice()
        super.onDestroy()
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
        device.getCurrentGPS()?.stopGPSService()
        LogRecorder.saveLog(false)
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        KeyboardManager.setKeyboardVisible(false)
        Countdown.restartCountdown()
        return super.dispatchTouchEvent(ev)
    }

    override fun setAppHeader(
        btLeft: String?,
        title: String?,
        textRight: String?,
        descriptionRight: String?
    ) {
        Logger.d("setAppHeader btLeft: $btLeft title: " + "$title textRight: $textRight descriptionRight: $descriptionRight")
    }

    override fun setAppBottom(btLeft: String?, btCenter: String?, btRight: String?) {
        Logger.d("setAppBottom btLeft: $btLeft btCenter: $btCenter btRight: $btRight")
    }

    override fun setAppHeaderState(
        isEnableBtLeft: Boolean?,
        isVisibleTitle: Boolean?,
        isVisibleElementRight: Boolean?
    ) {
        Logger.d(
            "setAppHeaderState isEnableBtLeft: $isEnableBtLeft |" +
                "isVisibleTitle: $isVisibleTitle |" +
                "isVisibleElementRight: $isVisibleElementRight"
        )
    }

    override fun setAppBottomState(
        isEnableBtLeft: Boolean?,
        isEnableBtCenter: Boolean?,
        isEnableBtRight: Boolean?
    ) {
        Logger.d(
            "setAppBottomState isEnableBtLeft: $isEnableBtLeft |" +
                "isEnableBtCenter: $isEnableBtCenter |" +
                "isEnableBtRight: $isEnableBtRight"
        )
    }

    override fun setNavigationCallback(callback: NavigationEventHandler) {
        navigationEventHandler = callback
    }
}