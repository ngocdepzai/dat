package com.hc.dat.view.dialog

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.*
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.hc.dat.model.InProgressSession
import com.hc.dat.view.BaseNotification
import com.hc.dat.view.TrainingSessionScreen
import com.hc.dat.viewmodel.FaceRecognitionViewModel
import com.lws.device.camerapreview.*
import com.lws.type.LogRecorder
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

    @RequiresApi(Build.VERSION_CODES.M)
    fun showDialog(
            activity: Activity,
            imageLogin: File?,
            imageLogout: File?,
            inProgressSession: InProgressSession,
            faceRecognitionViewModel: FaceRecognitionViewModel,
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
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        dialog?.show()
        initView(imageLogin = imageLogin, imageLogout = imageLogout, faceRecognitionViewModel = faceRecognitionViewModel, callback = callback)

        viewBinding.btCancel.visibility = View.INVISIBLE
        viewBinding.btConfirm.visibility = View.INVISIBLE
        BaseNotification.speakWithCallback(activity.getString(R.string.confirm_logout_info_message)) {
            Handler(Looper.getMainLooper()).post {
                viewBinding.btCancel.visibility = View.VISIBLE
                viewBinding.btConfirm.visibility = View.VISIBLE
            }
        }
    }


    private fun initView(
        imageLogin: File?,
        imageLogout: File?,
        faceRecognitionViewModel: FaceRecognitionViewModel,
        callback: ((confirmLogout: Boolean, notSendTC: Boolean) -> Any?)
    ) {
        // --- KIỂM TRA ẢNH ĐĂNG NHẬP ---
        val bmpLogin = processAndLogImage(imageLogin, "ẢNH ĐĂNG NHẬP")
        viewBinding.ivImageLogin.setImageBitmap(BitmapFactory.decodeFile(imageLogin?.absolutePath))
        // --- KIỂM TRA ẢNH ĐĂNG XUẤT ---
        val bmpLogout = processAndLogImage(imageLogout, "ẢNH ĐĂNG XUẤT")
        viewBinding.ivImageLogout.setImageBitmap(BitmapFactory.decodeFile(imageLogout?.absolutePath))

//        viewBinding.btConfirm.setOnClickListener {
//            if (imageLogin == null || imageLogout == null) return@setOnClickListener
//
//            // Hiện loading
//            com.hc.dat.view.BaseDialog.showProgressDialog(activity, "Đang đối soát ảnh đăng nhập/đăng xuất...")
//
//            CoroutineScope(Dispatchers.Main).launch {
//                val bmpLogin = BitmapFactory.decodeFile(imageLogin.absolutePath)
//                val bmpLogout = BitmapFactory.decodeFile(imageLogout.absolutePath)
//
//                if (bmpLogin != null && bmpLogout != null) {
//                    // GỌI HÀM SO SÁNH 2 ẢNH
//                    val score = faceRecognitionViewModel.compareTwoBitmaps(bmpLogin, bmpLogout)
//
//                    com.hc.dat.view.BaseDialog.dismissProgress()
//
//                    if (score >= 40) { // Ngưỡng an toàn là 40 điểm
//                        Logger.i("Đối soát thành công: $score điểm")
//                        dismiss()
//                        callback(true, viewBinding.cbNotSendTC.isChecked)
//                    } else {
//                        Logger.e("Đối soát thất bại: $score điểm")
//                        MaterialAlertDialogBuilder(activity)
//                                .setTitle("Xác thực không khớp")
//                                .setMessage("Ảnh đăng xuất không khớp với ảnh lúc đăng nhập (Độ khớp: $score%). Vui lòng kiểm tra lại người thực hiện đăng xuất!")
//                                .setPositiveButton("Thử lại", null)
//                                .show()
//                    }
//                } else {
//                    com.hc.dat.view.BaseDialog.dismissProgress()
//                }
//            }
//        }
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

    fun isShowing(): Boolean = dialog?.isShowing == true

    /**
     * Hàm hỗ trợ Log chi tiết tình trạng file và decode bitmap
     */
    private fun processAndLogImage(file: File?, tag: String): Bitmap? {
        if (file == null) {
            LogRecorder.i("CHECK_IMAGE", "$tag: File bị NULL (không có đối tượng file)")
            return null
        }

        if (!file.exists()) {
            LogRecorder.i("CHECK_IMAGE", "$tag: File KHÔNG tồn tại trên bộ nhớ. Path: ${file.absolutePath}")
            return null
        }

        val fileSize = file.length() // tính bằng byte
        val fileSizeKB = fileSize / 1024.0

        LogRecorder.i("CHECK_IMAGE", "$tag: Đường dẫn: ${file.absolutePath}")
        LogRecorder.i("CHECK_IMAGE", "$tag: Dung lượng file: $fileSizeKB KB")

        if (fileSize <= 0) {
            LogRecorder.i("CHECK_IMAGE", "$tag: CẢNH BÁO: File có dung lượng 0 byte (Ảnh bị rỗng/đen)!")
            return null
        }

        // Thử decode file thành bitmap
        return try {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            if (bitmap == null) {
                LogRecorder.i("CHECK_IMAGE", "$tag: LỖI: Không thể decode file thành Bitmap (File có thể bị lỗi định dạng hoặc hỏng)!")
            } else {
                LogRecorder.i("CHECK_IMAGE", "$tag: Decode thành công. Kích thước: ${bitmap.width}x${bitmap.height}")
            }
            bitmap
        } catch (e: Exception) {
            LogRecorder.i("CHECK_IMAGE", "$tag: LỖI CRASH khi decode: ${e.message}")
            null
        }
    }
}
