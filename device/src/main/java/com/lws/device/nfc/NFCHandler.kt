package com.lws.device.nfc

import DeviceLogger
import android.app.Activity
import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import com.lws.type.Logger
import com.lws.type.convertToString
import java.io.UnsupportedEncodingException
import java.nio.charset.Charset
import kotlin.experimental.and

class NFCHandler: NFCDevice {

    private var nfcEvent: NFCEvent? = null
    private var nfcAction: NFCAction? = null
    private var dataWriteToNFCCard: String = ""

    companion object {}

    override val newIntentCallback: (activity: Activity, intent: Intent?)
    -> Unit = { _: Activity, intent: Intent? ->
        DeviceLogger.d("newIntentCallback intent: $intent")
        intent?.also {
            Logger.i("nfcEvent: ${nfcAction?.name}")
            if (nfcAction == NFCAction.NFC_WRITE_DATA) {
                val result = writeNfcTag(it, dataWriteToNFCCard)
                this.nfcEvent?.onNFCDataDetected(
                    nfcAction = NFCAction.NFC_WRITE_DATA,
                    result
                )
            } else {
                val readingResult: String? = extractMessageFromNdefMessage(it)
                DeviceLogger.d("newIntentCallback readingResult: $readingResult")
                readingResult?.also { result ->
                    this.nfcEvent?.onNFCDataDetected(
                        nfcAction = NFCAction.NFC_DATA_DETECTED,
                        result
                    )
                } ?: this.nfcEvent?.onNFCDataDetected(nfcAction = NFCAction.NFC_DATA_INCORRECT)
            }

        }
    }

    override suspend fun waitNFCDataDetected(
        nfcEvent: NFCEvent?,
        nfcAction: NFCAction?,
        dataWriteToNFCCard: String
    ) {
        this.nfcEvent = nfcEvent
        this.nfcAction = nfcAction
        this.dataWriteToNFCCard = dataWriteToNFCCard
    }

    private fun extractMessageFromNdefMessage(intent: Intent): String? {
        Logger.d("extractMessageFromNdefMessage")
        var result: String? = null
        intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)?.also { parcelableArray ->
            if (parcelableArray.isNotEmpty()) {
                val ndefMessage: NdefMessage = parcelableArray.first() as NdefMessage
                ndefMessage.records?.also { ndefRecords ->
                    Logger.i("ndefRecords: $ndefRecords")
                    if (ndefRecords.isNotEmpty()) {
                        val ndefRecord: NdefRecord = ndefRecords.first()
                        Logger.i("ndefRecord: $ndefRecord")
                        val payload = ndefRecord.payload
                        try {
                            val encoding = if ((payload[0] and 128.toByte()).toInt() == 0) "UTF-8" else "UTF-16"
                            val languageLength = (payload[0] and 51).toInt()
                            result = String(payload, languageLength + 1, payload.size - languageLength - 1, Charset.forName(encoding))
                            Logger.i("stringResult: $result")
                            // lấy mã serial của thẻ
                            val hexdump: String? = intent.getByteArrayExtra(
                                NfcAdapter.EXTRA_ID
                            )?.convertToString()
                            Logger.i("hexdump: $hexdump")
                        } catch (e: UnsupportedEncodingException) {
                            DeviceLogger.e("Error: ${e.message}")
                        }
                    }
                }
            }
        }
        return result
    }


    private fun writeNfcTag(intent: Intent, content: String): String? {
        val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
        if (tag != null) {
            return writeNfcTag(tag, content)
        }
        return null
    }
    private fun writeNfcTag(tag: Tag, text: String): String? {
        try {
            val ndef = Ndef.get(tag)

            if (ndef != null) {
                ndef.connect()
                val mimeType = "text/plain"
                val textBytes = text.toByteArray(Charset.forName("UTF-8"))
                val ndefRecord = NdefRecord.createTextRecord("en", text)
                val ndefMessage = NdefMessage(arrayOf(ndefRecord))

                return if (ndef.maxSize >= ndefMessage.toByteArray().size && ndef.isWritable) {
                    ndef.writeNdefMessage(ndefMessage)
                    Logger.i("vinhdt: Đã ghi thành công vào thẻ NFC")
                    " Đã ghi thành công vào thẻ NFC"

                } else {
                    Logger.i("vinhdt: Không thể ghi vào thẻ NFC hoặc thẻ đã đầy")
                    null
                }

                ndef.close()
            } else {
                val ndefFormatable = NdefFormatable.get(tag)
                return if (ndefFormatable != null) {
                    ndefFormatable.connect()
                    val ndefMessage = NdefMessage(arrayOf(NdefRecord.createTextRecord("en", text)))
                    ndefFormatable.format(ndefMessage)
                    ndefFormatable.close()
                    Logger.i("vinhdt: Đã định dạng và ghi thành công vào thẻ NFC")
                    "Đã định dạng và ghi thành công vào thẻ NFC"
                } else {
                    Logger.i("vinhdt: Thẻ NFC không hỗ trợ NDEF")
                    null
                }
            }
        } catch (e: Exception) {
            Logger.i("vinhdt: Lỗi: ${e.message}")
            return null
        }
    }
}