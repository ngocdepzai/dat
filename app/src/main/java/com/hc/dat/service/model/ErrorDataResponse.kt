package com.hc.dat.service.model

import com.google.gson.annotations.SerializedName
import com.hc.dat.service.ServiceDefinition

data class ErrorDataResponse(
    @SerializedName(ServiceDefinition.MESSAGE) val message: String? = null
)
