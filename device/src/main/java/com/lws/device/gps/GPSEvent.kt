package com.lws.device.gps


interface GPSEvent {
    fun onGPSUpdate(action: GPSAction, data: Any? = null)
}

enum class GPSAction {
    GPS_DISABLE,
    GPS_LOST_SIGNAL,
    GPS_SETTING_CHANGED,
    LOCATION_UPDATED,
    SATELLITE_COUNT_UPDATED
}