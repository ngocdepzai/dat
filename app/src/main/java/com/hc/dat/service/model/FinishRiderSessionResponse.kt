package com.hc.dat.service.model

import com.google.gson.annotations.SerializedName
import com.hc.dat.service.ServiceDefinition

data class FinishRiderSessionResponse(
    @SerializedName(ServiceDefinition.STATUS) val status: Int,
    @SerializedName(ServiceDefinition.MESSAGE) val message: String
)
