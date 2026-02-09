package com.hc.dat.model

import hc.manager.datapp.app.DeviceItem

/**
 * Created by Duc Bui on 2023/06.
 * Author: Duc Bui
 * Email: ducbui1890@gmail.com
 * Hanoi, VN.
 */
data class CarInfo(
    var id: String? = "",
    var plate: String? = "",
    var plateSlug: String? = "",
    var deviceId: String? = "",
    var oldPlate: String? = "",
    var createdDate: String? = "",
    var createdBy: String? = "",
    var updatedBy: String? = "",
    var updatedDate: String? = "",
    var sendData: String? = "",
    var cname: String? = "",
    var cphone: String? = "",
    var caddress: String? = "",
    var vehicleOptionId: String? = "",
    var deviceSeri: String? = "",
    var device: List<DeviceItem?>? = ArrayList(),
    var authens: List<DeviceItem?>? = ArrayList(),
    var vehicleOptionName: String? = "",
    var status: String? = "",
    var teacherId: String? = "",
    var teacherName: String? = "",
    var licenseTypeId: String? = "",
    var licenseTypeName: String? = "",
    var trainingCenterId: String? = "",
    var trainingCenterName: String? = "",
    var deviceGpsId: String? = "",
    var deviceCaBinId: String? = "",
    var deviceGpsSeri: String? = "",
    var deviceCaBinSeri: String? = "",
    var setpointType: String? = "",
    var classroomName: String? = "",
    var cabinName: String? = "",
    var maxSpeed: Int = 0,
    var provinceId: String? = "",
    var trainingCertificate: String? = "",
    var registrationCertificate: String? = "",
    var soGtvtid: String? = "",
    var productionYear: String? = "",
    var listTeacherId: List<String?>? = ArrayList()
)
