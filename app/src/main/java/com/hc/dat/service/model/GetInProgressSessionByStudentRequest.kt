package com.hc.dat.service.model

import com.google.gson.annotations.SerializedName
import com.hc.dat.service.ServiceDefinition

data class GetInProgressSessionByStudentRequest(
    @SerializedName(ServiceDefinition.USER_CODE) val userCode: String
)
