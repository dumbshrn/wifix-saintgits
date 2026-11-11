package team.codialo.wifix.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import team.codialo.wifix.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import team.codialo.wifix.logic.LoginLogic
import team.codialo.wifix.logic.WifiFixBackgroundService
import team.codialo.wifix.ui.theme.WifixTheme
import androidx.core.content.edit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WifixHomeScreen() {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isWifixing by remember { mutableStateOf(false) }
    var isConnecting by remember { mutableStateOf(false) }
    var showLogDialog by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val loginLogic = remember { LoginLogic(context) }
    val networkState by loginLogic.networkState.collectAsState()
    
    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("wifix_prefs", Context.MODE_PRIVATE)
        username = prefs.getString("username", "") ?: ""
        password = prefs.getString("password", "") ?: ""
        val savedWifixing = prefs.getBoolean("wifixing_enabled", false)
        isWifixing = savedWifixing
        if (savedWifixing) startWifiFixService(context)
        
        loginLogic.startNetworkMonitoring()
    }
    
    
    DisposableEffect(Unit) {
        onDispose {
            loginLogic.stopNetworkMonitoring()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            StatusBar(
                isWifixing = isWifixing,
                networkState = networkState,
                loginLogic = loginLogic,
                onWifixingToggle = { 
                    isWifixing = it
                    saveWifixingState(context, it, username, password)
                    if (it) {
                        startWifiFixService(context)
                        Toast.makeText(context, "WiFixing enabled", Toast.LENGTH_SHORT).show()
                    } else {
                        stopWifiFixService(context)
                        Toast.makeText(context, "WiFixing disabled", Toast.LENGTH_SHORT).show()
                    }
                },
                coroutineScope = coroutineScope
            )

            Spacer(modifier = Modifier.height(16.dp))

            WifixLogo(
                onLogoClick = { showLogDialog = true }
            )

            UsernameField(
                username = username,
                onUsernameChange = { username = it }
            )

            PasswordField(
                password = password,
                onPasswordChange = { password = it },
                passwordVisible = passwordVisible,
                onPasswordVisibilityToggle = { passwordVisible = !passwordVisible }
            )

            ConnectButton(
                isConnecting = isConnecting,
                onConnectClick = {
                    coroutineScope.launch {
                        isConnecting = true
                        try {
                            val success = loginLogic.login(username, password)
                            if (success) {
                                saveCredentials(context, username, password)
                                Toast.makeText(context, "Login successful", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Login failed", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Connection error", Toast.LENGTH_SHORT).show()
                        } finally {
                            isConnecting = false
                        }
                    }
                }
            )

            LogoutButton(
                onLogoutClick = {
                    coroutineScope.launch {
                        val success = loginLogic.logout(username)
                        if (success) {
                            Toast.makeText(context, "Logout successful", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Logout failed", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )

            LockAppInfo()

            Spacer(modifier = Modifier.height(8.dp))

            Footer()
        }

        if (showLogDialog) {
            LogViewerModal(
                loginLogic = loginLogic,
                onDismiss = { showLogDialog = false }
            )
        }
    }
}

@Composable
private fun StatusBar(
    isWifixing: Boolean,
    networkState: LoginLogic.NetworkState,
    loginLogic: LoginLogic,
    onWifixingToggle: (Boolean) -> Unit,
    coroutineScope: kotlinx.coroutines.CoroutineScope
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (isWifixing) "WiFixing" else "Not WiFixing",
                    color = Color.White,
                    fontSize = 18.sp
                )
                Text(
                    text = networkState.networkType,
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
            Text(
                text = SimpleDateFormat("MM/dd, HH:mm:ss", Locale.getDefault()).format(Date()),
                color = Color.Gray,
                fontSize = 14.sp
            )
            Switch(
                checked = isWifixing,
                onCheckedChange = onWifixingToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color.Blue,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = Color.Gray
                )
            )
        }
        
        if (networkState.hasCaptivePortal || !networkState.hasInternetAccess) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (networkState.isCheckingConnectivity) {
                        CircularProgressIndicator(
                            color = Color.Yellow,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .background(
                                    color = if (networkState.hasInternetAccess) Color.Green else Color.Red,
                                    shape = RoundedCornerShape(2.dp)
                                )
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when {
                            networkState.isCheckingConnectivity -> "Checking Connection..."
                            networkState.hasInternetAccess -> "Internet Available"
                            networkState.hasCaptivePortal -> "Login Required"
                            else -> "No Internet Access"
                        },
                        color = when {
                            networkState.isCheckingConnectivity -> Color.Yellow
                            networkState.hasInternetAccess -> Color.Green
                            else -> Color.Red
                        },
                        fontSize = 14.sp
                    )
                }
                
                Row(
                    modifier = Modifier.padding(horizontal = 32.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (networkState.gatewayIp.isNotEmpty()) "Gateway: ${networkState.gatewayIp}" else "Gateway: Not found",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                    if (!networkState.isCheckingConnectivity) {
                        Button(
                            onClick = { 
                                coroutineScope.launch {
                                    loginLogic.retryConnectivityCheck()
                                }
                            },
                            modifier = Modifier
                                .height(28.dp)
                                .padding(start = 8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Gray.copy(alpha = 0.3f)
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text(
                                text = "Retry",
                                color = Color.White,
                                fontSize = 12.sp
                            )
                        }
                    }
                    IconButton(
                        onClick = { loginLogic.refreshNetworkConnection() },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Refresh Network",
                            tint = Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        } else if (networkState.isConnected && networkState.hasInternetAccess) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (networkState.isCheckingConnectivity) {
                        CircularProgressIndicator(
                            color = Color.Green,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .background(
                                    color = Color.Green,
                                    shape = RoundedCornerShape(2.dp)
                                )
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (networkState.isCheckingConnectivity) "Checking Connection..." else "Internet Validated",
                        color = Color.Green,
                        fontSize = 14.sp
                    )
                }
                
                Row(
                    modifier = Modifier.padding(horizontal = 32.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (networkState.gatewayIp.isNotEmpty()) "Gateway: ${networkState.gatewayIp}" else "Gateway: Not found",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                    if (!networkState.isCheckingConnectivity) {
                        Button(
                            onClick = { 
                                coroutineScope.launch {
                                    loginLogic.retryConnectivityCheck()
                                }
                            },
                            modifier = Modifier
                                .height(28.dp)
                                .padding(start = 8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Gray.copy(alpha = 0.3f)
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text(
                                text = "Retry",
                                color = Color.White,
                                fontSize = 12.sp
                            )
                        }
                    }
                    IconButton(
                        onClick = { loginLogic.refreshNetworkConnection() },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Refresh Network",
                            tint = Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WifixLogo(
    onLogoClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(id = R.drawable.mainicon),
            contentDescription = "WiFix Logo",
            modifier = Modifier
                .size(140.dp)
                .clickable { onLogoClick() }
        )
    }
}

@Composable
private fun UsernameField(
    username: String,
    onUsernameChange: (String) -> Unit
) {
    Text(
        text = "Username:",
        color = Color.White,
        fontSize = 12.sp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        textAlign = TextAlign.Start
    )

    OutlinedTextField(
        value = username,
        onValueChange = onUsernameChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedBorderColor = Color.Blue,
            unfocusedBorderColor = Color.Gray,
            cursorColor = Color.White
        ),
        shape = RoundedCornerShape(8.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PasswordField(
    password: String,
    onPasswordChange: (String) -> Unit,
    passwordVisible: Boolean,
    onPasswordVisibilityToggle: () -> Unit
) {
    Text(
        text = "Password:",
        color = Color.White,
        fontSize = 12.sp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        textAlign = TextAlign.Start
    )

    OutlinedTextField(
        value = password,
        onValueChange = onPasswordChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 20.dp),
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            IconButton(onClick = onPasswordVisibilityToggle) {
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
            unfocusedBorderColor = Color.Gray,
            cursorColor = Color.White
        ),
        shape = RoundedCornerShape(8.dp)
    )
}

@Composable
private fun ConnectButton(
    isConnecting: Boolean = false,
    onConnectClick: () -> Unit
) {
    Button(
        onClick = onConnectClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(bottom = 12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF2196F3)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        if (isConnecting) {
            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = "CONNECT",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun LogoutButton(
    onLogoutClick: () -> Unit
) {
    Button(
        onClick = onLogoutClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(bottom = 12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Red
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = "LOGOUT",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun LockAppInfo() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 16.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Info,
            contentDescription = "Info",
            tint = Color.Gray,
            modifier = Modifier
                .size(20.dp)
                .padding(end = 4.dp)
        )
        Text(
            text = "Lock the app in recent tasks",
            color = Color.Gray,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun Footer() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(bottom = 8.dp)
    ) {
        Text(
            text = "Developed by Noel Georgi, Mathew Manachery",
            color = Color.Gray,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Logo by Muhammed Basil",
            color = Color.Gray,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Version 2.0.0",
                color = Color.Gray,
                fontSize = 12.sp
            )
            Image(
                painter = painterResource(id = R.drawable.github),
                contentDescription = "GitHub",
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

private fun saveCredentials(context: Context, username: String, password: String) {
    val sharedPreferences = context.getSharedPreferences("wifix_prefs", Context.MODE_PRIVATE)
    sharedPreferences.edit() {
        putString("username", username)
            .putString("password", password)
    }
}

private fun saveWifixingState(context: Context, isWifixing: Boolean, username: String, password: String) {
    val sharedPreferences = context.getSharedPreferences("wifix_prefs", Context.MODE_PRIVATE)
    sharedPreferences.edit() {
        putBoolean("wifixing_enabled", isWifixing)
            .putString("username", username)
            .putString("password", password)
    }
}

private fun startWifiFixService(context: Context) {
    val intent = Intent(context, WifiFixBackgroundService::class.java)
    context.startService(intent)
}

private fun stopWifiFixService(context: Context) {
    val intent = Intent(context, WifiFixBackgroundService::class.java)
    context.stopService(intent)
}

@Preview(showBackground = true)
@Composable
fun WifixHomeScreenPreview() {
    WifixTheme {
        WifixHomeScreen()
    }
}