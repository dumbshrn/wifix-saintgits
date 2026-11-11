package team.codialo.wifix.logic

import android.content.Context
import android.content.Intent
import android.net.CaptivePortal
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.provider.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LoginLogic(private val context: Context, private val captivePortal: CaptivePortal? = null) {
    
    data class NetworkState(
        val isConnected: Boolean = false,
        val hasCaptivePortal: Boolean = false,
        val hasInternetAccess: Boolean = false,
        val networkType: String = "Unknown",
        val captivePortalNetwork: Network? = null,
        val gatewayIp: String = "",
        val detectionMethod: String = "Unknown",
        val isCheckingConnectivity: Boolean = false
    )
    
    data class LogEntry(
        val timestamp: String,
        val level: String,
        val message: String,
        val details: String = ""
    )
    
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val _networkState = MutableStateFlow(NetworkState())
    val networkState: StateFlow<NetworkState> = _networkState.asStateFlow()
    private val logs = mutableListOf<LogEntry>()
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO)
    
    private fun addLog(level: String, message: String, details: String = "") {
        val timestamp = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date())
        logs.add(LogEntry(timestamp, level, message, details))
        if (logs.size > 20) logs.removeAt(0)
    }
    
    fun getLogs(): List<LogEntry> = logs.toList()
    
    fun startNetworkMonitoring() {
        val networkRequest = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
            
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                serviceScope.launch { updateNetworkState(network) }
            }
            
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                serviceScope.launch { updateNetworkState(network, capabilities) }
            }
            
            override fun onLost(network: Network) {
                _networkState.value = NetworkState()
            }
        }
        
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback!!)
        connectivityManager.activeNetwork?.let { 
            serviceScope.launch { updateNetworkState(it) }
        }
    }
    
    fun stopNetworkMonitoring() {
        networkCallback?.let { 
            connectivityManager.unregisterNetworkCallback(it)
            networkCallback = null
        }
    }
    
    private suspend fun updateNetworkState(network: Network, capabilities: NetworkCapabilities? = null) {
        val networkCapabilities = capabilities ?: connectivityManager.getNetworkCapabilities(network) ?: return
        
        val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        val hasCaptivePortal = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL)
        val isWifi = networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        
        val networkType = when {
            isWifi -> "WiFi"
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular"
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            else -> "Unknown"
        }
        
        val gatewayResult = if (hasInternet) discoverGateway(network) else GatewayResult("", "No Internet")
        val actualHasCaptivePortal = hasCaptivePortal || (hasInternet && gatewayResult.ip.isNotEmpty())
        val hasInternetAccess = if (hasInternet) checkInternetConnectivity(network) else false
        
        _networkState.value = NetworkState(
            isConnected = hasInternet,
            hasCaptivePortal = actualHasCaptivePortal,
            hasInternetAccess = hasInternetAccess,
            networkType = networkType,
            captivePortalNetwork = if (actualHasCaptivePortal) network else null,
            gatewayIp = gatewayResult.ip,
            detectionMethod = gatewayResult.method,
            isCheckingConnectivity = false
        )
        
        addLog("INFO", "Network updated", "Portal: $actualHasCaptivePortal, Gateway: ${gatewayResult.ip}, Internet: $hasInternetAccess")
    }
    
    private data class GatewayResult(val ip: String, val method: String)
    
    private suspend fun discoverGateway(network: Network): GatewayResult {
        return withContext(Dispatchers.IO) {
            try {
                val linkProperties = connectivityManager.getLinkProperties(network)
                if (linkProperties != null) {
                    val gatewayIp = extractGatewayFromLinkProperties(linkProperties)
                    if (gatewayIp.isNotEmpty()) {
                        addLog("SUCCESS", "Gateway discovered via LinkProperties", "IP: $gatewayIp")
                        return@withContext GatewayResult(gatewayIp, "LinkProperties Discovery")
                    }
                }
                
                addLog("WARNING", "LinkProperties discovery failed", "Falling back to known gateway")
                val fallbackIp = "10.10.10.100"
                return@withContext GatewayResult(fallbackIp, "Fallback")
                
            } catch (e: Exception) {
                addLog("ERROR", "Gateway discovery failed", "Error: ${e.message}")
                val fallbackIp = "10.10.10.100"
                GatewayResult(fallbackIp, "Error Fallback")
            }
        }
    }
    
    private fun extractGatewayFromLinkProperties(linkProperties: LinkProperties): String {
        return try {
            linkProperties.routes.forEach { route ->
                if (route.isDefaultRoute && route.gateway != null) {
                    val gatewayIp = route.gateway?.hostAddress
                    if (gatewayIp != null && gatewayIp.isNotEmpty()) {
                        addLog("DEBUG", "Found default route gateway", "IP: $gatewayIp")
                        return gatewayIp
                    }
                }
            }
            ""
        } catch (e: Exception) {
            addLog("ERROR", "Failed to extract gateway from LinkProperties", "Error: ${e.message}")
            ""
        }
    }
    
    private suspend fun checkInternetConnectivity(network: Network): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                addLog("INFO", "Checking internet connectivity", "Using Google connectivity check")
                val testUrl = "http://connectivitycheck.gstatic.com/generate_204"
                val url = URL(testUrl)
                val connection = network.openConnection(url) as HttpURLConnection
                
                connection.apply {
                    requestMethod = "GET"
                    connectTimeout = 5000
                    readTimeout = 5000
                    instanceFollowRedirects = false
                }
                
                val responseCode = connection.responseCode
                connection.disconnect()
                
                when (responseCode) {
                    204 -> {
                        addLog("SUCCESS", "Internet connectivity validated", "No captive portal detected")
                        true
                    }
                    in 300..399 -> {
                        val location = connection.getHeaderField("Location")
                        addLog("WARNING", "Captive portal detected via redirect", "Redirects to: $location")
                        false
                    }
                    200 -> {
                        addLog("WARNING", "Captive portal detected", "Received HTML content instead of 204")
                        false
                    }
                    else -> {
                        addLog("ERROR", "Unexpected response", "Code: $responseCode")
                        false
                    }
                }
            } catch (e: Exception) {
                addLog("ERROR", "Internet connectivity check failed", "Error: ${e.message}")
                false
            }
        }
    }
    
    suspend fun retryConnectivityCheck() {
        _networkState.value = _networkState.value.copy(isCheckingConnectivity = true)
        addLog("INFO", "Manual connectivity check initiated", "")
        
        connectivityManager.activeNetwork?.let { network ->
            updateNetworkState(network)
        } ?: run {
            _networkState.value = NetworkState()
            addLog("WARNING", "No active network found", "")
        }
    }
    
    fun getGatewayInfo(): String {
        val state = _networkState.value
        return if (state.gatewayIp.isNotEmpty()) "Gateway: ${state.gatewayIp}" else "Gateway: Not found"
    }
    
    fun refreshNetworkConnection() {
        addLog("INFO", "Manual network refresh requested", "")
        try {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)
            } else {
                Intent(android.provider.Settings.ACTION_WIFI_SETTINGS)
            }.apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
            
            context.startActivity(intent)
            addLog("INFO", "Network settings opened", "")
        } catch (e: Exception) {
            addLog("ERROR", "Failed to open settings", "Error: ${e.message}")
        }
    }
    
    suspend fun login(username: String, password: String): Boolean {
        return withContext(Dispatchers.IO) {
            // Saintgits Cyberoam portal: always at 10.10.10.100:8090
            val portalIp = "10.10.10.100"
            val portalPort = "8090"

            try {
                addLog("INFO", "Starting Cyberoam login", "Portal: $portalIp:$portalPort")

                val loginUrl = "http://$portalIp:$portalPort/login.xml"
                val network = _networkState.value.captivePortalNetwork

                val formData = "mode=191" +
                        "&username=${java.net.URLEncoder.encode(username, "UTF-8")}" +
                        "&password=${java.net.URLEncoder.encode(password, "UTF-8")}" +
                        "&a=${System.currentTimeMillis()}" +
                        "&producttype=0"

                val loginResponse = makeRequest(network, loginUrl, "POST", formData)

                val success = loginResponse.statusCode in 200..299
                if (success) {
                    addLog("SUCCESS", "Login successful", "Code: ${loginResponse.statusCode}")
                    captivePortal?.let {
                        try {
                            it.reportCaptivePortalDismissed()
                            addLog("SUCCESS", "Captive portal dismissed", "Portal API called successfully")
                        } catch (e: Exception) {
                            addLog("WARNING", "Failed to dismiss captive portal", "Error: ${e.message}")
                        }
                    }
                } else {
                    addLog("ERROR", "Login failed", "Code: ${loginResponse.statusCode}")
                }

                success
            } catch (e: Exception) {
                addLog("ERROR", "Login exception", "Error: ${e.message}")
                false
            }
        }
    }
    
    suspend fun logout(username: String = ""): Boolean {
        return withContext(Dispatchers.IO) {
            val portalIp = "10.10.10.100"
            val portalPort = "8090"

            try {
                addLog("INFO", "Starting Cyberoam logout", "Portal: $portalIp:$portalPort")

                val logoutUrl = "http://$portalIp:$portalPort/logout.xml"
                val network = _networkState.value.captivePortalNetwork

                val formData = "mode=193" +
                        "&username=${java.net.URLEncoder.encode(username, "UTF-8")}" +
                        "&a=${System.currentTimeMillis()}" +
                        "&producttype=0"

                val response = makeRequest(network, logoutUrl, "POST", formData)
                val success = response.statusCode in 200..299

                if (success) {
                    addLog("SUCCESS", "Logout successful", "Code: ${response.statusCode}")
                } else {
                    addLog("ERROR", "Logout failed", "Code: ${response.statusCode}")
                }

                success
            } catch (e: Exception) {
                addLog("ERROR", "Logout exception", "Error: ${e.message}")
                false
            }
        }
    }
    
    private data class NetworkResponse(val statusCode: Int, val body: String, val success: Boolean)
    
    private fun makeRequest(network: Network?, url: String, method: String, postData: String? = null): NetworkResponse {
        return try {
            val connection = if (network != null) {
                network.openConnection(URL(url)) as HttpURLConnection
            } else {
                URL(url).openConnection() as HttpURLConnection
            }
            
            connection.apply {
                connectTimeout = 10_000
                readTimeout = 10_000
                requestMethod = method
                if (method == "POST" && postData != null) {
                    doOutput = true
                    setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                    OutputStreamWriter(outputStream).use { it.write(postData) }
                }
            }
            
            val responseCode = connection.responseCode
            val responseBody = BufferedReader(
                InputStreamReader(if (responseCode >= 400) connection.errorStream else connection.inputStream)
            ).use { it.readText() }
            
            connection.disconnect()
            NetworkResponse(responseCode, responseBody, true)
        } catch (e: Exception) {
            addLog("ERROR", "Network request failed", "Error: ${e.message}")
            NetworkResponse(0, "", false)
        }
    }
    
    
    fun clearLogs() {
        logs.clear()
        addLog("INFO", "Logs cleared", "")
    }
}