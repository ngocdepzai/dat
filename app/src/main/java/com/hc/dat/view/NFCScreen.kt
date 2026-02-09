package com.hc.dat.view

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import com.hc.dat.view.adapter.DialogButtonClickListener
import com.lws.device.nfc.NFCAction
import com.lws.device.nfc.NFCEvent
import com.lws.type.LogRecorder
import com.lws.type.Logger
import hc.manager.datapp.R
import hc.manager.datapp.databinding.ActivityNfcBinding

class NFCScreen : DatBaseScreen() {
    private lateinit var viewBinding: ActivityNfcBinding
    private lateinit var currentDialog: AlertDialog

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {

        viewBinding = ActivityNfcBinding.inflate(inflater, container, false)
        initView()
        return viewBinding.root
    }

    private fun initView() {
        viewBinding.btnNFCWrite.setOnClickListener {
            val content: String = viewBinding.edtContent.text.trim().toString()
            Logger.i("data write to nfc $content")
            if (content.isNotBlank() && content.isNotEmpty()) {
                    showCustomNfcDialog()
                startNFCWriting(content = content)
            } else {
                BaseNotification.showWarning(message = "Vui lòng nhập nội dung cần ghi!")
            }
        }
        viewBinding.btnNFCRead.setOnClickListener {
                showCustomNfcDialog()
            startNFCReading()
        }
    }

    private fun startNFCWriting(content: String) {
        appViewModel.startNFCCard(
            nfcAction = NFCAction.NFC_WRITE_DATA,
            dataWriteToNFCCard = content,
            nfcEvent = object : NFCEvent {
                override fun onNFCDataDetected(nfcAction: NFCAction, data: String?) {
                    if (currentDialog.isShowing) {
                        currentDialog.dismiss()
                    }
                    appViewModel.stopNFCCard()
                    when (nfcAction) {
                        NFCAction.NFC_WRITE_DATA -> {
                            if (data != null) {
                                viewBinding.edtContent.text.clear()
                                showDialog(
                                    title = getString(R.string.title_notification),
                                    message = data,
                                    buttonList = listOf(getString(R.string.ok)),
                                    listener = object : DialogButtonClickListener {
                                        override fun onDialogButtonClick(position: Int) {
                                            dismissDialog()
                                        }
                                    }
                                )
                            } else {
                                showDialog(
                                    title = getString(R.string.title_notification),
                                    message = "ghi thẻ thất bại",
                                    buttonList = listOf(getString(R.string.ok)),
                                    listener = object : DialogButtonClickListener {
                                        override fun onDialogButtonClick(position: Int) {
                                            dismissDialog()
                                        }
                                    }
                                )

                            }
                        }

                        else -> {}
                    }

                }

            })
    }

    private fun startNFCReading() {
        Logger.d("startNFCReading")
        appViewModel.startNFCCard(
            nfcEvent = object : NFCEvent {
                override fun onNFCDataDetected(nfcAction: NFCAction, data: String?) {
                    if (currentDialog.isShowing) {
                        currentDialog.dismiss()
                    }
                    appViewModel.stopNFCCard()
                    when (nfcAction) {
                        NFCAction.NFC_DISABLE -> {
                            LogRecorder.e(
                                "Quẹt thẻ RFID",
                                getString(R.string.login_by_nfc_method_note_ready)
                            )
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
                            showDialog(
                                title = getString(R.string.title_notification),
                                message = "Nội dung trong thẻ: \n$data",
                                buttonList = listOf(getString(R.string.ok)),
                                listener = object : DialogButtonClickListener {
                                    override fun onDialogButtonClick(position: Int) {
                                        dismissDialog()
                                    }
                                }
                            )
                        }

                        else -> {}
                    }
                }
            }
        )
    }

    private fun showCustomNfcDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_nfc, null)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .create()
        dialog.setOnShowListener {
            dialog.setOnDismissListener {
                appViewModel.stopNFCCard()
            }
        }
        dialog.show()

        currentDialog = dialog
    }

}