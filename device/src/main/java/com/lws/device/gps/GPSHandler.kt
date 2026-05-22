package com.lws.device.gps

import DeviceLogger
import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.GnssStatus
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.lws.type.Logger
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.util.*

class GPSHandler: GPSDevice {

    private var listGPSEventListener: MutableList<GPSEvent> = mutableListOf()

    private val reqPermissionChannel = Channel<Boolean>()
    private var reqPermissionScope: CoroutineScope? = null

    private var fusedLocationProviderClient: FusedLocationProviderClient? = null
    private var locationManager: LocationManager? = null
    // request GPS with short frequency is spent more power
    private var locationRequest: LocationRequest =
        LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000).build()
    private var lastLocation: Location? = null

    companion object {
        private const val ACCESS_COARSE_LOCATION_PERMISSION_REQUEST_CODE = 3
        private const val ACCESS_FINE_LOCATION_PERMISSION_REQUEST_CODE = 4

        private const val PERMISSION_CAMERA_PERMISSION_REQUEST_CODE = 5
        private const val PERMISSION_READ_PHONE_STATE_PERMISSION_REQUEST_CODE = 6
        private const val PERMISSION_WRITE_STORAGE_PERMISSION_REQUEST_CODE = 7
        private const val PERMISSION_READ_STORAGE_PERMISSION_REQUEST_CODE = 8
        private const val PERMISSION_ACCESS_NETWORK_STATE = 9
        val LIST_PERMISSION_REQUIRED: List<Pair<String, Int>> = listOf(
            Pair(Manifest.permission.ACCESS_COARSE_LOCATION, ACCESS_COARSE_LOCATION_PERMISSION_REQUEST_CODE),
            Pair(Manifest.permission.ACCESS_FINE_LOCATION, ACCESS_FINE_LOCATION_PERMISSION_REQUEST_CODE),
            Pair(Manifest.permission.CAMERA, PERMISSION_CAMERA_PERMISSION_REQUEST_CODE),
            Pair(Manifest.permission.READ_PHONE_STATE, PERMISSION_READ_PHONE_STATE_PERMISSION_REQUEST_CODE),
            Pair(Manifest.permission.WRITE_EXTERNAL_STORAGE, PERMISSION_WRITE_STORAGE_PERMISSION_REQUEST_CODE),
            Pair(Manifest.permission.READ_EXTERNAL_STORAGE, PERMISSION_READ_STORAGE_PERMISSION_REQUEST_CODE),
            Pair(Manifest.permission.ACCESS_NETWORK_STATE, PERMISSION_ACCESS_NETWORK_STATE),
        )

        private const val LOCATION_SURVIVE_TIME_MILLIS = 30000
    }

    override var requestPermissionResultCallback: (
        activity: Activity,
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) -> Unit = { activity: Activity, requestCode: Int, permissions: Array<out String>, grantResults: IntArray ->
            Logger.d("requestPermissionResultCallback requestCode: $requestCode | permissions: $permissions | grantResults: $grantResults")
            if (LIST_PERMISSION_REQUIRED.map { it.second }.contains(requestCode)) {
                reqPermissionScope?.launch {
                    checkPermissionRequired(activity)
                }
            }
        }

