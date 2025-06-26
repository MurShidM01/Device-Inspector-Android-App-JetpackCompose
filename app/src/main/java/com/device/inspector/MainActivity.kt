package com.device.inspector

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Point
import android.hardware.Sensor
import android.hardware.SensorManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.safetynet.SafetyNet
import com.google.android.gms.safetynet.SafetyNetApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.NetworkInterface
import java.text.DecimalFormat
import kotlin.math.roundToInt
import com.device.inspector.ui.theme.DeviceInspectorTheme
import android.content.Intent
import android.content.IntentFilter
import com.scottyab.rootbeer.RootBeer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.shadow
import android.net.DhcpInfo
import android.net.TrafficStats
import android.bluetooth.BluetoothAdapter
import java.net.InetAddress
import java.net.URL
import java.util.concurrent.Executors
import java.util.concurrent.Future
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Close
import android.net.Uri

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DeviceInspectorTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }
}

sealed class Screen(val route: String, val title: String, val icon: @Composable () -> Unit) {
    object System : Screen("system", "System", { Icon(Icons.Filled.Android, "System") })
    object Hardware : Screen("hardware", "Hardware", { Icon(Icons.Filled.Memory, "Hardware") })
    object Network : Screen("network", "Network", { Icon(Icons.Filled.Wifi, "Network") })
    object Battery : Screen("battery", "Battery", { Icon(Icons.Filled.BatteryFull, "Battery") })
    object Sensors : Screen("sensors", "Sensors", { Icon(Icons.Filled.Sensors, "Sensors") })
}

val sectionGradients = listOf(
    Brush.linearGradient(listOf(Color(0xFF7F7FD5), Color(0xFF86A8E7), Color(0xFF91EAE4))), // System
    Brush.linearGradient(listOf(Color(0xFF43CEA2), Color(0xFF185A9D))), // Hardware
    Brush.linearGradient(listOf(Color(0xFFFF512F), Color(0xFFDD2476))), // Network
    Brush.linearGradient(listOf(Color(0xFF614385), Color(0xFF516395))), // Battery
    Brush.linearGradient(listOf(Color(0xFF11998E), Color(0xFF38EF7D)))  // Sensors
)

val sectionIcons = listOf(
    Icons.Filled.Info,      // System
    Icons.Filled.Storage,   // Hardware
    Icons.Filled.Wifi,      // Network
    Icons.Filled.BatteryStd,// Battery
    Icons.Filled.Sensors    // Sensors
)

