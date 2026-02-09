package com.hc.dat.service.model

import com.google.gson.annotations.SerializedName
import com.hc.dat.service.ServiceDefinition

data class GetRequestRetrieverLogResponse(
    @SerializedName(ServiceDefinition.STATUS) val status: Int,
    @SerializedName(ServiceDefinition.MESSAGE) val message: String,
    @SerializedName(ServiceDefinition.STUDENT_CODE) val studentCode: String,
    @SerializedName(ServiceDefinition.DATE_TIME_LOG) val dateTimeLog: String
)
