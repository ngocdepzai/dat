package com.hc.dat.model

import android.location.Location
import com.hc.dat.utils.Utils
import com.hc.dat.utils.Utils.isValidGpsSpeed
import com.hc.dat.viewmodel.VerifyResult
import com.lws.type.LogRecorder
import com.lws.type.Logger
import java.io.File

/**
 * Created by Duc Bui on 2023/06.
 * Author: Duc Bui
 * Email: ducbui1890@gmail.com
 * Hanoi, VN.
 */
data class SessionVerificationInfo(
    private var latestLocation: Location? = null,
    var lat: Double = 0.0,
    var long: Double = 0.0,
    var distance: Float = 0f,
    var duration: Long = 0L,
    var timeAuth: Long? = null,
    private var speed: Double = 0.0,
    var lastLocationUpdateTime: Long = 0L,
    var verifyResult: VerifyResult = VerifyResult.VERIFY_SUCCESS,
    var searchScore: Float? = null,
//    var faceImageData: FaceImageData? = null,
    var faceImageFile: File? = null,
    var studentImageAuthUrl: String? = null,
    var studentImageAuthPath: String? = null,
    var studentImageLogoutUrl: String? = null,
    var studentImageLogoutPath: String? = null
) {
    fun setLastLocation(location: Location) {
//        Logger.i("Location lat: ${location.latitude} | long: ${location.longitude} | accuracy: ${location.accuracy}")
//        Logger.i("Location hasSpeed: ${location.hasSpeed()} | speed: ${location.speed*(3600/1000)}KM/h")
        // filter location has lat, long incorrect in vietnam geo
        if (location.latitude > 8 && location.latitude < 23 && location.longitude > 102 && location.longitude < 110
        ) {
            // calculate distance base on max speed assumption is not more than 100km/h -> not than 28m/s
            val calculateDistance: Float = latestLocation?.distanceTo(location) ?: 0.0f
//            val durationCalculate: Int = if (latestLocation != null) ((location.time - latestLocation!!.time).toInt() / 1000) else 0
            val durationCalculate: Float = if (latestLocation != null) ((location.time - latestLocation!!.time) / 1000f) else 0f
            val speedCalculate = if (durationCalculate > 0f) calculateDistance / durationCalculate else 0f
//            Logger.i("Location calculateDistance: $calculateDistance | durationCalculate: $durationCalculate")
//            Logger.i("Location speedCalculate: $speedCalculate compare with 28m/s")
            Logger.i("HoangSpeed so sánh speed & speedCalculate: " + "calculateDistance: $calculateDistance |  durationCalculate: $durationCalculate | speedCalculate: $speedCalculate | speed: ${location.speed} | latitude: ${location.latitude} | longitude: ${location.longitude}  | time: ${location.time}")
            LogRecorder.i("Lấy GPS thành công so sánh speed & speedCalculate", "calculateDistance: $calculateDistance |  durationCalculate: $durationCalculate | speedCalculate: $speedCalculate | speed: ${location.speed} | latitude: ${location.latitude} | longitude: ${location.longitude}  | time: ${location.time}")
            /**
             * comment logic check jump gps because can not calculate when case speed always return 0
             */
//            val accelerationCalculate: Float = if (latestLocation != null)
//                    ((location.speed - latestLocation!!.speed)/(durationCalculate))
//            else 0F
//            val v0t: Float = if (latestLocation != null) latestLocation?.speed!! * durationCalculate else 0.0F
//            val at2: Float = accelerationCalculate * durationCalculate * durationCalculate
//            val distanceEstimate: Float = (v0t + at2/2)
//            // gps distance pass when it's not more than x2 distanceEstimate
//            val checkJumpGps = (calculateDistance/2) <= distanceEstimate
//            Logger.i("Location calculateDistance: $calculateDistance | accelerationCalculate: $accelerationCalculate | distanceEstimate: $distanceEstimate | checkJumpGps: $checkJumpGps")
//            if (speedCalculate <= 28F && checkJumpGps)
//                BaseNotification.showMessage("Thỏa mãn: $calculateDistance mét")
//            else
//                BaseNotification.showError("Loại bỏ: $calculateDistance mét")
            // latestLocation is null if first location when start app
//            if (latestLocation == null || (speedCalculate <= 28F && checkJumpGps)) {

            if (latestLocation == null || speedCalculate <= 28F) {
                latestLocation?.apply {
                    distance = this.distanceTo(location)
                    duration = location.time - this.time
                }
                latestLocation = location

                if (isValidGpsSpeed(location, speedCalculate)) {
                    speed = Utils.convertLocationSpeed(location)
                } else {
                    speed = Utils.convertLocationSpeedCalculate(speedCalculate)
                }

                lat = location.latitude
                long = location.longitude
                lastLocationUpdateTime = Utils.getRealTimeStamp()/1000
            } else {
                Logger.e("Warning: Location maybe fake , because distance get than 28m per second -> ignore this location")
                latestLocation = location
            }
        } else {
            Logger.e("Wrong location: lat: ${location.latitude} | long: ${location.longitude}")
        }
    }

    fun getSpeed(): Double = speed
}
