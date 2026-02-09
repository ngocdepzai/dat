package com.hc.dat.model

/**
 * Created by Duc Bui on 2023/06.
 * Author: Duc Bui
 * Email: ducbui1890@gmail.com
 * Hanoi, VN.
 */
data class TrainingCenter(
    var id: String? = null,
    var code: String? = null,
    var name: String? = null,
    var address: String? = null,
    var phoneNumber: String? = null,
    var managerName: String? = null,
    var teacherSendTc: Boolean = false
)
