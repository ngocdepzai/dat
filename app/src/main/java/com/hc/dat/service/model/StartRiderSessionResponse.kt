package com.hc.dat.service.model

import com.google.gson.annotations.SerializedName
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
    @SerializedName(ServiceDefinition.TIME_24H_TEACHER) val time24hTeacher: Double? = null
)
