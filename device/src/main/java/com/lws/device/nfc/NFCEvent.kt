package com.lws.device.nfc

interface NFCEvent {
    fun onNFCDataDetected(nfcAction: NFCAction, data: String? = null)
}

enum class NFCAction {
    NFC_DISABLE,
    NFC_DATA_DETECTED,
    NFC_DATA_INCORRECT,
    NFC_WRITE_DATA,
}