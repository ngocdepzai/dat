package com.hc.dat.model.result

object ErrorCode {
    /**
     * App module using range of number from 0 to 99 for definition error code.
     * Outside range used for other module
     */
    const val NO_ERROR = 0
    const val UNKNOWN = 99

    const val REGISTER_FAILED = 1
    const val TEMPORARY_FAILED = 2

    const val CAN_NOT_CONNECT_TO_SERVER = 3
    const val SERVER_DECLINED_REQUEST = 4
    const val CAN_NOT_DELETE_REGISTER = 5
    const val SERVER_DECLINE_AUTHENTICATE = 6
    const val DELIVERY_NOT_FOUND = 7

    const val PUSH_SESSION_TO_TC_FAIL = 101
    const val FINISH_SESSION_FAIL = 102

    /**
     * Đã gửi request kết thúc phiên nhưng không đọc được kết quả (timeout, mất kết nối,
     * response không đúng định dạng). Không thể biết server đã kết thúc phiên hay chưa
     * nên bắt buộc phải lưu lại và gửi lại với coverReSend = true.
     */
    const val FINISH_SESSION_RESULT_UNKNOWN = 103
}
