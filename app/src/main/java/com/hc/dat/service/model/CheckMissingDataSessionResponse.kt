package com.hc.dat.service.model

import com.google.gson.annotations.SerializedName
import com.hc.dat.service.ServiceDefinition

data class CheckMissingDataSessionResponse (
    val listMissDataAuthen: List<DateMissing>,
    val listMissDataGps: List<DateMissing>,
    @SerializedName(ServiceDefinition.STATUS) val status: Int,
    @SerializedName(ServiceDefinition.MESSAGE) val message: String,
)
data class DateMissing(
    val fromTime: String? = null,
    val toTime: String? = null,
)