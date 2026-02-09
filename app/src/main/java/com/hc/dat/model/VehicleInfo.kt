package com.hc.dat.model

/**
 * Created by Duc Bui on 2023/06.
 * Author: Duc Bui
 * Email: ducbui1890@gmail.com
 * Hanoi, VN.
 */
data class VehicleInfo(
    var id: String? = null,
    var plate: String? = null,
    var plateSlug: String? = null,
    var deviceId: String? = null,
    var oldPlace: String? = null,
    var deviceGpsId: String? = null,
    var deviceCaBinId: String? = null,
    var setpointType: String? = null
)
