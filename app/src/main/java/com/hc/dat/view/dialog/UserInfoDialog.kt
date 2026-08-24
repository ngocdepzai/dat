package com.hc.dat.view.dialog

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import coil.request.ImageRequest
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.hc.dat.model.database.entity.Gender
import com.hc.dat.model.database.entity.UserEntity
import com.hc.dat.model.database.entity.UserType
import com.hc.dat.service.ServiceDefinition
import com.hc.dat.utils.ImageLoader
import com.hc.dat.view.BaseNotification
import com.lws.type.Logger
import hc.manager.datapp.R

internal object UserInfoDialog {
    private var dialog: AlertDialog? = null

    fun showDialog(
        context: Context,
        userEntity: UserEntity,
        captureImagePath: String? = null,
        callback: (confirm: Boolean, data: UserEntity) -> Unit
    ) {
        Logger.d("showDialog userEntity: $userEntity")
        val isTeacher = userEntity.userType != UserType.STUDENT.code
        val layoutRes = if (isTeacher) {
            R.layout.dat_user_info_dialog_teacher
        } else {
            R.layout.dat_user_info_dialog
        }
        val view = LayoutInflater.from(context).inflate(layoutRes, null, false)

        val tvTitle = view.findViewById<TextView>(R.id.tvTitle)
        val tvFullName = view.findViewById<TextView>(R.id.tvFullName)
        val tvPhoneNumber = view.findViewById<TextView>(R.id.tvPhoneNumber)
        val tvIdNumber = view.findViewById<TextView>(R.id.tvIdNumber)
        val tvGender = view.findViewById<TextView>(R.id.tvGender)
        val tvBirthday = view.findViewById<TextView>(R.id.tvBirthday)
        val tvAddressInfo = view.findViewById<TextView>(R.id.tvAddressInfo)
        val imgAvatar = view.findViewById<ImageView>(R.id.imgAvatar)
        val vCapture = view.findViewById<View>(R.id.vCapture)
        val ivCapture = view.findViewById<ImageView>(R.id.ivCapture)
        val btCancelDialog = view.findViewById<Button>(R.id.btCancelDialog)
        val btConfirm = view.findViewById<Button>(R.id.btConfirm)

        tvTitle.text = if (userEntity.userType == UserType.STUDENT.code) {
            context.getString(R.string.student_info_title)
        } else context.getString(R.string.teacher_info_title)
        tvFullName.text = userEntity.fullName
        tvPhoneNumber.text = userEntity.phoneNumber
        tvIdNumber.text = userEntity.userCode
        tvGender.text = Gender.findByCode(userEntity.gender).valueName
        tvBirthday.text = userEntity.birthday
        tvAddressInfo.text = userEntity.address
        Logger.i("Image link: ${ServiceDefinition.IMAGE_RESIZE_URL}${userEntity.avatarId}")
        userEntity.avatarId?.also {
            val request = ImageRequest.Builder(context)
                .data("${ServiceDefinition.IMAGE_FULL_SIZE_URL}$it")
                .setHeader("User-Agent", "Mozilla/5.0")
                .crossfade(true)
                .placeholder(R.drawable.ic_loading)
                .allowHardware(false)
                .target(imgAvatar)
                .build()
            ImageLoader.imageLoader?.enqueue(request)
        }
            ?: also { imgAvatar.setImageDrawable(context.getDrawable(R.drawable.nonavatar)) }

        captureImagePath?.also {
            vCapture.visibility = View.VISIBLE
            val request = ImageRequest.Builder(context)
                .data(captureImagePath)
                .crossfade(true)
                .allowHardware(false)
                .target(ivCapture)
                .build()
            ImageLoader.imageLoader?.enqueue(request)
        }

        dialog?.run {
            if (isShowing) dismiss()
        }

        dialog = MaterialAlertDialogBuilder(context)
            .setCancelable(false)
            .setView(view).create()
        dialog?.setOnShowListener {
            dialog?.window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        dialog?.show()

        if (!isTeacher) {
            btCancelDialog.visibility = View.INVISIBLE
            btConfirm.visibility = View.INVISIBLE
        }
        BaseNotification.speakWithCallback(context.getString(R.string.confirm_user_info_message)) {
            Handler(Looper.getMainLooper()).post {
                btCancelDialog.visibility = View.VISIBLE
                btConfirm.visibility = View.VISIBLE
            }
        }

        btCancelDialog.setOnClickListener {
            dialog?.dismiss()
            dialog = null
            callback(false, userEntity)
        }
        btConfirm.setOnClickListener {
            dialog?.dismiss()
            dialog = null
            callback(true, userEntity)
        }
    }

    fun dismiss() {
        dialog?.dismiss()
        dialog = null
    }
}
