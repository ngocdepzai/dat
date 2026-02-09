package com.hc.dat.service.model

import com.google.gson.annotations.SerializedName
import com.hc.dat.service.ServiceDefinition

class CheckMissingDataSessionRequest(
    @SerializedName(ServiceDefinition.SESSION_ID) val sessionId: String,
)
