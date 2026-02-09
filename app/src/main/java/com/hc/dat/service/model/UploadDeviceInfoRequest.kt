package com.hc.dat.service.model

import com.google.gson.annotations.SerializedName
import com.hc.dat.service.ServiceDefinition

data class UploadDeviceInfoRequest(
    @SerializedName(ServiceDefinition.SERIAL_NUMBER) val seri: String,
    @SerializedName(ServiceDefinition.IMEI1) val imei1: String,
    @SerializedName(ServiceDefinition.IMEI2) val imei2: String,
    @SerializedName(ServiceDefinition.SIM_SERIAL_NUMBER) val simReal: String,
    @SerializedName(ServiceDefinition.VERSION_APP_DAT) val versionAppDat: String
)