@Composable
fun GradientAppBar(title: String, icon: ImageVector, onInfoClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0xFF43CEA2), Color(0xFF185A9D))
                )
            )
            .zIndex(1f),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.padding(start = 20.dp, end = 8.dp).size(32.dp)
            )
            Text(
                text = title,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 2.dp).weight(1f)
            )
            IconButton(
                onClick = onInfoClick,
                modifier = Modifier.padding(end = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.HelpOutline,
                    contentDescription = "App Info",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
fun AnimatedSplashScreen(showSplash: Boolean) {
    AnimatedVisibility(
        visible = showSplash,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut(animationSpec = tween(durationMillis = 1000)) + shrinkVertically()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF43CEA2), Color(0xFF185A9D))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Filled.DeviceHub,
                    contentDescription = "App Logo",
                    modifier = Modifier.size(120.dp),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Device Inspector",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp
                    ),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun AppInfoDialog(show: Boolean, onDismiss: () -> Unit) {
    if (show) {
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()
        var updateStatus by remember { mutableStateOf<String?>(null) }
        var latestReleaseUrl by remember { mutableStateOf<String?>(null) }
        var checkingUpdate by remember { mutableStateOf(false) }
        val currentVersion = "1.0"
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x66000000))
                .wrapContentSize(Alignment.Center)
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(12.dp),
                modifier = Modifier
                    .padding(24.dp)
                    .widthIn(max = 360.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.background(Color.White)
                ) {
                    // Gradient header with icon
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF43CEA2), Color(0xFF185A9D))
                                ),
                                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                            )
                            .padding(top = 24.dp, bottom = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.HelpOutline,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Device Inspector",
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = Color(0xFF185A9D),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Text(
                        "Version: $currentVersion",
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF43CEA2),
                        fontSize = 16.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Divider(modifier = Modifier.padding(horizontal = 24.dp), color = Color(0xFF43CEA2), thickness = 1.dp)
                    Spacer(Modifier.height(12.dp))
                    Text("Ali Khan Jalbani", fontWeight = FontWeight.Medium, color = Color(0xFF185A9D))
                    Spacer(Modifier.height(4.dp))
                    Text("alikhanjalbani@outlook.com", fontWeight = FontWeight.Medium, color = Color(0xFF185A9D))
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Device Inspector is an Open-Source tool to view detailed system, hardware, battery, and network info of your Android device. Built fully with Jetpack Compose, it features a modern UI, splash screen, and bottom navigation. Works perfectly on non-rooted devices and respects your privacy.",
                        color = Color.DarkGray,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 18.dp)
                    )
                    Spacer(Modifier.height(18.dp))
                    // Update check section
                    if (updateStatus == null && !checkingUpdate) {
                        Button(
                            onClick = {
                                checkingUpdate = true
                                updateStatus = null
                                coroutineScope.launch {
                                    try {
                                        val apiUrl = "https://api.github.com/repos/MurShidM01/Device-Inspector-Android-App-JetpackCompose/releases/latest"
                                        val json = withContext(Dispatchers.IO) { URL(apiUrl).readText() }
                                        val tag = Regex("""\"tag_name\"\s*:\s*\"([^\"]+)\"""").find(json)?.groupValues?.get(1) ?: ""
                                        val htmlUrl = Regex("""\"html_url\"\s*:\s*\"([^\"]+)\"""").find(json)?.groupValues?.get(1)
                                        latestReleaseUrl = htmlUrl
                                        val latestVersion = tag.trimStart('v', 'V')
                                        if (latestVersion > currentVersion) {
                                            updateStatus = "new_version"
                                        } else {
                                            updateStatus = "latest"
                                        }
                                    } catch (e: Exception) {
                                        updateStatus = "error"
                                    }
                                    checkingUpdate = false
                                }
                            },
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF185A9D)),
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Icon(Icons.Filled.HelpOutline, contentDescription = null, tint = Color.White)
                            Spacer(Modifier.width(6.dp))
                            Text("Check for Update", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    } else if (checkingUpdate) {
                        CircularProgressIndicator(color = Color(0xFF43CEA2), modifier = Modifier.size(32.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("Checking for updates...", color = Color.Gray, fontSize = 14.sp)
                    } else if (updateStatus == "new_version" && latestReleaseUrl != null) {
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(latestReleaseUrl))
                                context.startActivity(intent)
                            },
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF43CEA2)),
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Icon(Icons.Filled.HelpOutline, contentDescription = null, tint = Color.White)
                            Spacer(Modifier.width(6.dp))
                            Text("Update Available!", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Text("A new version is available.", color = Color(0xFF185A9D), fontWeight = FontWeight.Medium)
                    } else if (updateStatus == "latest") {
                        Text("You are using the latest version.", color = Color(0xFF43CEA2), fontWeight = FontWeight.Bold)
                    } else if (updateStatus == "error") {
                        Text("Could not check for updates.", color = Color.Red, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF43CEA2)),
                        modifier = Modifier.padding(bottom = 18.dp)
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = null, tint = Color.White)
                        Spacer(Modifier.width(6.dp))
                        Text("Close", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    var showSplash by remember { mutableStateOf(true) }
    val navController = rememberNavController()
    val items = listOf(Screen.System, Screen.Hardware, Screen.Network, Screen.Battery, Screen.Sensors)
    var currentScreenIndex by remember { mutableStateOf(0) }
    var showInfoDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(1800)
        showSplash = false
    }

    AnimatedSplashScreen(showSplash)

    AnimatedVisibility(
        visible = !showSplash,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Scaffold(
            topBar = {
                GradientAppBar(
                    title = items[currentScreenIndex].title,
                    icon = sectionIcons[currentScreenIndex],
                    onInfoClick = { showInfoDialog = true }
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = Color.White,
                    tonalElevation = 8.dp
                ) {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination
                    items.forEachIndexed { idx, screen ->
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = sectionIcons[idx],
                                    contentDescription = screen.title,
                                    tint = if (currentDestination?.hierarchy?.any { it.route == screen.route } == true)
                                        MaterialTheme.colorScheme.primary else Color.Gray
                                )
                            },
                            label = {
                                Text(
                                    screen.title,
                                    fontWeight = if (currentDestination?.hierarchy?.any { it.route == screen.route } == true)
                                        FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                                currentScreenIndex = idx
                            }
                        )
                    }
                }
            },
            containerColor = Color(0xFFF7F9FB)
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.System.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Screen.System.route) { SystemScreenModern() }
                composable(Screen.Hardware.route) { HardwareScreenModern() }
                composable(Screen.Network.route) { NetworkScreenModern() }
                composable(Screen.Battery.route) { BatteryScreenModern() }
                composable(Screen.Sensors.route) { SensorsScreenModern() }
            }
            AppInfoDialog(show = showInfoDialog, onDismiss = { showInfoDialog = false })
        }
    }
}