//    override fun checkGPSAvailable(activity: Activity): Boolean {
//        locationManager = activity.getSystemService(Context.LOCATION_SERVICE) as LocationManager
//        return if (locationManager != null) {
//            val locationGpsEnable = locationManager!!.isProviderEnabled(LocationManager.GPS_PROVIDER)
//            val locationNetworkEnable = locationManager!!.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
//            DeviceLogger.i("locationGpsEnable: $locationGpsEnable | locationNetworkEnable: $locationNetworkEnable")
////            return locationGpsEnable || locationNetworkEnable
//            return locationGpsEnable
//        } else false
//    }

    override fun checkGPSAvailable(activity: Activity): Boolean {
        val locationManager = activity.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        if (locationManager == null) return false

        // 1. Kiểm tra xem Provider có được bật trong cài đặt hay không
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        if (!isGpsEnabled && !isNetworkEnabled) {
            DeviceLogger.i("GPS và Network đều đang tắt")
            return false
        }

        try {
            // 2. Lấy vị trí gần nhất từ GPS hoặc Network
            val lastGpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            val lastNetworkLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

            // 3. Kiểm tra tính hợp lệ của tọa độ (khác null và khác 0.0)
            val isGpsValid = lastGpsLocation != null &&
                    lastGpsLocation.latitude != 0.0 &&
                    lastGpsLocation.longitude != 0.0

            val isNetworkValid = lastNetworkLocation != null &&
                    lastNetworkLocation.latitude != 0.0 &&
                    lastNetworkLocation.longitude != 0.0

            DeviceLogger.i("GPS Enable: $isGpsEnabled (Valid: $isGpsValid) | Network Enable: $isNetworkEnabled (Valid: $isNetworkValid)")

            // Trả về true nếu bất kỳ provider nào đang bật VÀ có dữ liệu tọa độ thật
            return (isGpsEnabled && isGpsValid) || (isNetworkEnabled && isNetworkValid)

        } catch (e: SecurityException) {
            DeviceLogger.e("Thiếu quyền truy cập vị trí: ${e.message}")
            return false
        } catch (e: Exception) {
            DeviceLogger.e("Lỗi kiểm tra GPS: ${e.message}")
            return false
        }
    }

    override fun addGPSEventListener(gpsEvent: GPSEvent) {
        if (!listGPSEventListener.contains(gpsEvent)) {
            listGPSEventListener.add(gpsEvent)
        }
    }

    override fun removeGPSEventListener(gpsEvent: GPSEvent) {
        Logger.d("stopGPSServiceListener")
        listGPSEventListener.remove(gpsEvent)

    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun stopGPSService() {
        locationManager?.unregisterGnssStatusCallback(gnssStatusCallback)
        fusedLocationProviderClient?.removeLocationUpdates(locationCallback)
    }

    @SuppressLint("MissingPermission")
    override suspend fun startGPSService(activity: Activity) {
        Logger.d("startGPSService")

        reqPermissionScope = CoroutineScope(Dispatchers.Default)

        reqPermissionScope?.launch {
            checkPermissionRequired(activity)
        }
        var limitTimes = 10
        while (!reqPermissionChannel.receive() && limitTimes > 0) {
            Logger.w("Exist permissions must be required by function's mandatory!")
            limitTimes--
        }
        if (limitTimes <= 0) {
            reqPermissionScope?.cancel()
            return
        }

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(activity)
        locationManager = activity.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (locationManager != null) {
            fusedLocationProviderClient?.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        }
    }
    @RequiresApi(Build.VERSION_CODES.N)
    override fun startSatelliteCounter(activity: Activity){
        locationManager = activity.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (ActivityCompat.checkSelfPermission(
                activity,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        locationManager!!.registerGnssStatusCallback(gnssStatusCallback)
    }

    override fun getLatestLocation(): Location? {
        return lastLocation?.let {
            val currentTime = Calendar.getInstance().timeInMillis
            DeviceLogger.i("current currentTime: $currentTime | location time: ${it.time}")
            // location will be expired if time living over LOCATION_SURVIVE_TIME_MILLIS
            if (currentTime - it.time > LOCATION_SURVIVE_TIME_MILLIS) null
            else it
        }
    }

    override fun checkMoving(): Boolean? {
        return lastLocation?.let {
            val currentTime = Calendar.getInstance().timeInMillis
            DeviceLogger.i("current currentTime: $currentTime | location time: ${it.time}")
            // location will be expired if time living over LOCATION_SURVIVE_TIME_MILLIS
            val checkExpired = currentTime - it.time
            DeviceLogger.i("checkExpired: $checkExpired | it.hasSpeed(): ${it.hasSpeed()} | speed: ${it.speed}")
            if (currentTime - it.time > LOCATION_SURVIVE_TIME_MILLIS) null
            else (it.hasSpeed() && it.speed > 5F)
        }
    }

    private val gnssStatusCallback = @RequiresApi(Build.VERSION_CODES.N)
    object : GnssStatus.Callback() {
        override fun onSatelliteStatusChanged(status: GnssStatus) {
            super.onSatelliteStatusChanged(status)
            val satelliteCount = status.satelliteCount
            var gpsSatellitesInUse = 0
            for (i in 0 until satelliteCount) {
                if (status.getConstellationType(i) == GnssStatus.CONSTELLATION_GPS
                    && status.usedInFix(i)
                ) {
                    gpsSatellitesInUse++
                }
            }
            listGPSEventListener.forEach {
                it.onGPSUpdate(GPSAction.SATELLITE_COUNT_UPDATED, "$gpsSatellitesInUse")
            }
        }
    }
    private val locationCallback = object: LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            super.onLocationResult(locationResult)
            checkMoving()
            lastLocation = locationResult.lastLocation
            DeviceLogger.i("onLocationResult lastLocation: $lastLocation")
            listGPSEventListener.forEach {
                it.onGPSUpdate(GPSAction.LOCATION_UPDATED, locationResult.lastLocation)
            }
        }

        override fun onLocationAvailability(p0: LocationAvailability) {
            super.onLocationAvailability(p0)
            DeviceLogger.i("onLocationAvailability LocationAvailability: ${p0.isLocationAvailable}")
            listGPSEventListener.forEach {
                it.onGPSUpdate(GPSAction.GPS_SETTING_CHANGED, p0.isLocationAvailable)
            }
            if (!p0.isLocationAvailable) lastLocation = null
        }
    }

    private suspend fun checkPermissionRequired(activity: Activity) {
        DeviceLogger.d("checkPermissionRequired")
        reqPermissionScope?.run {
            DeviceLogger.i("checkPermissionRequired isActive: $isActive")
            if (isActive) {
                LIST_PERMISSION_REQUIRED.firstOrNull {
                    DeviceLogger.i("checkPermissionRequired request permission: ${it.first} | check: ${ActivityCompat.checkSelfPermission(activity, it.first)}")
                    ActivityCompat.checkSelfPermission(activity, it.first) != PackageManager.PERMISSION_GRANTED
                }?.run {
                    DeviceLogger.i("checkPermissionRequired request permission: ${this.first}")
                    ActivityCompat.requestPermissions(
                        activity, arrayOf(this.first), this.second
                    )
                    reqPermissionChannel.send(false)
                } ?: reqPermissionChannel.send(true)
            }
        }
    }
}