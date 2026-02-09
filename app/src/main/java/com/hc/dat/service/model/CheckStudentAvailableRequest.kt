package com.hc.dat.service.model

import com.google.gson.annotations.SerializedName
import com.hc.dat.service.ServiceDefinition

data class CheckStudentAvailableRequest(
    @SerializedName(ServiceDefinition.SERIAL_NUMBER) val imei: String,
    @SerializedName(ServiceDefinition.USER_CODE) val studentCode: String
)