@Composable
fun InfoCardModern(
    title: String,
    icon: ImageVector,
    gradient: Brush,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp, horizontal = 8.dp)
            .shadow(8.dp, RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(gradient, shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                .padding(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            )
        }
        Divider(color = Color(0xFFF7F9FB), thickness = 1.dp)
        Column(
            modifier = Modifier
                .padding(18.dp)
                .fillMaxWidth()
        ) {
            content()
        }
    }
}

@Composable
fun InfoRowModern(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            color = Color(0xFF185A9D)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            color = Color(0xFF43CEA2)
        )
    }
}

// Modernized Section Screens
@Composable
fun SystemScreenModern() {
    val context = LocalContext.current
    val rootBeer = remember { RootBeer(context) }
    val scope = rememberCoroutineScope()
    var safetyNetStatus by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        scope.launch {
            withContext(Dispatchers.IO) {
                try {
                    if (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS) {
                        SafetyNet.getClient(context).attest(ByteArray(16), "YOUR_API_KEY")
                            .addOnSuccessListener { response: SafetyNetApi.AttestationResponse ->
                                safetyNetStatus = "Device is certified"
                            }
                            .addOnFailureListener { e: Exception ->
                                safetyNetStatus = "Device certification failed"
                            }
                    } else {
                        safetyNetStatus = "Google Play Services not available"
                    }
                } catch (e: Exception) {
                    safetyNetStatus = "SafetyNet check failed"
                }
            }
        }
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 8.dp, bottom = 16.dp)
    ) {
        item {
            InfoCardModern("Android System", Icons.Filled.Info, sectionGradients[0]) {
                InfoRowModern("Android Version", Build.VERSION.RELEASE)
                InfoRowModern("SDK Level", Build.VERSION.SDK_INT.toString())
                InfoRowModern("Security Patch", Build.VERSION.SECURITY_PATCH)
                InfoRowModern("Build ID", Build.ID)
                InfoRowModern("Build Type", Build.TYPE)
            }
        }
        item {
            InfoCardModern("Device Information", Icons.Filled.DeveloperMode, sectionGradients[0]) {
                InfoRowModern("Manufacturer", Build.MANUFACTURER)
                InfoRowModern("Brand", Build.BRAND)
                InfoRowModern("Model", Build.MODEL)
                InfoRowModern("Product", Build.PRODUCT)
                InfoRowModern("Device", Build.DEVICE)
            }
        }
        item {
            InfoCardModern("Security", Icons.Filled.Power, sectionGradients[0]) {
                InfoRowModern("Root Status", if (rootBeer.isRooted) "Rooted" else "Not Rooted")
                InfoRowModern("USB Debugging", if (Settings.Global.getInt(context.contentResolver, Settings.Global.ADB_ENABLED, 0) == 1) "Enabled" else "Disabled")
                InfoRowModern("Developer Options", if (Settings.Global.getInt(context.contentResolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) == 1) "Enabled" else "Disabled")
                InfoRowModern("SafetyNet Status", safetyNetStatus ?: "Checking...")
            }
        }
    }
}

