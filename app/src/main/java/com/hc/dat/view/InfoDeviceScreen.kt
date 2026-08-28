package com.hc.dat.view

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.SeekBar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModelProviders
import com.hc.dat.utils.Utils.getDeviceInfo
import com.hc.dat.view.dialog.DeviceInfoDialog
import com.hc.dat.viewmodel.*
import com.lws.type.Logger
import hc.manager.datapp.activity.InAppUpdateActivity
import hc.manager.datapp.databinding.ActivityInfoDeviceBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class InfoDeviceScreen : DatBaseScreen() {
    private lateinit var viewBinding: ActivityInfoDeviceBinding
    private lateinit var riderSessionViewModel: RiderSessionViewModel

    private val files = mutableListOf<File>()
    companion object{
        const val REQUEST_CODE_PICK_FOLDER = 2001
        // SeekBar chỉ đi từ 0, nên progress được quy đổi sang phút bằng cách cộng mốc này.
        const val AUTO_LOGOUT_TEACHER_MINUTE_MIN = 45
    }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        riderSessionViewModel =
            ViewModelProviders.of(
                requireActivity(),
                viewModelFactory
            )[RiderSessionViewModel::class.java]
        // Inflate the layout for this fragment
        viewBinding = ActivityInfoDeviceBinding.inflate(inflater, container, false)

        initView()
        return viewBinding.root
    }

    private fun initView() {
        updateView()
        viewBinding.swtchVelocityAlert.isChecked = riderSessionViewModel.notifyErrorVelocity
        viewBinding.swtchUse4GAlert.isChecked = riderSessionViewModel.notifyUse4G
        viewBinding.swtchDistanceNotChange.isChecked = riderSessionViewModel.notifyDistanceNotChange
        CoroutineScope(Dispatchers.Main).launch {
            val autoLogoutTime = riderSessionViewModel.getAutoLogoutTime()
            viewBinding.skAutoLogoutTime.progress = autoLogoutTime
            viewBinding.tvAutoLogoutTime.text = "Thời gian tự động đăng xuất HV quá 10 giờ: 9h${viewBinding.skAutoLogoutTime.progress + 50}p"
            viewBinding.swtchAutoLogoutTeacher.isChecked =
                riderSessionViewModel.getAutoLogoutTeacherOver10HoursEnabled()
            val autoLogoutTeacherMinute = riderSessionViewModel.getAutoLogoutTeacherMinute()
            viewBinding.skAutoLogoutTeacherTime.progress =
                autoLogoutTeacherMinute - AUTO_LOGOUT_TEACHER_MINUTE_MIN
            viewBinding.tvAutoLogoutTeacherTime.text =
                "Thời gian tự động đăng xuất GV quá 10 giờ: 9h${autoLogoutTeacherMinute}p"
        }

        viewBinding.btUpdateApp.setOnClickListener {
            try {
                val intent = Intent(
                    requireContext(),
                    InAppUpdateActivity::class.java
                )
                startActivity(intent)
            } finally {
                requireActivity().finish()
            }
        }
        viewBinding.btEnterInfo.setOnClickListener{
            DeviceInfoDialog.showDialog(requireActivity(),applicationViewModel = appViewModel){
                updateView()
            }
        }
        viewBinding.btUpdateDeviceInfo.setOnClickListener {
            appViewModel.uploadDeviceInfo(appCallback)
        }
        viewBinding.btSelectFile.setOnClickListener {
            files.clear()
            // select file
//            openFilePicker()

            // select folder
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            intent.addCategory(Intent.CATEGORY_DEFAULT)
            startActivityForResult(intent, REQUEST_CODE_PICK_FOLDER)
        }

        viewBinding.swtchVelocityAlert.setOnClickListener {
            riderSessionViewModel.notifyErrorVelocity = viewBinding.swtchVelocityAlert.isChecked
        }
        viewBinding.swtchUse4GAlert.setOnClickListener {
            riderSessionViewModel.notifyUse4G = viewBinding.swtchUse4GAlert.isChecked
        }
        viewBinding.swtchDistanceNotChange.setOnClickListener{
            riderSessionViewModel.notifyDistanceNotChange = viewBinding.swtchDistanceNotChange.isChecked
        }
        viewBinding.swtchAutoLogoutTeacher.setOnClickListener {
            riderSessionViewModel.saveAutoLogoutTeacherOver10HoursEnabled(
                enabled = viewBinding.swtchAutoLogoutTeacher.isChecked
            )
        }
        viewBinding.skAutoLogoutTeacherTime.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                viewBinding.tvAutoLogoutTeacherTime.text =
                    "Thời gian tự động đăng xuất GV quá 10 giờ: 9h${progress + AUTO_LOGOUT_TEACHER_MINUTE_MIN}p"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                riderSessionViewModel.saveAutoLogoutTeacherMinute(
                    minute = seekBar.progress + AUTO_LOGOUT_TEACHER_MINUTE_MIN
                )
            }
        })
        viewBinding.skAutoLogoutTime.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                viewBinding.tvAutoLogoutTime.text =
                    "Thời gian tự động đăng xuất HV quá 10 giờ: 9h${progress + 50}p"

            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                riderSessionViewModel.saveAutoLogoutTime(autoLogoutTime = seekBar.progress)

            }
        })
    }
    private fun updateView(){
        val deviceInfo = getDeviceInfo(requireActivity())
        viewBinding.tvSeri.text = deviceInfo.seri
        viewBinding.tvImei1.text = " :${deviceInfo.imei1}"
        viewBinding.tvSerialSIM.text = " :${deviceInfo.simReal}"
        viewBinding.tvVersionApp.text = " :${deviceInfo.versionAppDat}"
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_PICK_FOLDER && resultCode == Activity.RESULT_OK) {
            val folderUri: Uri? = data?.data
            folderUri?.let {
                uploadFolderToServer(it)
            }
        }
    }
    private fun uploadFolderToServer(folderUri: Uri) {
        val documentFile = DocumentFile.fromTreeUri(requireContext(), folderUri)
        documentFile?.let {
            for (file in it.listFiles()) {
                if (file.isFile) {
                    appViewModel.uriToFile(requireContext(), file.uri)?.let { file ->
                        val fileRenamed = appViewModel.renameFile(file)
                        files.add(fileRenamed)
                    }
                }
            }
            showProgressDialog()
            appViewModel.pushFile(files = files,callback = appCallback)
        }
    }
    private val appCallback: (action: AppAction, data: Any?)
    -> Unit = { action: AppAction, data: Any? ->
        Logger.d("loginCallback action: $action | data: $data")
        dismissProgress()
        when (action) {
            AppAction.UPLOAD_DEVICE_INFO_SUCCESS -> {
                val message: String = data.toString()
                BaseNotification.showMessage(message = message, muteSpeak = true)
            }

            AppAction.UPLOAD_DEVICE_INFO_FAIL -> {
                val message: String = data.toString()
                BaseNotification.showError(message = message, muteSpeak = true)
            }
            AppAction.UPLOAD_FILE_SUCCESS -> {
                val message: String = data.toString()
                BaseNotification.showError(message = message, muteSpeak = true)
            }
            AppAction.UPLOAD_FILE_FAIL -> {
                val message: String = data.toString()
                BaseNotification.showError(message = message, muteSpeak = true)
            }
            else -> {}
        }
    }

    override fun onBackPressed(): Boolean {
        requireActivity().finish()
        return true
    }
    private fun openFilePicker() {
        selectFileLauncher.launch(arrayOf("*/*"))

    }
    private val selectFileLauncher =
        registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris: List<Uri> ->
            if (uris.isEmpty()) dismissProgress()
            uris.let {
                uris.forEach { uri ->
                    context?.let { context ->
                        appViewModel.uriToFile(context, uri)?.let { file ->
                            val fileRenamed = appViewModel.renameFile(file)
                            files.add(fileRenamed)
                        }
                    }
                }
                appViewModel.pushFile(files = files,callback = appCallback)
            }
        }
}
