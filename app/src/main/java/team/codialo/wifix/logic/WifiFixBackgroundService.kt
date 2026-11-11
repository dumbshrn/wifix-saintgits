package team.codialo.wifix.logic

import android.app.Service
import android.content.Intent
import android.content.SharedPreferences
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class WifiFixBackgroundService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var loginLogic: LoginLogic
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate() {
        super.onCreate()
        sharedPreferences = getSharedPreferences("wifix_prefs", MODE_PRIVATE)
        loginLogic = LoginLogic(this)
        loginLogic.startNetworkMonitoring()

        // React to captive portal detection — no polling, event-driven only
        loginLogic.networkState
            .onEach { state ->
                if (state.hasCaptivePortal && !state.hasInternetAccess) {
                    val enabled = sharedPreferences.getBoolean("wifixing_enabled", false)
                    if (!enabled) return@onEach
                    val username = sharedPreferences.getString("username", "") ?: ""
                    val password = sharedPreferences.getString("password", "") ?: ""
                    if (username.isNotEmpty() && password.isNotEmpty()) {
                        Log.d("WifiFixService", "Captive portal detected, logging in")
                        val success = loginLogic.login(username, password)
                        Log.d("WifiFixService", if (success) "Login OK" else "Login failed")
                    }
                }
            }
            .launchIn(serviceScope)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val enabled = sharedPreferences.getBoolean("wifixing_enabled", false)
        if (!enabled) stopSelf()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        loginLogic.stopNetworkMonitoring()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
