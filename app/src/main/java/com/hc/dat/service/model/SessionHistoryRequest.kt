package com.hc.dat.service.model

import com.google.gson.annotations.SerializedName
import com.hc.dat.service.ServiceDefinition

data class SessionHistoryRequest(
    @SerializedName(ServiceDefinition.SERIAL_NUMBER) val seri: String,
    @SerializedName(ServiceDefinition.START_TIME) val startTime: String?,
    @SerializedName(ServiceDefinition.END_TIME) val endTime: String?,
    @SerializedName(ServiceDefinition.LIMIT) val limit: Int,
    @SerializedName(ServiceDefinition.PAGE) val page: Int,
    @SerializedName(ServiceDefinition.SEND_GENERAL) val sendGeneral: Boolean?,

    )
