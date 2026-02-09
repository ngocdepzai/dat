package com.hc.dat.service.model

import com.google.gson.annotations.SerializedName
import com.hc.dat.model.result.SessionHistory
import com.hc.dat.service.ServiceDefinition

data class SessionHistoryResponse(
    @SerializedName(ServiceDefinition.TOTAL) val total: Int,
    @SerializedName(ServiceDefinition.STATUS) val status: Int,
    @SerializedName(ServiceDefinition.MESSAGE) val message: String,
    @SerializedName(ServiceDefinition.SESSIONS) val listSessionHistory: List<SessionHistory>,
)
