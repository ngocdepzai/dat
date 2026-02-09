package com.lws.device.network

import android.app.Activity

interface NetworkConnection {
    val requestPermissionResultCallback: (
        activity: Activity,
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) -> Unit
    fun startNetworkConnectionService(activity: Activity)
    fun checkConnectionAvailable(): Boolean
    fun setNetworkConnectionEventListener(event: NetworkConnectionEvent)
//    fun removeNetworkConnectionEventListener(event: NetworkConnectionEvent)
}
