package com.hc.dat.view.dialog

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import coil.request.ImageRequest
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.hc.dat.model.database.entity.Gender
import com.hc.dat.model.database.entity.UserEntity
import com.hc.dat.model.database.entity.UserType
import com.hc.dat.service.ServiceDefinition
import com.hc.dat.utils.ImageLoader
import com.lws.type.Logger
import hc.manager.datapp.R
import hc.manager.datapp.databinding.DatUserInfoDialogBinding

internal object UserInfoDialog {
    private var dialog: AlertDialog? = null

    fun showDialog(
        context: Context,
        userEntity: UserEntity,
        captureImagePath: String? = null,
        callback: (confirm: Boolean, data: UserEntity) -> Unit
    ) {
        Logger.d("showDialog userEntity: $userEntity")
        val view = LayoutInflater.from(context)
            .inflate(R.layout.dat_user_info_dialog, null, false)
        val viewBinding = DatUserInfoDialogBinding.bind(view)
        viewBinding.tvTitle.text = if (userEntity.userType == UserType.STUDENT.code) {
            context.getString(R.string.student_info_title) 
        }else context.getString(R.string.teacher_info_title)
        viewBinding.tvFullName.text = userEntity.fullName
        viewBinding.tvPhoneNumber.text = userEntity.phoneNumber
        viewBinding.tvIdNumber.text = userEntity.userCode
        viewBinding.tvGender.text = Gender.findByCode(userEntity.gender).valueName
        viewBinding.tvBirthday.text = userEntity.birthday
        viewBinding.tvAddressInfo.text = userEntity.address
        Logger.i("Image link: ${ServiceDefinition.IMAGE_RESIZE_URL}${userEntity.avatarId}")
        userEntity.avatarId?.also {
//            ImageUtil.imageLoader
//                ?.load("${ServiceDefinition.IMAGE_RESIZE_URL}$it")
//                ?.into(viewBinding.imgAvatar,)
            val request = ImageRequest.Builder(context)
                .data("${ServiceDefinition.IMAGE_FULL_SIZE_URL}$it")
                .setHeader("User-Agent", "Mozilla/5.0")
                .crossfade(true)
                .placeholder(R.drawable.ic_loading)
                .allowHardware(false)
                .target(viewBinding.imgAvatar)
                .build()
            ImageLoader.imageLoader?.enqueue(request)
        }
            ?: also { viewBinding.imgAvatar.setImageDrawable(context.getDrawable(R.drawable.nonavatar)) }

        captureImagePath?.also {
            viewBinding.vCapture.visibility = View.VISIBLE
            val request = ImageRequest.Builder(context)
                .data(captureImagePath)
                .crossfade(true)
                .allowHardware(false)
                .target(viewBinding.ivCapture)
                .build()
            ImageLoader.imageLoader?.enqueue(request)
        }

        dialog?.run {
            if (isShowing) dismiss()
        }

        dialog = MaterialAlertDialogBuilder(context)
            .setCancelable(false)
            .setView(viewBinding.root).create()
        dialog?.setOnShowListener {
            dialog?.window?.setLayout(
                view.width + 100,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        dialog?.show()

        viewBinding.btCancelDialog.setOnClickListener {
            callback(false, userEntity)
            dismiss()
        }
        viewBinding.btConfirm.setOnClickListener {
            callback(true, userEntity)
            dismiss()
        }
    }

    fun dismiss() {
        dialog?.dismiss()
    }
}
