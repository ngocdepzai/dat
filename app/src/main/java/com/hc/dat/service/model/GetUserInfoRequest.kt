package com.hc.dat.service.model

import com.google.gson.annotations.SerializedName
import com.hc.dat.service.ServiceDefinition

data class GetUserInfoRequest(
    @SerializedName(ServiceDefinition.ID_CARD) val idCard: String?,
    @SerializedName(ServiceDefinition.SERIAL_NUMBER) val seri: String,
    @SerializedName(ServiceDefinition.CODE_OR_ID_NO) val codeOrIdNo: String?,
    @SerializedName(ServiceDefinition.SERI_CARD) val seriCard: String?
)
