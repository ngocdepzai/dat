package com.hc.dat.service.model

import com.google.gson.annotations.SerializedName
import com.hc.dat.service.ServiceDefinition
import hc.manager.datapp.utils.Constant

data class GetObjectsLinkedDatRequest(
    @SerializedName(ServiceDefinition.SERIAL_NUMBER) val imei: String,
    @SerializedName(ServiceDefinition.APP_VERSION) val appVersion: String = Constant.VersionDat
)
