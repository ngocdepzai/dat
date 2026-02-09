package com.lws.device.network

import DeviceLogger
import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import com.lws.type.Logger
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel

class NetworkConnectionHandler: NetworkConnection {

//    private var listNetworkEventListener: MutableList<NetworkConnectionEvent> = mutableListOf()
    private var networkConnectionEvent: NetworkConnectionEvent? = null

    private val reqPermissionChannel = Channel<Boolean>()
    private var reqPermissionScope: CoroutineScope? = null
    private lateinit var connectivityManager: ConnectivityManager
    private var networkConnectionAvailable: Boolean = false
    private var internetAvailable: Boolean = false

    // keep old state for prevent send callback duplicate times
    private var networkConnectionOldState: Boolean = false
    private var internetOldState: Boolean = false

    companion object {
        private const val PERMISSION_INTERNET_REQUEST_CODE = 10
        private const val PERMISSION_ACCESS_NETWORK_STATE_REQUEST_CODE = 11
        val LIST_PERMISSION_REQUIRED: List<Pair<String, Int>> = listOf(
            Pair(Manifest.permission.INTERNET, PERMISSION_INTERNET_REQUEST_CODE),
            Pair(Manifest.permission.ACCESS_NETWORK_STATE, PERMISSION_ACCESS_NETWORK_STATE_REQUEST_CODE),
        )
    }

    override var requestPermissionResultCallback: (
        activity: Activity,
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) -> Unit = { activity: Activity, requestCode: Int, permissions: Array<out String>, grantResults: IntArray ->
        DeviceLogger.d("requestPermissionResultCallback requestCode: $requestCode | permissions: $permissions | grantResults: $grantResults")
            if (LIST_PERMISSION_REQUIRED.map { it.second }.contains(requestCode)) {
                reqPermissionScope?.launch {
                    checkPermissionRequired(activity)
                }
            }
        }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        // network is available for use
        @RequiresApi(Build.VERSION_CODES.M)
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            DeviceLogger.i("onAvailable network: $network")
            checkAndNotifyInternetConnection()
            Logger.i("onAvailable networkConnectionAvailable: $networkConnectionAvailable | internetAvailable: $internetAvailable")
            networkConnectionEvent?.onNetworkUpdate(
                action = NetworkConnectionAction.NETWORK_AVAILABLE,
                connectionAvailable = networkConnectionAvailable,
                internetAvailable = internetAvailable
            )
        }

        // Network capabilities have changed for the network
        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            super.onCapabilitiesChanged(network, networkCapabilities)
        }

        // lost network connection
        @RequiresApi(Build.VERSION_CODES.M)
        override fun onLost(network: Network) {
            super.onLost(network)
            DeviceLogger.i("onLost network: $network")
            networkConnectionAvailable = false
            internetAvailable = false
            Logger.i("onLost networkConnectionAvailable: $networkConnectionAvailable | internetAvailable: $internetAvailable")
            networkConnectionEvent?.onNetworkUpdate(
                action = NetworkConnectionAction.NETWORK_LOST,
                connectionAvailable = networkConnectionAvailable,
                internetAvailable = internetAvailable
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun startNetworkConnectionService(activity: Activity) {
        DeviceLogger.d("startNetworkConnectionService")

        reqPermissionScope = CoroutineScope(Dispatchers.Default)
        reqPermissionScope?.launch {
            checkPermissionRequired(activity)
            var limitTimes = 10
            while (!reqPermissionChannel.receive() && limitTimes > 0) {
                DeviceLogger.w("Exist permissions must be required by function's mandatory!")
                limitTimes--
            }
            if (limitTimes <= 0) {
                reqPermissionScope?.cancel()
                return@launch
            }
        }
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .build()

        connectivityManager = activity.getSystemService(ConnectivityManager::class.java) as ConnectivityManager
        connectivityManager.requestNetwork(networkRequest, networkCallback)

        checkAndNotifyInternetConnection()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun checkAndNotifyInternetConnection() {
        connectivityManager.activeNetwork?.also { network ->
            connectivityManager.getNetworkCapabilities(network)?.also { capabilities ->
                if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ) {
//                    Logger.e("DucBT capabilities: $capabilities")
                    networkConnectionAvailable = true
//                    internetAvailable = !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
                    // Todo hard code always true by issue API return wrong
                    internetAvailable = true
                } else {
                    networkConnectionAvailable = false
                }
            } ?: also {
                networkConnectionAvailable = false
                internetAvailable = false
            }
        } ?: also {
            networkConnectionAvailable = false
            internetAvailable = false
        }
    }

    override fun checkConnectionAvailable(): Boolean {
        return (networkConnectionAvailable && internetAvailable)
    }

    override fun setNetworkConnectionEventListener(event: NetworkConnectionEvent) {
        DeviceLogger.d("setNetworkConnectionEventListener event: $event")
        networkConnectionEvent = event
        networkConnectionEvent?.onNetworkUpdate(
            action = NetworkConnectionAction.NETWORK_INIT,
            connectionAvailable = networkConnectionAvailable,
            internetAvailable = internetAvailable
        )
    }

//    override fun removeNetworkConnectionEventListener(event: NetworkConnectionEvent) {
//        DeviceLogger.d("removeGPSEventListener")
//        listNetworkEventListener.remove(event)
//    }

    private suspend fun checkPermissionRequired(activity: Activity) {
        DeviceLogger.d("checkPermissionRequired")
        reqPermissionScope?.run {
            DeviceLogger.i("checkPermissionRequired isActive: $isActive")
            if (isActive) {
                LIST_PERMISSION_REQUIRED.firstOrNull {
                    DeviceLogger.i("checkPermissionRequired request permission: ${it.first} | check: ${ActivityCompat.checkSelfPermission(activity, it.first)}")
                    ActivityCompat.checkSelfPermission(activity, it.first) != PackageManager.PERMISSION_GRANTED
                }?.run {
                    DeviceLogger.i("checkPermissionRequired request permission: ${this.first}")
                    ActivityCompat.requestPermissions(
                        activity, arrayOf(this.first), this.second
                    )
                    reqPermissionChannel.send(false)
                } ?: reqPermissionChannel.send(true)
            }
        }
    }
}