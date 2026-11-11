package team.codialo.wifix

import android.content.Context
import android.content.Intent
import android.net.CaptivePortal
import android.net.ConnectivityManager
import android.net.Network
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import team.codialo.wifix.logic.LoginLogic
import team.codialo.wifix.ui.theme.WifixTheme

class CaptivePortalActivity : ComponentActivity() {
    
    private var captivePortal: CaptivePortal? = null
    private var network: Network? = null
    private lateinit var loginLogic: LoginLogic
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        captivePortal = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(ConnectivityManager.EXTRA_CAPTIVE_PORTAL, CaptivePortal::class.java)
        } else {
            intent.getParcelableExtra(ConnectivityManager.EXTRA_CAPTIVE_PORTAL)
        }
        
        network = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK, Network::class.java)
        } else {
            intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK)
        }
        
        loginLogic = LoginLogic(this, captivePortal)
        
        Log.d("CaptivePortalActivity", "Network received: $network")
        
        setContent {
            WifixTheme {
                CaptivePortalScreen(
                    onLoginSuccess = { handleLoginSuccess() },
                    onLoginFailure = { handleLoginFailure() },
                    onIgnoreNetwork = { handleIgnoreNetwork() },
                    loginLogic = loginLogic
                )
            }
        }
    }
    
    private fun handleLoginSuccess() {
        captivePortal?.reportCaptivePortalDismissed()
        Log.d("CaptivePortalActivity", "Captive portal dismissed successfully")
        Toast.makeText(this, "Connected to WiFi successfully", Toast.LENGTH_SHORT).show()
        finish()
    }
    
    private fun handleLoginFailure() {
        Log.w("CaptivePortalActivity", "Login failed, keeping captive portal active")
        Toast.makeText(this, "Login failed, please try again", Toast.LENGTH_SHORT).show()
    }
    
    private fun handleIgnoreNetwork() {
        captivePortal?.ignoreNetwork()
        Log.d("CaptivePortalActivity", "Network ignored")
        Toast.makeText(this, "Network ignored", Toast.LENGTH_SHORT).show()
        finish()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptivePortalScreen(
    onLoginSuccess: () -> Unit,
    onLoginFailure: () -> Unit,
    onIgnoreNetwork: () -> Unit,
    loginLogic: LoginLogic
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isConnecting by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val networkState by loginLogic.networkState.collectAsState()
    
    LaunchedEffect(Unit) {
        loginLogic.startNetworkMonitoring()
        
        val prefs = context.getSharedPreferences("wifix_prefs", Context.MODE_PRIVATE)
        username = prefs.getString("username", "") ?: ""
        password = prefs.getString("password", "") ?: ""
    }
    
    
    DisposableEffect(Unit) {
        onDispose {
            loginLogic.stopNetworkMonitoring()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "WiFix Captive Portal",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                when {
                    networkState.hasCaptivePortal -> {
                        Text(
                            text = "Login Required",
                            fontSize = 16.sp,
                            color = Color.Yellow
                        )
                    }
                    networkState.isConnected -> {
                        Text(
                            text = "Connected",
                            fontSize = 16.sp,
                            color = Color.Green
                        )
                    }
                    else -> {
                        Text(
                            text = "Checking Network...",
                            fontSize = 16.sp,
                            color = Color.Gray
                        )
                    }
                }
                
                Text(
                    text = "Network: ${networkState.networkType}",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                
                
                Text(
                    text = if (networkState.gatewayIp.isNotEmpty()) "Gateway: ${networkState.gatewayIp}" else "Gateway: Not found",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.Blue,
                        unfocusedBorderColor = Color.Gray
                    )
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Filled.Check else Icons.Filled.Close,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                tint = Color.Gray
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.Blue,
                        unfocusedBorderColor = Color.Gray
                    )
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = {
                        coroutineScope.launch {
                            isConnecting = true
                            try {
                                val success = loginLogic.login(username, password)
                                if (success) {
                                    saveCredentials(context, username, password)
                                    onLoginSuccess()
                                } else {
                                    onLoginFailure()
                                }
                            } catch (e: Exception) {
                                onLoginFailure()
                            } finally {
                                isConnecting = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2196F3)
                    ),
                    enabled = !isConnecting && username.isNotEmpty() && password.isNotEmpty()
                ) {
                    if (isConnecting) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("CONNECT", color = Color.White)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedButton(
                    onClick = onIgnoreNetwork,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.Gray
                    )
                ) {
                    Text("IGNORE NETWORK")
                }
            }
        }
    }
}

private fun saveCredentials(context: Context, username: String, password: String) {
    val sharedPreferences = context.getSharedPreferences("wifix_prefs", Context.MODE_PRIVATE)
    sharedPreferences.edit().apply {
        putString("username", username)
        putString("password", password)
        apply()
    }
}