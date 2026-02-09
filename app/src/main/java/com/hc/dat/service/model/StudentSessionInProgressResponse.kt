package com.hc.dat.service.model

import com.google.gson.annotations.SerializedName
import com.hc.dat.model.InProgressSession
import com.hc.dat.service.ServiceDefinition

data class StudentSessionInProgressResponse(
    @SerializedName(ServiceDefinition.STATUS) val status: Int,
    @SerializedName(ServiceDefinition.MESSAGE) val message: String,
    @SerializedName(ServiceDefinition.CURRENT_DATE) val currentDate: String,
    @SerializedName(ServiceDefinition.SESSION) val inProgressSession: InProgressSession?
)
