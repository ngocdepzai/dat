package com.hc.dat.service.model

import com.google.gson.annotations.SerializedName
import com.hc.dat.model.CarInfo
import com.hc.dat.service.ServiceDefinition

data class GetCarsResponse(
    @SerializedName(ServiceDefinition.STATUS) val status: Int,
    @SerializedName(ServiceDefinition.MESSAGE) val message: String,
    @SerializedName(ServiceDefinition.VEHICLES) val cars: List<CarInfo>
)
