package com.hc.dat.model

import com.hc.dat.model.database.entity.RiderSessionEntity
import com.hc.dat.utils.Utils
import java.util.Date

/**
 * Created by Duc Bui on 2023/06.
 * Author: Duc Bui
 * Email: ducbui1890@gmail.com
 * Hanoi, VN.
 */
data class InProgressSession(
    var id: String = "",
    var studentCode: String? = "",
    var plateSlug: String? = "",
    var seri: String? = "",
    var teacherCode: String? = "",
    var teacherName: String? = "",
    var totalTime: Double = 0.0,
    var totalDis: Float = 0f,
    var totalVerifyCounter: Int = 0,
    var successVerifyCounter: Int = 0,
    var startTime: Long = 0,
    var loginDate: String = "",
    var lastUploadGPSTime: Long = 0,
    var totalAuthDataUploadSuccess: Int = 0,
    var totalAuthDataUpload: Int = 0,
    var totalGPSUploadSuccess: Int = 0,
    var totalGPSUpload: Int = 0,
    var totalGps: Int = 0,
    var totalAuthen: Int = 0,
//    val nightTime: Double,
//    val fromNightTime: Double,
//    val toNightTime: Double,
//    val automaticTransmissionTime: Double,
//    val totalAutomaticTransmissionTime: Double,
//    val timeIn24H: Double,
    val nightTime: Double? = null,
    val fromNightTime: Double? = null,
    val toNightTime: Double? = null,
    val automaticTransmissionTime: Double? = null,
    val totalAutomaticTransmissionTime: Double? = null,
    // var chứ không phải val: phiên mở offline chưa có hai giá trị này, chúng được đổ vào
    // giữa phiên khi có mạng trở lại và phiên được đẩy lên server thành công.
    var timeIn24H: Double? = null,
    var time24hTeacher: Double? = null,
    val lastAuthenTime: Date? = null,
    // use for CER function
    var localRiderSession: RiderSessionEntity? = null

)
