package com.hc.dat.model

import android.location.Location
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
            // Loại bỏ các điểm có độ chính xác quá kém (nhảy GPS) ---
//            if (location.hasAccuracy() && location.accuracy > 200f) {
//                Logger.e("GPS ignored - low accuracy: ${location.accuracy}")
//                return
//            }

            val calculateDistance: Float = latestLocation?.distanceTo(location) ?: 0.0f
            val durationCalculate: Float = if (latestLocation != null) ((location.time - latestLocation!!.time) / 1000f) else 0f
            val speedCalculate = if (durationCalculate > 0f) calculateDistance / durationCalculate else 0f
            Logger.i("HoangSpeed so sánh speed & speedCalculate: " + "calculateDistance: $calculateDistance |  durationCalculate: $durationCalculate | speedCalculate: $speedCalculate | speed: ${location.speed} | latitude: ${location.latitude} | longitude: ${location.longitude}  | time: ${location.time}")
            LogRecorder.i("Lấy GPS thành công so sánh speed & speedCalculate", "calculateDistance: $calculateDistance |  durationCalculate: $durationCalculate | speedCalculate: $speedCalculate | speed: ${location.speed} | latitude: ${location.latitude} | longitude: ${location.longitude}  | time: ${location.time}")

            val durationTooLong = durationCalculate > 30f

            // Nếu 1 điểm GPS mới cách điểm cũ hơn 1km thì hệ thống sẽ không cập nhật tọa độ đó vào dữ liệu gửi lên server
            // (tránh vẽ đường kẻ xuyên thành phố).
//            if (latestLocation == null || speedCalculate <= 28f && calculateDistance < 3000f) {
            if (latestLocation == null || speedCalculate <= 28f) {
                // Cập nhật khoảng cách và thời gian cho đoạn di chuyển này
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
                // Gán distance = 0 để server không cộng dồn quãng đường sai
                this.distance = 0f
                this.duration = 0L

                // nếu quá lâu mới có GPS tiếp theo thì chấp nhận cập nhật latestLocation làm mốc mới
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
