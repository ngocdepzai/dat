package com.hc.dat.model

import android.location.Location
import com.hc.dat.utils.KalmanSpeedFilter
import com.hc.dat.utils.Utils
import com.hc.dat.utils.Utils.isValidGpsSpeed
import com.hc.dat.viewmodel.VerifyResult
import com.lws.type.LogRecorder
import com.lws.type.Logger
import java.io.File

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
//    fun setLastLocation(location: Location) {
//        // filter location has lat, long incorrect in vietnam geo
//        if (location.latitude > 8 && location.latitude < 23 && location.longitude > 102 && location.longitude < 110
//        ) {
//            // calculate distance base on max speed assumption is not more than 100km/h -> not than 28m/s
//            val calculateDistance: Float = latestLocation?.distanceTo(location) ?: 0.0f
////            val durationCalculate: Int = if (latestLocation != null) ((location.time - latestLocation!!.time).toInt() / 1000) else 0
//            val durationCalculate: Float = if (latestLocation != null) ((location.time - latestLocation!!.time) / 1000f) else 0f
//            val speedCalculate = if (durationCalculate > 0f) calculateDistance / durationCalculate else 0f
//            Logger.i("HoangSpeed so sánh speed & speedCalculate: " + "calculateDistance: $calculateDistance |  durationCalculate: $durationCalculate | speedCalculate: $speedCalculate | speed: ${location.speed} | latitude: ${location.latitude} | longitude: ${location.longitude}  | time: ${location.time}")
//            LogRecorder.i("Lấy GPS thành công so sánh speed & speedCalculate", "calculateDistance: $calculateDistance |  durationCalculate: $durationCalculate | speedCalculate: $speedCalculate | speed: ${location.speed} | latitude: ${location.latitude} | longitude: ${location.longitude}  | time: ${location.time}")
//
//            if (latestLocation == null || speedCalculate <= 28F) {
//                latestLocation?.apply {
//                    distance = this.distanceTo(location)
//                    duration = location.time - this.time
//                }
//                latestLocation = location
//
//                if (isValidGpsSpeed(location, speedCalculate)) {
//                    speed = Utils.convertLocationSpeed(location)
//                } else {
//                    speed = Utils.convertLocationSpeedCalculate(speedCalculate)
//                }
//
//                lat = location.latitude
//                long = location.longitude
//                lastLocationUpdateTime = Utils.getRealTimeStamp()/1000
//            } else {
//                Logger.e("Warning: Location maybe fake , because distance get than 28m per second -> ignore this location")
//                latestLocation = location
//            }
//        } else {
//            Logger.e("Wrong location: lat: ${location.latitude} | long: ${location.longitude}")
//        }
//    }

    fun setLastLocation(location: Location) {
        LogRecorder.i(
                "GPS_RAW",
                "lat=${location.latitude}, lng=${location.longitude}, speed=${location.speed}, acc=${location.accuracy}, time=${location.time}"
        )

        // ✅ RULE 0: Check VN boundary
        if (location.latitude !in 8.0..23.0 || location.longitude !in 102.0..110.0) {
            Logger.e("Wrong location: lat: ${location.latitude} | long: ${location.longitude}")
            LogRecorder.i("GPS_DROP", "❌ OUT_OF_BOUND lat=${location.latitude}, lng=${location.longitude}")
            this.distance = 0f
            return
        }

        val prev = latestLocation

        if (prev != null) {

            val distance = prev.distanceTo(location) // mét
            val duration = (location.time - prev.time) / 1000f // giây

            if (duration <= 0) {
                LogRecorder.i("GPS_DROP", "❌ INVALID_TIME duration=$duration")
                this.distance = 0f
                return
            }

            val speedCalculate = distance / duration // m/s
            LogRecorder.i(
                    "GPS_CHECK",
                    "distance=$distance m | duration=$duration s | speedCalc=$speedCalculate m/s | rawSpeed=${location.speed} | acc=${location.accuracy}"
            )

            // 🚨 RULE 1: tốc độ không thực tế ( >144km/h )
            if (speedCalculate > 40f) {
                Logger.e("GPS JUMP (speed too high)")
                LogRecorder.i("GPS_DROP", "❌ JUMP_SPEED distance=$distance duration=$duration speedCalc=$speedCalculate")
                this.distance = 0f
                return
            }

            // 🚨 RULE 2: jump khoảng cách lớn
            if (distance > 1000f) {
                Logger.e("GPS JUMP (distance too far)")
                LogRecorder.i("GPS_DROP", "❌ JUMP_DISTANCE distance=$distance")
                this.distance = 0f
                return
            }

            // 🚨 RULE 3: accuracy kém
            if (location.accuracy > 30f) {
                Logger.e("GPS accuracy too low")
                LogRecorder.i("GPS_DROP", "❌ BAD_ACCURACY acc=${location.accuracy}")
                this.distance = 0f
                return
            }

            // 🚨 RULE 4: đứng im nhưng speed cao
            if (distance < 10 && location.speed > 20) {
                Logger.e("GPS fake speed")
                LogRecorder.i("GPS_DROP", "❌ FAKE_SPEED distance=$distance speed=${location.speed}")
                this.distance = 0f
                return
            }

            // ✅ ACCEPT → update distance
            this.distance = distance
            this.duration = (location.time - prev.time)
            LogRecorder.i(
                    "GPS_ACCEPT",
                    "✅ VALID distance=$distance duration=$duration"
            )

            // ✅ xử lý speed
            speed = if (isValidGpsSpeed(location, speedCalculate)) {
                Utils.convertLocationSpeed(location)
            } else {
                LogRecorder.i(
                        "GPS_SPEED_FALLBACK",
                        "⚠️ USE_CALCULATED speedCalc=$speedCalculate raw=${location.speed}"
                )
                Utils.convertLocationSpeedCalculate(speedCalculate)
            }

        } else {
            LogRecorder.i("GPS_INIT", "First valid location accepted")
            speed = Utils.convertLocationSpeed(location)
        }

        // ✅ CHỈ update latestLocation khi PASS
        latestLocation = location
        lat = location.latitude
        long = location.longitude
        lastLocationUpdateTime = Utils.getRealTimeStamp() / 1000

        LogRecorder.i(
                "GPS_UPDATE",
                "lat=$lat, lng=$long, speed=$speed km/h, totalDistance=$distance"
        )
    }

    fun getSpeed(): Double = speed
}
