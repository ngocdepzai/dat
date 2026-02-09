package com.hc.dat.model.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.hc.dat.model.LoginType
import com.hc.dat.model.UserInfo
import com.hc.dat.model.database.entity.UserEntity.Companion.TABLE_NAME
import java.io.Serializable

@Entity(
    tableName = TABLE_NAME
)
data class UserEntity
constructor(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = ID)
    var id: Long = 0,
    @ColumnInfo(name = USER_ID)
    var userId: String,
    @ColumnInfo(name = USER_NAME)
    var userName: String,
    @ColumnInfo(name = GENDER)
    var gender: Int,
    @ColumnInfo(name = FULL_NAME)
    var fullName: String,
    @ColumnInfo(name = PHONE_NUMBER)
    var phoneNumber: String,
    @ColumnInfo(name = USER_CODE)
    var userCode: String,
    @ColumnInfo(name = CITIZEN_ID)
    var citizenId: String,
    @ColumnInfo(name = AVATAR_ID)
    var avatarId: String?,
    @ColumnInfo(name = ADDRESS)
    var address: String,
    @ColumnInfo(name = USER_TYPE)
    var userType: Int,
    @ColumnInfo(name = TRAINING_CENTER_ID)
    var trainingCenterId: String,
    @ColumnInfo(name = BIRTHDAY)
    var birthday: String,
    @ColumnInfo(name = COURSE_ID)
    var courseId: String?,
    @ColumnInfo(name = COURSE_CODE)
    var courseCode: String?,
    @ColumnInfo(name = COURSE_LICENSE)
    var courseLicense: String?,
    @ColumnInfo(name = NFC_ID)
    var nfcId: String?,
    @ColumnInfo(name = TOTAL_TIME_STUDIED)
    var totalTimeStudied: Double?,
    @ColumnInfo(name = TOTAL_DISTANCE_RODE)
    var totalDistanceRode: Float?,
    @ColumnInfo(name = TOTAL_COURSE_TIME)
    var totalCourseTime: Double?,
    @ColumnInfo(name = TOTAL_COURSE_DISTANCE)
    var totalCourseDistance: Float?,

    @ColumnInfo(name = LAST_LOGIN_TYPE)
    var lastLoginType: Int,
    @ColumnInfo(name = FACE_GROUP_READY)
    var faceGroupReady: Boolean = false
) : CommonEntity(), Serializable {
    companion object {
        const val TABLE_NAME = "user"
        const val ID = "id"
        const val USER_ID = "user_id"
        const val USER_NAME = "user_name"
        const val GENDER = "gender"
        const val FULL_NAME = "full_name"
        const val PHONE_NUMBER = "phone_number"
        const val USER_CODE = "user_code"
        const val CITIZEN_ID = "citizen_id"
        const val AVATAR_ID = "avatar_id"
        const val ADDRESS = "address"
        const val USER_TYPE = "user_type"
        const val TRAINING_CENTER_ID = "training_center_id"
        const val BIRTHDAY = "birthday"
        const val COURSE_ID = "course_id"
        const val COURSE_CODE = "course_code"
        const val COURSE_LICENSE = "course_license"
        const val NFC_ID = "nfc_id"
        const val TOTAL_TIME_STUDIED = "total_time_studied"
        const val TOTAL_DISTANCE_RODE = "total_distance_rode"
        const val TOTAL_COURSE_TIME = "total_course_time"
        const val TOTAL_COURSE_DISTANCE = "total_course_distance"

        const val LAST_LOGIN_TYPE = "last_login_type"
        const val FACE_GROUP_READY = "face_group_ready"
    }

    fun updateUserBasicInfo(
        updateUserEntity: UserEntity
    ) {
        this.userId = updateUserEntity.userId
        this.userName = updateUserEntity.userName
        this.gender = updateUserEntity.gender
        this.fullName = updateUserEntity.fullName
        this.phoneNumber = updateUserEntity.phoneNumber
        this.userCode = updateUserEntity.userCode
        this.address = updateUserEntity.address
        this.avatarId = updateUserEntity.avatarId
        this.trainingCenterId = updateUserEntity.trainingCenterId
        this.birthday = updateUserEntity.birthday
        this.courseId = updateUserEntity.courseId
        this.courseCode = updateUserEntity.courseCode
        this.courseLicense = updateUserEntity.courseLicense
        this.nfcId = updateUserEntity.nfcId
        this.totalTimeStudied = updateUserEntity.totalTimeStudied
        this.totalDistanceRode = updateUserEntity.totalDistanceRode
        this.totalCourseTime = updateUserEntity.totalCourseTime
        this.totalCourseDistance = updateUserEntity.totalCourseDistance
    }
}

fun UserEntity.convertToModel(): UserInfo {
    return UserInfo(
        id = userId,
        username = userName,
        gender = Gender.findByCode(gender).valueName,
        name = fullName,
        phoneNumber = phoneNumber,
        userCode = userCode,
        idNo = citizenId,
        avatarId = avatarId,
        address = address,
        userType = UserType.findByCode(userType).valueName,
        trainingCenterId = trainingCenterId,
        birthDay = birthday,
        courseId = courseId,
        courseCode = courseCode,
        driverLicense = courseLicense,
        idCard = nfcId,
        totalTime = totalTimeStudied ?: 0.0,
        totalDis = totalDistanceRode,
        totalCourseTime = totalCourseTime ?: 0.0,
        totalCourseDis = totalCourseDistance,
        loginType = LoginType.findByCode(lastLoginType),
        faceGroupIsReady = faceGroupReady
    )
}

fun UserInfo.convertToModelEntity(): UserEntity {
    return UserEntity(
        userId = id!!,
        userName = username ?: "",
        gender = Gender.findByValueName(gender).code,
        fullName = name ?: "",
        phoneNumber = phoneNumber ?: "",
        userCode = userCode ?: "",
        citizenId = idNo ?: "",
        avatarId = avatarId,
        address = address ?: "",
        userType = UserType.findByValueName(userType).code,
        trainingCenterId = trainingCenterId!!,
        birthday = birthDay ?: "",
        courseId = courseId,
        courseCode = courseCode,
        courseLicense = driverLicense,
        nfcId = idCard,
        totalTimeStudied = totalTime,
        totalDistanceRode = totalDis,
        totalCourseTime = totalCourseTime,
        totalCourseDistance = totalCourseDis,
        lastLoginType = loginType.code,
        faceGroupReady = faceGroupIsReady
    )
}

enum class Gender(val code: Int, val valueName: String) {
    MEN(0, "NAM"),
    WOMEN(1, "NỮ"),
    UNKNOWN(-1, "UNKNOWN");

    companion object {
        fun findByValueName(valueName: String?): Gender {
            return values().find {
                it.valueName == valueName
            } ?: UNKNOWN
        }

        fun findByCode(code: Int): Gender {
            return values().find {
                it.code == code
            } ?: UNKNOWN
        }
    }
}

enum class UserType(val code: Int, val valueName: String) {
    TEACHER(0, "TEACHER"),
    STUDENT(1, "STUDENT"),
    UNKNOWN(-1, "UNKNOWN");

    companion object {
        fun findByValueName(valueName: String?): UserType {
            return values().find {
                it.valueName == valueName
            } ?: UNKNOWN
        }

        fun findByCode(code: Int): UserType {
            return values().find {
                it.code == code
            } ?: UNKNOWN
        }
    }
}
