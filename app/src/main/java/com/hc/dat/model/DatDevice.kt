package com.hc.dat.model

/**
 * Created by Duc Bui on 2023/06.
 * Author: Duc Bui
 * Email: ducbui1890@gmail.com
 * Hanoi, VN.
 */
data class DatDevice(
    var id: String? = null,
    var seri: String? = null,
    var deviceTypeId: String? = null,
    var apiUrl: String? = null,
    var timeAuth: Int = 0,
    var timeSendGps: Int = 0,
    var distanceError: Int = 0,
    var fakeDisPercent: Int = 0,
    var warningTime: Int = 0,
    var warningDistance: Int = 0,
    var isWarningTime: Boolean? = null,
    var isWarningDistance: Boolean? = null,
    var fakeTimePercent: Int = 0
)
