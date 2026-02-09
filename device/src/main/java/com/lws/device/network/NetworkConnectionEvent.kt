package com.lws.device.network

interface NetworkConnectionEvent {
    fun onNetworkUpdate(
        action: NetworkConnectionAction,
        connectionAvailable: Boolean,
        internetAvailable: Boolean
    )
}

enum class NetworkConnectionAction {
    NETWORK_LOST,
    NETWORK_AVAILABLE,
    NETWORK_CAPABILITIES_CHANGED,
    NETWORK_INIT,
}