@Composable
fun HardwareScreenModern() {
    val context = LocalContext.current
    val windowManager = remember { context.getSystemService(Context.WINDOW_SERVICE) as WindowManager }
    val activityManager = remember { context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager }
    val displayMetrics = remember { DisplayMetrics().also { windowManager.defaultDisplay.getMetrics(it) } }
    val point = remember { Point().also { windowManager.defaultDisplay.getRealSize(it) } }
    val memoryInfo = remember { ActivityManager.MemoryInfo().also { activityManager.getMemoryInfo(it) } }
    val statFs = remember { StatFs(Environment.getDataDirectory().path) }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 8.dp, bottom = 16.dp)
    ) {
        item {
            InfoCardModern("Display", Icons.Filled.Info, sectionGradients[1]) {
                InfoRowModern("Resolution", "${point.x} x ${point.y}")
                InfoRowModern("Density", "${(displayMetrics.density * 160f).roundToInt()} DPI")
                InfoRowModern("Refresh Rate", "${windowManager.defaultDisplay.refreshRate.roundToInt()} Hz")
                InfoRowModern("Screen Size", "${displayMetrics.widthPixels}x${displayMetrics.heightPixels}")
            }
        }
        item {
            InfoCardModern("Processor", Icons.Filled.Storage, sectionGradients[1]) {
                InfoRowModern("CPU Architecture", Build.SUPPORTED_ABIS[0])
                InfoRowModern("Processor", Build.HARDWARE)
                InfoRowModern("Cores", Runtime.getRuntime().availableProcessors().toString())
            }
        }
        item {
            InfoCardModern("Memory", Icons.Filled.Storage, sectionGradients[1]) {
                InfoRowModern("Total RAM", formatSize(memoryInfo.totalMem))
                InfoRowModern("Available RAM", formatSize(memoryInfo.availMem))
                InfoRowModern("Low Memory", if (memoryInfo.lowMemory) "Yes" else "No")
                InfoRowModern("Threshold", formatSize(memoryInfo.threshold))
            }
        }
        item {
            InfoCardModern("Storage", Icons.Filled.Storage, sectionGradients[1]) {
                InfoRowModern("Total Storage", formatSize(statFs.totalBytes))
                InfoRowModern("Available Storage", formatSize(statFs.availableBytes))
                InfoRowModern("Free Storage", formatSize(statFs.freeBytes))
            }
        }
    }
}

