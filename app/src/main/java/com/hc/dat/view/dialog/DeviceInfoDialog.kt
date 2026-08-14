package com.hc.dat.view.dialog

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.*
import android.os.Build
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.hc.dat.utils.Utils
import com.hc.dat.view.BaseDialog.dismissProgress
import com.hc.dat.view.BaseDialog.showProgressDialog
import com.hc.dat.viewmodel.AppAction
import com.hc.dat.viewmodel.ApplicationViewModel
import com.lws.device.camerapreview.*
import com.lws.type.Logger
import hc.manager.datapp.R
import hc.manager.datapp.databinding.DeviceInfoDialogBinding
import kotlinx.coroutines.*

@SuppressLint("StaticFieldLeak")
internal object DeviceInfoDialog {
    private var dialog: AlertDialog? = null
    private lateinit var viewBinding: DeviceInfoDialogBinding
    private lateinit var applicationViewModel: ApplicationViewModel
    var serialNumber = ""
    var imei1 = ""
    var imei2 = ""
    private lateinit var activity: Activity

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    fun showDialog(
        activity: Activity,
        applicationViewModel: ApplicationViewModel,
        function: () -> Unit,
    ) {
        Logger.d("showDialog")
        // reset old data

        this.activity = activity
        this.applicationViewModel = applicationViewModel
        val view = LayoutInflater.from(activity)
            .inflate(R.layout.device_info_dialog, null, false)
        viewBinding = DeviceInfoDialogBinding.bind(view)

        dialog?.run {
            if (isShowing) dismiss()
        }

        dialog = MaterialAlertDialogBuilder(activity)
            .setCancelable(true)
            .setView(viewBinding.root).create()
        dialog?.setOnShowListener {
            dialog?.window?.setLayout(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        dialog?.show()
        initView(function)
    }


    @RequiresApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    private fun initView(function: () -> Unit) {
        viewBinding.btnConfirm.setOnClickListener {
            serialNumber = viewBinding.edtSerialNumber.editText?.text.toString()
            imei1 = viewBinding.edtImei1.editText?.text.toString()
            imei2 = viewBinding.edtImei2.editText?.text.toString()
            if (serialNumber.isNotEmpty() && imei1.isNotEmpty() && imei2.isNotEmpty()) {
                showProgressDialog(activity)
                Utils.saveDataConfigToExternalStorage(
                    serialNumber = serialNumber,
                    imei1 = imei1,
                    imei2 = imei2
                )
                applicationViewModel.saveDeviceConfigToSharePre(
                    serialNumber = serialNumber,
                    imei1 = imei1,
                    imei2 = imei2
                )
                applicationViewModel.getObjectsLinkedDat(appCallback)
                function()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    val appCallback: (action: AppAction, data: Any?)
    -> Unit = { action: AppAction, data: Any? ->
        Logger.d("loginCallback action: $action | data: $data")
        dismissProgress()
        when (action) {
            AppAction.INIT_CONFIG_DATA_SUCCESS -> {
                applicationViewModel.getDeviceConfig()
                showAlert(
                    title = activity.getString(R.string.title_notification),
                    content = activity.getString(R.string.load_dat_device_config_success)
                )
                dismiss()
            }

            else -> {
                showAlert(
                    title = activity.getString(R.string.error_title_dialog),
                    content = activity.getString(R.string.load_dat_device_config_error)
                )
            }
        }
    }
    private fun showAlert(title: String, content: String) {
        val builder = AlertDialog.Builder(activity)
        builder.setTitle(title)
        builder.setMessage(content)

        builder.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()
        }
        val dialog: AlertDialog = builder.create()
        dialog.show()
    }

    fun dismiss() {
        Logger.d("dismiss")
        dialog?.dismiss()
    }
}

