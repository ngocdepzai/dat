package com.lws.device

import android.app.Activity
import android.content.IntentFilter
import android.nfc.NfcAdapter
import com.lws.device.camera.CameraDevice
import com.lws.device.camerapreview.CameraPreviewDevice
import com.lws.device.gps.GPSDevice
import com.lws.device.network.NetworkConnection
import com.lws.device.nfc.NFCDevice

class Device constructor(
//    private val printerDevice: PrinterDevice?,
    private val cameraDevice: CameraDevice?,
    private val nfcDevice: NFCDevice?,
    private val gpsDevice: GPSDevice?,
    private val networkConnection: NetworkConnection?,
    private val cameraPreviewDevice: CameraPreviewDevice?,
) {
    private lateinit var activity: Activity
    private var searchThreshold: Float? = null
//    private var nfcAdapter: NfcAdapter? = null

    companion object {
        private const val FLAGS_READER = NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK
        private val NFC_ACTION_FILTER = arrayOf(
            IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED),
            IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED),
            IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED)
        )
        const val NFC_READER_REQUEST_CODE: Int = 0
    }

    fun connectDevice(activity: Activity) {
        this.activity = activity
//        CoroutineScope(Dispatchers.Default).launch {
//            delay(5000)
////            createFacePassHandler()
//        }
//        createFacePassHandler()
        cameraPreviewDevice?.startExtraCameraEventListener()
        networkConnection?.startNetworkConnectionService(this.activity)
    }

    fun disconnectDevice() {
        releaseFacePass()
    }
    fun setSearchThreshold(searchThreshold: Float?){
        this.searchThreshold = searchThreshold
    }

    fun getSearchThreshold(): Float?{
        return searchThreshold
    }
    private fun releaseFacePass() {
    }


//    fun getCurrentPrinter(): PrinterDevice? = printerDevice
    fun getCurrentCamera(): CameraDevice? = cameraDevice
    fun getCurrentNFC(): NFCDevice? = nfcDevice
    fun getCurrentGPS(): GPSDevice? = gpsDevice
    fun getCurrentNetworkConnection(): NetworkConnection? = networkConnection
    fun getCameraPreview(): CameraPreviewDevice? = cameraPreviewDevice
}
