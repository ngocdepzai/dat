package com.hc.dat.service.model

import com.google.gson.annotations.SerializedName
import com.hc.dat.service.ServiceDefinition

data class FinishRiderSessionRequest(
    @SerializedName(ServiceDefinition.SERIAL_NUMBER) val imei: String,
    @SerializedName(ServiceDefinition.CODE) val studentCode: String,
    @SerializedName(ServiceDefinition.GPS_LAT) val gpsLat: Double,
    @SerializedName(ServiceDefinition.GPS_LONG) val gpsLong: Double,
    @SerializedName(ServiceDefinition.SESSION_ID) val sessionId: String,
    @SerializedName(ServiceDefinition.TIME) val logoutTime: Double,
    @SerializedName(ServiceDefinition.PATH) val loginImageUrl: String,
    @SerializedName(ServiceDefinition.IS_SEND_TC) val sendTC: Boolean,
    // coverReSend = true when resend session save in local device
    @SerializedName(ServiceDefinition.COVER_RE_SEND) val coverReSend: Boolean = false
)
