package com.lws.device.error

import com.lws.type.BaseException


open class DeviceException (
    message: String = "Device communicator has found an exception!!",
    code: Int = ErrorCode.UNKNOWN,
    cause: Throwable? = null
): BaseException(message, code, cause)