@Composable
fun NetworkScreenModern() {
    val context = LocalContext.current
    val connectivityManager = remember { context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager }
    val wifiManager = remember { context.getSystemService(Context.WIFI_SERVICE) as WifiManager }
    val telephonyManager = remember { context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager }
    val networkCapabilities = remember {
        connectivityManager.activeNetwork?.let { connectivityManager.getNetworkCapabilities(it) }
    }
    val dhcpInfo = remember { wifiManager.dhcpInfo }
    val bluetoothAdapter = remember { BluetoothAdapter.getDefaultAdapter() }
    val interfaces = remember { NetworkInterface.getNetworkInterfaces().toList() }
    val (publicIp, setPublicIp) = remember { mutableStateOf("Loading...") }
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val ip = URL("https://api.ipify.org").readText()
                setPublicIp(ip)
            } catch (e: Exception) {
                setPublicIp("Unavailable")
            }
        }
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 8.dp, bottom = 16.dp)
    ) {
        item {
            InfoCardModern("Network Status", Icons.Filled.Wifi, sectionGradients[2]) {
                InfoRowModern("Internet Available", if (networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true) "Yes" else "No")
                InfoRowModern("Network Type", when {
                    networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> "WiFi"
                    networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "Cellular"
                    else -> "None"
                })
                InfoRowModern("Local IP", getLocalIpAddress())
                InfoRowModern("Public IP", publicIp)
                InfoRowModern("Gateway IP", dhcpInfo?.gateway?.let { intToIp(it) } ?: "Unknown")
                InfoRowModern("DNS 1", dhcpInfo?.dns1?.let { intToIp(it) } ?: "Unknown")
                InfoRowModern("DNS 2", dhcpInfo?.dns2?.let { intToIp(it) } ?: "Unknown")
            }
        }
        item {
            InfoCardModern("WiFi Information", Icons.Filled.Wifi, sectionGradients[2]) {
                InfoRowModern("WiFi Enabled", if (wifiManager.isWifiEnabled) "Yes" else "No")
                if (wifiManager.isWifiEnabled) {
                    val wifiInfo = try { wifiManager.connectionInfo } catch (e: Exception) { null }
                    InfoRowModern("SSID", wifiInfo?.ssid?.removePrefix("\"")?.removeSuffix("\"") ?: "Unknown")
                    InfoRowModern("Signal Strength", wifiInfo?.rssi?.toString()?.plus(" dBm") ?: "Unknown")
                    InfoRowModern("Link Speed", wifiInfo?.linkSpeed?.toString()?.plus(" Mbps") ?: "Unknown")
                    InfoRowModern("MAC Address", try { getMacAddress() } catch (e: Exception) { "Unknown" })
                    InfoRowModern("Frequency", wifiInfo?.frequency?.let { "$it MHz (${if (it >= 5000) "5GHz" else "2.4GHz"})" } ?: "Unknown")
                    InfoRowModern("Supplicant State", wifiInfo?.supplicantState?.toString() ?: "Unknown")
                }
            }
        }
        item {
            InfoCardModern("Mobile Network", Icons.Filled.Wifi, sectionGradients[2]) {
                InfoRowModern("Network Operator", telephonyManager.networkOperatorName.takeIf { !it.isNullOrBlank() } ?: "Unknown")
                InfoRowModern("SIM Operator", telephonyManager.simOperatorName.takeIf { !it.isNullOrBlank() } ?: "Unknown")
                InfoRowModern("Network Type", try { getNetworkType(telephonyManager) } catch (e: Exception) { "Unknown" })
                InfoRowModern("Roaming", if (runCatching { telephonyManager.isNetworkRoaming }.getOrDefault(false)) "Yes" else "No")
            }
        }
        item {
            InfoCardModern("Bluetooth & Data", Icons.Filled.Wifi, sectionGradients[2]) {
                InfoRowModern("Bluetooth Supported", if (bluetoothAdapter != null) "Yes" else "No")
                InfoRowModern("Bluetooth Enabled", if (bluetoothAdapter?.isEnabled == true) "Yes" else "No")
                InfoRowModern("Data Sent", formatSize(TrafficStats.getTotalTxBytes()))
                InfoRowModern("Data Received", formatSize(TrafficStats.getTotalRxBytes()))
            }
        }
        item {
            InfoCardModern("Network Interfaces", Icons.Filled.Wifi, sectionGradients[2]) {
                if (interfaces.isEmpty()) {
                    Text("No interfaces found", color = Color.Gray, fontStyle = FontStyle.Italic)
                } else {
                    interfaces.forEach { iface ->
                        Text(
                            text = iface.displayName,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF185A9D),
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val ips = iface.inetAddresses.toList().filter { it.hostAddress != null && !it.isLoopbackAddress }
                        if (ips.isEmpty()) {
                            Text(
                                text = "  No IP addresses",
                                color = Color.Gray,
                                fontSize = 13.sp,
                                fontStyle = FontStyle.Italic
                            )
                        } else {
                            ips.forEach { ip ->
                                Text(
                                    text = "  ${ip.hostAddress}",
                                    color = Color.DarkGray,
                                    fontSize = 13.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun BatteryScreenModern() {
    val context = LocalContext.current
    val batteryManager = remember { context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager }
    val batteryStatus = remember {
        context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }
    val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
    val percent = if (level >= 0 && scale > 0) (level * 100 / scale) else -1
    val temperature = (batteryStatus?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)?.toFloat() ?: -1f) / 10f
    val voltage = batteryStatus?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1) ?: -1
    val technology = batteryStatus?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "Unknown"
    val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
    val health = batteryStatus?.getIntExtra(BatteryManager.EXTRA_HEALTH, -1) ?: -1
    val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
    val present = batteryStatus?.getBooleanExtra(BatteryManager.EXTRA_PRESENT, true) ?: true
    val plugged = batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
    val chargingSource = when (plugged) {
        BatteryManager.BATTERY_PLUGGED_USB -> "USB"
        BatteryManager.BATTERY_PLUGGED_AC -> "AC"
        BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
        else -> "Unknown"
    }
    val capacity = getBatteryCapacity(context)
    val chargeCounter = try { batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER) } catch (e: Exception) { -1 }
    val energyCounter = try { batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_ENERGY_COUNTER) } catch (e: Exception) { -1L }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 8.dp, bottom = 16.dp)
    ) {
        item {
            InfoCardModern("General Status", Icons.Filled.BatteryStd, sectionGradients[3]) {
                InfoRowModern("Level", if (percent >= 0) "$percent%" else "Unknown")
                InfoRowModern("Charging", if (isCharging) "Yes" else "No")
                InfoRowModern("Charging Source", chargingSource)
                InfoRowModern("Present", if (present) "Yes" else "No")
            }
        }
        item {
            InfoCardModern("Technical Details", Icons.Filled.BatteryStd, sectionGradients[3]) {
                InfoRowModern("Temperature", if (temperature >= 0) "$temperature°C" else "Unknown")
                InfoRowModern("Voltage", if (voltage >= 0) "$voltage mV" else "Unknown")
                InfoRowModern("Technology", technology)
                InfoRowModern("Capacity", if (capacity > 0) "$capacity mAh" else "Unknown")
            }
        }
        item {
            InfoCardModern("Counters", Icons.Filled.BatteryStd, sectionGradients[3]) {
                InfoRowModern("Charge Counter", if (chargeCounter > 0) "$chargeCounter µAh" else "Unknown")
                InfoRowModern("Energy Counter", if (energyCounter > 0) "$energyCounter nWh" else "Unknown")
            }
        }
        item {
            InfoCardModern("Health", Icons.Filled.BatteryStd, sectionGradients[3]) {
                InfoRowModern("Status", getBatteryStatus(status))
                InfoRowModern("Health", getBatteryHealth(health))
            }
        }
    }
}

@Composable
fun SensorsScreenModern() {
    val context = LocalContext.current
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val sensors = remember { sensorManager.getSensorList(Sensor.TYPE_ALL) }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 8.dp, bottom = 16.dp)
    ) {
        items(sensors) { sensor ->
            InfoCardModern(sensor.name, Icons.Filled.Sensors, sectionGradients[4]) {
                InfoRowModern("Type", getSensorType(sensor.type))
                InfoRowModern("Vendor", sensor.vendor)
                InfoRowModern("Version", sensor.version.toString())
                InfoRowModern("Power", "${sensor.power} mA")
                InfoRowModern("Resolution", "${sensor.resolution}")
                InfoRowModern("Maximum Range", "${sensor.maximumRange}")
            }
        }
    }
}

