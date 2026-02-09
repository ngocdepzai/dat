package com.hc.dat.service.model

import com.google.gson.annotations.SerializedName
import com.hc.dat.service.ServiceDefinition

data class GetCurrentSessionInfoResponse(
    @SerializedName(ServiceDefinition.STATUS) val status: Int,
    @SerializedName(ServiceDefinition.MESSAGE) val message: String,
    @SerializedName(ServiceDefinition.TOTAL_TIME) val totalTime: Double,
    @SerializedName(ServiceDefinition.TOTAL_DIS) val totalDis: Float
)
