package com.lws.device.error

class PrintExecuteException (errorCode: Int): DeviceException(
    "Printing execute but found an exception!",
    errorCode
)