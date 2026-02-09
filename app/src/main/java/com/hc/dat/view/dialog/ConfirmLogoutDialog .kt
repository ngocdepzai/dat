package com.hc.dat.view.dialog

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.*
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.hc.dat.model.InProgressSession
import com.hc.dat.view.TrainingSessionScreen
import com.lws.device.camerapreview.*
import com.lws.type.Logger
import hc.manager.datapp.R
import hc.manager.datapp.databinding.ConfirmLogoutDialogBinding
import hc.manager.datapp.utils.DateUtil
import kotlinx.coroutines.*
import java.io.File
import kotlin.math.floor
import kotlin.math.roundToInt

@SuppressLint("StaticFieldLeak")
internal object ConFirmLogoutDialog {
    private var dialog: AlertDialog? = null
    private lateinit var viewBinding: ConfirmLogoutDialogBinding


    private lateinit var activity: Activity

    fun showDialog(
        activity: Activity,
        imageLogin: File?,
        imageLogout: File?,
        inProgressSession: InProgressSession,
        callback: ((confirmLogout: Boolean, notSendTC: Boolean) -> Any?)
    ) {
        Logger.d("showDialog")
        this.activity = activity

        val view = LayoutInflater.from(activity)
            .inflate(R.layout.confirm_logout_dialog, null, false)
        viewBinding = ConfirmLogoutDialogBinding.bind(view)

        val totalDistance: Float = inProgressSession.totalDis / 1000f
        viewBinding.tvTotalDistance.text =
            activity.getString(R.string.total_distance_value, totalDistance)
        viewBinding.tvTotalTime.text = DateUtil.ConvertHms(inProgressSession.totalTime)
        val totalImageUploadCountsByTime: Int =
            1 + Math.floor(inProgressSession.totalTime).toInt() / 300
        viewBinding.tvUploadImageResult.text =
            "${inProgressSession.totalAuthDataUploadSuccess}/${inProgressSession.totalAuthDataUpload}($totalImageUploadCountsByTime)"
        val totalGPSUploadCountsByTime: Int =
            1 + Math.floor(inProgressSession.totalTime).toInt() / 10
        viewBinding.tvUploadGPSResult.text =
            "${inProgressSession.totalGPSUploadSuccess}/${inProgressSession.totalGPSUpload}($totalGPSUploadCountsByTime)"

        val authCountByTime: Int = (1 + floor(inProgressSession.totalTime).toInt() / 300)
        val successPercentage: Int =
            ((inProgressSession.successVerifyCounter.toDouble() / authCountByTime) * 100).roundToInt()
        viewBinding.tvPercentagePass.text = "$successPercentage%"
        val color =
            if (successPercentage >= TrainingSessionScreen.GOOD_SUCCESS_PERCENTAGE) Color.GREEN
            else if (successPercentage in TrainingSessionScreen.NORMAL_SUCCESS_PERCENTAGE until TrainingSessionScreen.GOOD_SUCCESS_PERCENTAGE) Color.YELLOW
            else if (successPercentage in TrainingSessionScreen.LOW_SUCCESS_PERCENTAGE until TrainingSessionScreen.NORMAL_SUCCESS_PERCENTAGE) activity.getColor(
                R.color.orange
            )
            else Color.RED
        viewBinding.tvPercentagePass.setTextColor(color)


        dialog?.run {
            if (isShowing) dismiss()
        }

        dialog = MaterialAlertDialogBuilder(activity)
            .setCancelable(false)
            .setView(viewBinding.root).create()
        dialog?.setOnShowListener {
            dialog?.window?.setLayout(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        dialog?.show()
        initView(imageLogin = imageLogin, imageLogout = imageLogout, callback = callback)
    }


    private fun initView(
        imageLogin: File?,
        imageLogout: File?,
        callback: ((confirmLogout: Boolean, notSendTC: Boolean) -> Any?)
    ) {
        viewBinding.ivImageLogin.setImageBitmap(BitmapFactory.decodeFile(imageLogin?.absolutePath))
        viewBinding.ivImageLogout.setImageBitmap(BitmapFactory.decodeFile(imageLogout?.absolutePath))

        viewBinding.btConfirm.setOnClickListener {
            dismiss()
            callback(true, viewBinding.cbNotSendTC.isChecked)
        }
        viewBinding.btCancel.setOnClickListener {
            dismiss()
            callback(false, viewBinding.cbNotSendTC.isChecked)
        }
        viewBinding.cbNotSendTC.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                AlertDialog.Builder(activity)
                    .setTitle("Thông báo")
                    .setMessage("Xác nhận không truyền TC")
                    .setPositiveButton("Đăng xuất") { dialog, _ ->
                        dialog.dismiss()
                        dismiss()
                        callback(true, viewBinding.cbNotSendTC.isChecked)
                    }
                    .setNegativeButton("Hủy") { dialog, _ ->
                        viewBinding.cbNotSendTC.isChecked = false
                        dialog.dismiss()
                    }
                    .create()
                    .show()
            }
        }
    }


    fun dismiss() {
        Logger.d("dismiss")
        dialog?.dismiss()
    }
}
