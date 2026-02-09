package com.hc.dat.service.model

import com.google.gson.annotations.SerializedName
import com.hc.dat.service.ServiceDefinition

data class DeviceConfigResponse (
    @SerializedName(ServiceDefinition.STATUS) val status: Int,
    @SerializedName(ServiceDefinition.OFFLINE_START_SESSION) val offlineStartSession: Boolean?,
    @SerializedName(ServiceDefinition.OFFLINE_FINISH_SESSION) val offlineFinishSession: Boolean?,
    @SerializedName(ServiceDefinition.IS_APP_VERSION_ACTIVE) val isAppVersionActive: Boolean?,
    @SerializedName(ServiceDefinition.SEARCH_THRESHOLD) val searchThreshold: Float?
)