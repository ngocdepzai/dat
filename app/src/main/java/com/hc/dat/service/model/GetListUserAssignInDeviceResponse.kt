package com.hc.dat.service.model

import com.google.gson.annotations.SerializedName
import com.hc.dat.model.UserInfo
import com.hc.dat.service.ServiceDefinition

data class GetListUserAssignInDeviceResponse(
    @SerializedName(ServiceDefinition.STATUS) val status: Int,
    @SerializedName(ServiceDefinition.MESSAGE) val message: String,
    @SerializedName(ServiceDefinition.USERS) val users: List<UserInfo>
)
