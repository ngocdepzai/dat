package com.lws.device.error

class PrinterCommunicationException: DeviceException(
    "Can not connect to printer selected!",
    ErrorCode.CONNECTION_ERROR
)