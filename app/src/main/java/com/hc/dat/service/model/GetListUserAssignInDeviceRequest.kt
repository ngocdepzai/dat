package com.hc.dat.service.model

import com.google.gson.annotations.SerializedName
import com.hc.dat.service.ServiceDefinition

data class GetListUserAssignInDeviceRequest(
    @SerializedName(ServiceDefinition.SERIAL_NUMBER) val seri: String,
    @SerializedName(ServiceDefinition.TYPE) val type: Int
)
