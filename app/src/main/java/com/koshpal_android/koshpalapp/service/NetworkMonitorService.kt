package com.koshpal_android.koshpalapp.service

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkMonitorService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val syncService: TransactionSyncService
) {
    
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    
    private val _isNetworkAvailable = MutableStateFlow(false)
    val isNetworkAvailable: StateFlow<Boolean> = _isNetworkAvailable.asStateFlow()
    
    private val _networkType = MutableStateFlow(NetworkType.NONE)
    val networkType: StateFlow<NetworkType> = _networkType.asStateFlow()
    
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    
    enum class NetworkType {
        NONE, WIFI, CELLULAR, ETHERNET, OTHER
    }
    
    init {
        startNetworkMonitoring()
    }
    
    private fun startNetworkMonitoring() {
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            .build()
        
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                Log.d("NetworkMonitor", "🌐 Network available: $network")
                _isNetworkAvailable.value = true
                updateNetworkType()
                
                // Trigger sync when network becomes available
                triggerAutoSync()
            }
            
            override fun onLost(network: Network) {
                super.onLost(network)
                Log.d("NetworkMonitor", "❌ Network lost: $network")
                _isNetworkAvailable.value = false
                _networkType.value = NetworkType.NONE
            }
            
            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                super.onCapabilitiesChanged(network, networkCapabilities)
                updateNetworkType()
            }
        }
        
        try {
            connectivityManager.registerNetworkCallback(networkRequest, networkCallback!!)
            Log.d("NetworkMonitor", "✅ Network monitoring started")
        } catch (e: Exception) {
            Log.e("NetworkMonitor", "❌ Failed to start network monitoring: ${e.message}")
        }
    }
    
    private fun updateNetworkType() {
        val activeNetwork = connectivityManager.activeNetwork ?: return
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return
        
        val networkType = when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.CELLULAR
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.ETHERNET
            else -> NetworkType.OTHER
        }
        
        _networkType.value = networkType
        Log.d("NetworkMonitor", "📡 Network type: $networkType")
    }
    
    private fun triggerAutoSync() {
        // Trigger sync when network becomes available
        // This will sync any pending transactions
        try {
            // Note: In a real implementation, you might want to queue pending syncs
            // and process them when network becomes available
            Log.d("NetworkMonitor", "🔄 Network available, triggering auto-sync")
            // syncService.triggerPendingSync() // This would be implemented in sync service
        } catch (e: Exception) {
            Log.e("NetworkMonitor", "❌ Failed to trigger auto-sync: ${e.message}")
        }
    }
    
    /**
     * Check if network is currently available
     */
    fun isNetworkAvailable(): Boolean {
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
    
    /**
     * Get current network type
     */
    fun getCurrentNetworkType(): NetworkType {
        val activeNetwork = connectivityManager.activeNetwork ?: return NetworkType.NONE
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return NetworkType.NONE
        
        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.CELLULAR
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.ETHERNET
            else -> NetworkType.OTHER
        }
    }
    
    /**
     * Stop network monitoring
     */
    fun stopNetworkMonitoring() {
        networkCallback?.let { callback ->
            try {
                connectivityManager.unregisterNetworkCallback(callback)
                Log.d("NetworkMonitor", "✅ Network monitoring stopped")
            } catch (e: Exception) {
                Log.e("NetworkMonitor", "❌ Failed to stop network monitoring: ${e.message}")
            }
        }
        networkCallback = null
    }
    
    /**
     * Get network quality indicator
     */
    fun getNetworkQuality(): NetworkQuality {
        val activeNetwork = connectivityManager.activeNetwork ?: return NetworkQuality.POOR
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return NetworkQuality.POOR
        
        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkQuality.EXCELLENT
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkQuality.EXCELLENT
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                // Check cellular signal strength if available
                when {
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) -> NetworkQuality.GOOD
                    else -> NetworkQuality.FAIR
                }
            }
            else -> NetworkQuality.POOR
        }
    }
    
    enum class NetworkQuality {
        EXCELLENT, GOOD, FAIR, POOR
    }
}
