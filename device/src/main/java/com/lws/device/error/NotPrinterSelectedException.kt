package com.lws.device.error

class NotPrinterSelectedException: DeviceException(
    "Haven't printer selected" +
            "Please call setPrinterSelection() method for set selected!",
    ErrorCode.NOT_PRINTER_SELECTED
)