// Utility functions
private fun formatSize(size: Long): String {
    val df = DecimalFormat("0.00")
    val kb = size.toFloat() / 1024
    val mb = kb / 1024
    val gb = mb / 1024
    return when {
        gb >= 1 -> "${df.format(gb)} GB"
        mb >= 1 -> "${df.format(mb)} MB"
        kb >= 1 -> "${df.format(kb)} KB"
        else -> "${size} Bytes"
    }
}

private fun getMacAddress(): String {
    try {
        val interfaces = NetworkInterface.getNetworkInterfaces()
        while (interfaces.hasMoreElements()) {
            val networkInterface = interfaces.nextElement()
            if (networkInterface.name.equals("wlan0", ignoreCase = true)) {
                val mac = networkInterface.hardwareAddress
                val stringBuilder = StringBuilder()
                for (i in mac.indices) {
                    stringBuilder.append(String.format("%02X:", mac[i]))
                }
                if (stringBuilder.isNotEmpty()) {
                    stringBuilder.deleteCharAt(stringBuilder.length - 1)
                }
                return stringBuilder.toString()
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return "Unknown"
}

private fun getNetworkType(telephonyManager: TelephonyManager): String {
    return when (telephonyManager.dataNetworkType) {
        TelephonyManager.NETWORK_TYPE_GPRS,
        TelephonyManager.NETWORK_TYPE_EDGE,
        TelephonyManager.NETWORK_TYPE_CDMA,
        TelephonyManager.NETWORK_TYPE_1xRTT,
        TelephonyManager.NETWORK_TYPE_IDEN -> "2G"
        TelephonyManager.NETWORK_TYPE_UMTS,
        TelephonyManager.NETWORK_TYPE_EVDO_0,
        TelephonyManager.NETWORK_TYPE_EVDO_A,
        TelephonyManager.NETWORK_TYPE_HSDPA,
        TelephonyManager.NETWORK_TYPE_HSUPA,
        TelephonyManager.NETWORK_TYPE_HSPA,
        TelephonyManager.NETWORK_TYPE_EVDO_B,
        TelephonyManager.NETWORK_TYPE_EHRPD,
        TelephonyManager.NETWORK_TYPE_HSPAP -> "3G"
        TelephonyManager.NETWORK_TYPE_LTE -> "4G"
        TelephonyManager.NETWORK_TYPE_NR -> "5G"
        else -> "Unknown"
    }
}

private fun getBatteryStatus(status: Int): String {
    return when (status) {
        BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
        BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
        BatteryManager.BATTERY_STATUS_FULL -> "Full"
        BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not Charging"
        BatteryManager.BATTERY_STATUS_UNKNOWN -> "Unknown"
        else -> "Unknown"
    }
}

private fun getBatteryHealth(health: Int): String {
    return when (health) {
        BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
        BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
        BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
        BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
        BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
        BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Unspecified Failure"
        BatteryManager.BATTERY_HEALTH_UNKNOWN -> "Unknown"
        else -> "Unknown"
    }
}

private fun getSensorType(type: Int): String {
    return when (type) {
        Sensor.TYPE_ACCELEROMETER -> "Accelerometer"
        Sensor.TYPE_AMBIENT_TEMPERATURE -> "Ambient Temperature"
        Sensor.TYPE_GRAVITY -> "Gravity"
        Sensor.TYPE_GYROSCOPE -> "Gyroscope"
        Sensor.TYPE_LIGHT -> "Light"
        Sensor.TYPE_LINEAR_ACCELERATION -> "Linear Acceleration"
        Sensor.TYPE_MAGNETIC_FIELD -> "Magnetic Field"
        Sensor.TYPE_PRESSURE -> "Pressure"
        Sensor.TYPE_PROXIMITY -> "Proximity"
        Sensor.TYPE_RELATIVE_HUMIDITY -> "Relative Humidity"
        Sensor.TYPE_ROTATION_VECTOR -> "Rotation Vector"
        Sensor.TYPE_SIGNIFICANT_MOTION -> "Significant Motion"
        Sensor.TYPE_STEP_COUNTER -> "Step Counter"
        Sensor.TYPE_STEP_DETECTOR -> "Step Detector"
        else -> "Unknown"
    }
}

// --- Helper functions ---
private fun getLocalIpAddress(): String {
    return try {
        NetworkInterface.getNetworkInterfaces().toList().flatMap { it.inetAddresses.toList() }
            .firstOrNull { !it.isLoopbackAddress && it is InetAddress && it.hostAddress.indexOf(':') < 0 }?.hostAddress ?: "Unknown"
    } catch (e: Exception) {
        "Unknown"
    }
}

private fun intToIp(i: Int): String =
    ((i and 0xFF).toString() + "." +
            ((i shr 8) and 0xFF) + "." +
            ((i shr 16) and 0xFF) + "." +
            ((i shr 24) and 0xFF))

private fun getBatteryCapacity(context: Context): Int {
    return try {
        val mPowerProfileClass = Class.forName("com.android.internal.os.PowerProfile")
        val constructor = mPowerProfileClass.getConstructor(Context::class.java)
        val powerProfile = constructor.newInstance(context)
        val method = mPowerProfileClass.getMethod("getBatteryCapacity")
        (method.invoke(powerProfile) as Double).toInt()
    } catch (e: Exception) {
        -1
    }
}