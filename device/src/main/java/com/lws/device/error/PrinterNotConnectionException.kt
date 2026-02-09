package com.lws.device.error

class PrinterNotConnectionException: DeviceException(
    "Not have connect to printer!",
    ErrorCode.PRINTER_NOT_CONNECTED
)