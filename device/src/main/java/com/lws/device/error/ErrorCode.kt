package com.lws.device.error


object ErrorCode {
    /**
     * Device module using range of number from 400 to 499 for definition error code.
     * Outside range used for other module
     */
    const val NO_ERROR = 400
    const val UNKNOWN = 499
    const val CONNECTION_ERROR = 401
    const val MISSING_CONFIG = 402
    const val NOT_PRINTER_SELECTED = 403

    const val PRINTER_BATTERY_ERROR = 404
    const val PRINTER_NOT_FOUND = 405
    const val PRINTER_BUSY = 406
    const val PRINTER_CANCELED = 407
    const val PRINTER_PAPER_EMPTY = 407
    const val PRINTER_PAPER_JAM = 408
    const val PRINTER_NOT_CONNECTED = 409
    const val HAVE_NOT_PRINTER_CONNECTED = 410

    const val INPUT_MISSED_CUSTOMER = 411
    const val INPUT_MISSED_AMOUNT = 412
    const val START_END_TIME_INVALID = 413
    const val TANK_PERCENTAGE_INVALID = 414
    const val PRESSURE_BEFORE_INVALID = 415
    const val PRESSURE_AFTER_INVALID = 415
    const val TEMPERATURE_INVALID = 416
    const val INPUT_MISSED_TANK_PERCENT_BEFORE = 417
    const val INPUT_MISSED_REASON = 418
    const val INPUT_TANK_PERCENTAGE_INVALID = 419
}

