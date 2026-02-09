package com.hc.dat.model.result

import com.google.gson.annotations.SerializedName


data class SessionHistory(
    var id: String = "",
    var studentCode: String = "",
    var loginDateParse: String = "",
    var logoutDateParse: String = "",
    var totalTime: Double = 0.0,
    var totalDis: Float = 0f,
    var sentGeneral: Boolean = false,
    var studentName: String = "",
    @SerializedName("hopLe")
    var isSessionValid: Boolean = false,
    @SerializedName("detailHopLe")
    var invalidReasons: List<String> = listOf()

)
