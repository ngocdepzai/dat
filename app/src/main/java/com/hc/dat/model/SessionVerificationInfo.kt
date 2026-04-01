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
    fun setLastLocation(location: Location) {
        // filter location has lat, long incorrect in vietnam geo
        if (location.latitude > 8 && location.latitude < 23 && location.longitude > 102 && location.longitude < 110
        ) {
            // calculate distance base on max speed assumption is not more than 100km/h -> not than 28m/s
            val calculateDistance: Float = latestLocation?.distanceTo(location) ?: 0.0f
//            val durationCalculate: Int = if (latestLocation != null) ((location.time - latestLocation!!.time).toInt() / 1000) else 0
            val durationCalculate: Float = if (latestLocation != null) ((location.time - latestLocation!!.time) / 1000f) else 0f
            val speedCalculate = if (durationCalculate > 0f) calculateDistance / durationCalculate else 0f
            Logger.i("HoangSpeed so sánh speed & speedCalculate: " + "calculateDistance: $calculateDistance |  durationCalculate: $durationCalculate | speedCalculate: $speedCalculate | speed: ${location.speed} | latitude: ${location.latitude} | longitude: ${location.longitude}  | time: ${location.time}")
            LogRecorder.i("Lấy GPS thành công so sánh speed & speedCalculate", "calculateDistance: $calculateDistance |  durationCalculate: $durationCalculate | speedCalculate: $speedCalculate | speed: ${location.speed} | latitude: ${location.latitude} | longitude: ${location.longitude}  | time: ${location.time}")

            val isDistanceJump = calculateDistance > (28f * durationCalculate)
            val durationTooLong = durationCalculate > 30f

            if (latestLocation == null || (speedCalculate <= 28f && !isDistanceJump)) {
                LogRecorder.i("GPS accepted | ",
                         "distance=$calculateDistance m | " +
                                "duration=$durationCalculate s | " +
                                "speedCalculate=$speedCalculate m/s | " +
                                "speed=${location.speed} m/s | " +
                                "accuracy=${location.accuracy} m | " +
                                "lat=${location.latitude} | " +
                                "long=${location.longitude}"
                )
            } else {
                LogRecorder.i("GPS ignored - maybe jump | ",
                         "distance=$calculateDistance m | " +
                                "duration=$durationCalculate s | " +
                                "speedCalculate=$speedCalculate m/s | " +
                                "maxAllowedDistance=${28f * durationCalculate} m | " +
                                "isDistanceJump=$isDistanceJump | " +
                                "speed=${location.speed} m/s | " +
                                "accuracy=${location.accuracy} m | " +
                                "oldLat=${latestLocation?.latitude} | " +
                                "oldLong=${latestLocation?.longitude} | " +
                                "newLat=${location.latitude} | " +
                                "newLong=${location.longitude}"
                )
            }

            if (latestLocation == null || speedCalculate <= 28) {
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
                Logger.e("Warning: Location maybe fake, because distance get than 40m per second -> ignore this location")
                LogRecorder.e("Warning: Location maybe fake, ","because distance get than 40m per second -> ignore this location")

                // nếu quá lâu mới có GPS tiếp theo thì chấp nhận cập nhật latestLocation
                if (durationTooLong) {
                    latestLocation = location
                    Logger.e("Accept new latestLocation because duration too long: $durationCalculate s")
                    LogRecorder.e("durationTooLong: ","Accept new latestLocation because duration too long: $durationCalculate s")
                }
            }
        } else {
            Logger.e("Wrong location: lat: ${location.latitude} | long: ${location.longitude}")
            LogRecorder.e("Wrong location:"," lat: ${location.latitude} | long: ${location.longitude}")
        }
    }

    fun getSpeed(): Double = speed
}
