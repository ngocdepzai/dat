package com.hc.dat.service.model

import com.google.gson.annotations.SerializedName
import com.hc.dat.model.DatDevice
import com.hc.dat.model.TrainingCenter
import com.hc.dat.model.VehicleInfo
import com.hc.dat.service.ServiceDefinition

data class GetObjectsLinkedDatResponse(
    @SerializedName(ServiceDefinition.STATUS) val status: Int,
    @SerializedName(ServiceDefinition.MESSAGE) val message: String,
    @SerializedName(ServiceDefinition.CURRENT_DATE) val currentDate: String,
    @SerializedName(ServiceDefinition.DAT_DEVICE) val datDevice: DatDevice?,
    @SerializedName(ServiceDefinition.TRAINING_CENTER) val trainingCenter: TrainingCenter?,
    @SerializedName(ServiceDefinition.VEHICLE) val carVehicle: VehicleInfo?
)
