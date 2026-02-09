package com.hc.dat.model.result

object ErrorCode {
    /**
     * App module using range of number from 0 to 99 for definition error code.
     * Outside range used for other module
     */
    const val NO_ERROR = 0
    const val SUCCESS_WITH_ERROR = 0
    const val UNKNOWN = 99

    const val REGISTER_FAILED = 1
    const val TEMPORARY_FAILED = 2

    const val CAN_NOT_CONNECT_TO_SERVER = 3
    const val SERVER_DECLINED_REQUEST = 4
    const val CAN_NOT_DELETE_REGISTER = 5
    const val SERVER_DECLINE_AUTHENTICATE = 6
    const val DELIVERY_NOT_FOUND = 7

    const val PUSH_SESSION_TO_TC_FAIL = 101
}
