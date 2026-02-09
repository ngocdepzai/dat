package com.hc.dat.service.model

import com.google.gson.annotations.SerializedName
import com.hc.dat.service.ServiceDefinition

data class UploadImageStartSessionResponse(
    @SerializedName(ServiceDefinition.STATUS) val status: Int,
    @SerializedName(ServiceDefinition.MESSAGE) val message: String,
    @SerializedName(ServiceDefinition.FILE_PATH) val filePath: String
)
