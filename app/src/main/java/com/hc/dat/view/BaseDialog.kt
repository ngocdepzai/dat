package com.hc.dat.view

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.hc.dat.view.adapter.DialogButtonAdapter
import com.hc.dat.view.adapter.DialogButtonClickListener
import com.lws.type.Logger
import hc.manager.datapp.R
import hc.manager.datapp.databinding.BaseDialogBinding
import hc.manager.datapp.databinding.ProgressDialogBinding

internal object BaseDialog {
    private var dialog: AlertDialog? = null
    private var progressDialog: AlertDialog? = null

    fun showDialog(
        context: Context,
        title: String?,
        message: String?,
        buttonList: List<String>?,
        cancelable: Boolean,
        listener: DialogButtonClickListener?
    ) {
        Logger.d("showDialog title: $title message: $message cancelable: $cancelable buttonList: $buttonList")
        val view = LayoutInflater.from(context)
            .inflate(R.layout.base_dialog, null, false)
        val viewBinding = BaseDialogBinding.bind(view)
        title?.run {
            viewBinding.dialogTitle.visibility = View.VISIBLE
            viewBinding.dialogTitle.text = this
        } ?: run {
            viewBinding.dialogTitle.visibility = View.GONE
        }
        message?.run {
            viewBinding.dialogMessage.visibility = View.VISIBLE
            viewBinding.dialogMessage.text = this
        } ?: run {
            viewBinding.dialogMessage.visibility = View.GONE
        }
        buttonList?.run {
            viewBinding.dialogButtonList.visibility = View.VISIBLE
            viewBinding.dialogButtonList.apply {
                val linearLayoutManager = LinearLayoutManager(
                    context,
                    if (buttonList.size < 4) LinearLayoutManager.HORIZONTAL
                    else LinearLayoutManager.VERTICAL,
                    false
                )
                this.layoutManager = linearLayoutManager
                adapter = DialogButtonAdapter(
                    list = buttonList,
                    listener = listener
                )
            }
        } ?: run {
            viewBinding.dialogButtonList.visibility = View.GONE
        }

        dialog?.run {
            if (isShowing) dismiss()
        }

        dialog = MaterialAlertDialogBuilder(context)
            .setCancelable(cancelable)
            .setView(viewBinding.root).create()
        dialog?.show()
    }

    fun showProgressDialog(
        context: Context,
        message: String? = null
    ) {
        Logger.d("showProgressDialog")
        val view = LayoutInflater.from(context)
            .inflate(R.layout.progress_dialog, null, false)
        val viewBinding = ProgressDialogBinding.bind(view)

        if (message != null) {
            viewBinding.tvLoadingMessage.text = message
        }
        progressDialog?.dismiss()

        progressDialog = MaterialAlertDialogBuilder(context)
            .setCancelable(false)
            .setView(viewBinding.root).create()
        progressDialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        progressDialog?.show()
    }

    fun dismissProgress() {
        progressDialog?.dismiss()
    }

    fun dismiss() {
        dialog?.dismiss()
    }
}
