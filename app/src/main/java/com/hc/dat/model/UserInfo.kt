package com.hc.dat.model

import java.io.Serializable

/**
 * Created by Duc Bui on 2023/06.
 * Author: Duc Bui
 * Email: ducbui1890@gmail.com
 * Hanoi, VN.
 */
data class UserInfo(
//    var userid: String? = null,
    var username: String? = "",
    var email: String? = "",
    var gender: String? = "",
    var name: String? = "",
    var phoneNumber: String? = "",
    var idNo: String? = "",
    var address: String? = "",
    var userType: String? = "",
    var code: String? = "",
    var trainingCenterId: String? = "",
    var avatarId: String? = "",
    var authenImages: List<String>? = listOf(),
    var birthDay: String? = "",
    var userCode: String? = "",
    var id: String? = "",
    var courseId: String? = "",
    var courseCode: String? = "",
    var driverLicense: String? = "",
    var faceToken: String? = "",
    var idCard: String? = "",
    var totalTime: Double = 0.0,
    var totalDis: Float? = 0f,
    var totalCourseTime: Double = 0.0,
    var totalCourseDis: Float? = 0f,

    var loginType: LoginType = LoginType.OTHER,
    var faceGroupIsReady: Boolean = false
) : Serializable

enum class LoginType(val code: Int) {
    RFID(1),
    FINGER_PRINT(2),
    FACE(3),
    ID_PASSWORD(4),
    OTHER(5);

    companion object {
        fun findByCode(code: Int): LoginType {
            return values().find {
                it.code == code
            } ?: OTHER
        }
    }
}
