package com.lws.device.gps

import android.app.Activity
import android.location.Location

interface GPSDevice {
//    val activityResultCallback: (activity: Activity, requestCode: Int, resultCode: Int, data: Intent?) -> Unit
    val requestPermissionResultCallback: (
        activity: Activity,
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) -> Unit
    fun checkGPSAvailable(activity: Activity): Boolean
    suspend fun startGPSService(activity: Activity)
    fun stopGPSService()
    fun addGPSEventListener(gpsEvent: GPSEvent)
    fun removeGPSEventListener(gpsEvent: GPSEvent)
    fun checkMoving(): Boolean?
    fun getLatestLocation(): Location?
    fun startSatelliteCounter(activity: Activity)
}
