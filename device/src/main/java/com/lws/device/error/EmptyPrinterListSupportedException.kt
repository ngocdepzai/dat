package com.lws.device.error

class EmptyPrinterListSupportedException: DeviceException(
    "You missing set list of printer supported model! " +
            "Please config it by call setPrintersSupported([YOUR PRINTER LIST])!",
    ErrorCode.MISSING_CONFIG
)