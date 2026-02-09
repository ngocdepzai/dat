package com.lws.device.nfc

import android.app.Activity
import android.content.Intent

interface NFCDevice {
    val newIntentCallback: (activity: Activity, data: Intent?) -> Unit
    suspend fun waitNFCDataDetected(
        nfcEvent: NFCEvent?,
        nfcAction: NFCAction? = null,
        dataWriteToNFCCard: String = ""
    )
}
