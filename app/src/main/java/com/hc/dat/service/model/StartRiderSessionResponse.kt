package com.hc.dat.service.model

import com.google.gson.annotations.SerializedName
import com.hc.dat.model.SessionTimeLimitInfo
import com.hc.dat.service.ServiceDefinition

data class StartRiderSessionResponse(
    @SerializedName(ServiceDefinition.STATUS) val status: Int,
    @SerializedName(ServiceDefinition.MESSAGE) val message: String,
    @SerializedName(ServiceDefinition.CURRENT_DATE) val currentDate: String,
    @SerializedName(ServiceDefinition.SESSION_ID) val sessionId: String,
    @SerializedName(ServiceDefinition.NIGHT_TIME) val nightTime: Double,
    @SerializedName(ServiceDefinition.FROM_NIGHT_TIME) val fromNightTime: Double,
    @SerializedName(ServiceDefinition.TO_NIGHT_TIME) val toNightTime: Double,
    @SerializedName(ServiceDefinition.AUTOMATIC_TIME) val automaticTransmissionTime: Double,
    @SerializedName(ServiceDefinition.TOTAL_AUTOMATIC_TIME) val totalAutomaticTransmissionTime: Double,
    @SerializedName(ServiceDefinition.TIME_IN_24H) val timeIn24H: Double,
    @SerializedName(ServiceDefinition.TIME_24H_TEACHER) val time24hTeacher: Double? = null,
    // Ngưỡng đêm/số tự động tính theo giây, chỉ endpoint này trả về - check-user-in-session
    // không có nên phải lưu lại máy lúc mở phiên. Min là mức tối thiểu của khoá học, hiện
    // chưa dùng, khai báo để giữ đủ contract của API.
    @SerializedName(ServiceDefinition.NIGHT_TIME_MIN) val nightTimeMin: Double? = null,
    @SerializedName(ServiceDefinition.NIGHT_TIME_MAX) val nightTimeMax: Double? = null,
    @SerializedName(ServiceDefinition.AUTO_TIME_MIN) val autoTimeMin: Double? = null,
    @SerializedName(ServiceDefinition.AUTO_TIME_MAX) val autoTimeMax: Double? = null,
    @SerializedName(ServiceDefinition.IS_AUTOMATIC_TRANSMISSION) val isAutomaticTransmission: Boolean? = null
)

/**
 * Rút mốc giờ đêm, giờ xe số tự động và ngưỡng tối đa từ phản hồi mở phiên.
 *
 * Server có thể trả null cho các ngưỡng khi khoá học không giới hạn, quy về 0 để phía dùng
 * chỉ cần kiểm tra `> 0`.
 */
fun StartRiderSessionResponse.toSessionTimeLimitInfo(): SessionTimeLimitInfo =
    SessionTimeLimitInfo(
        nightTime = nightTime,
        nightFromHour = fromNightTime,
        nightToHour = toNightTime,
        nightTimeMax = nightTimeMax ?: 0.0,
        automaticTransmissionTime = automaticTransmissionTime,
        autoTimeMax = autoTimeMax ?: 0.0,
        isAutomaticTransmission = isAutomaticTransmission ?: false
    )
