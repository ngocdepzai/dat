package com.hc.dat.service.model

import com.google.gson.annotations.SerializedName
import com.hc.dat.service.ServiceDefinition

data class StartRiderSessionRequest(
    @SerializedName(ServiceDefinition.SERIAL_NUMBER) val seri: String,
    @SerializedName(ServiceDefinition.CODE) val studentCode: String,
    @SerializedName(ServiceDefinition.GPS_LAT) val gpsLat: Double,
    @SerializedName(ServiceDefinition.GPS_LONG) val gpsLong: Double,
    @SerializedName(ServiceDefinition.LOGIN_TYPE) val loginType: Int,
    @SerializedName(ServiceDefinition.TIME) val loginTime: Double,
    @SerializedName(ServiceDefinition.PATH) val loginImageUrl: String,
    @SerializedName(ServiceDefinition.TEACHER_CODE) val teacherCode: String,
    @SerializedName(ServiceDefinition.VERSION) val version: String,
    @SerializedName(ServiceDefinition.IMEI1) val imei: String,
    @SerializedName(ServiceDefinition.SIM_SERIAL_NUMBER) val simSerialNumber: String,
    @SerializedName(ServiceDefinition.INTERNET) val networkStatus: Byte?,
    )
