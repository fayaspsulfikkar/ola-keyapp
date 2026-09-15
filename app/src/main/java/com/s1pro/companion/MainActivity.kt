package com.s1pro.companion

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.s1pro.companion.bluetooth.BluetoothPermissionState
import com.s1pro.companion.ui.theme.S1ProCompanionTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            S1ProCompanionTheme(darkTheme = true, dynamicColor = false) {
                val context = LocalContext.current
                val scanner = remember { com.s1pro.companion.bluetooth.BLEScanner(context) }
                val gattClient = remember { com.s1pro.companion.bluetooth.BLEGattClient(context) }
                var currentScreen by remember { mutableStateOf("dashboard") }
                var selectedDeviceAddress by remember { mutableStateOf<String?>(null) }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Color(0xFF121212)
                ) { innerPadding ->
                    BluetoothManagerWrapper(modifier = Modifier.padding(innerPadding)) { isReady ->
                        if (currentScreen == "dashboard") {
                            DashboardScreen(
                                isReady = isReady,
                                onNavigateToScanner = { currentScreen = "scanner" }
                            )
                        } else if (currentScreen == "scanner") {
                            ScannerScreen(
                                scanner = scanner,
                                onBack = { currentScreen = "dashboard" },
                                onConnect = { address ->
                                    selectedDeviceAddress = address
                                    currentScreen = "connection"
                                }
                            )
                        } else if (selectedDeviceAddress != null) {
                            DisposableEffect(selectedDeviceAddress) {
                                gattClient.connect(selectedDeviceAddress!!)
                                onDispose {
                                    gattClient.disconnect()
                                }
                            }

                            if (currentScreen == "connection") {
                                ConnectionScreen(
                                    gattClient = gattClient,
                                    deviceAddress = selectedDeviceAddress!!,
                                    onBack = { 
                                        selectedDeviceAddress = null
                                        currentScreen = "scanner" 
                                    },
                                    onProceedToDiagnostics = { currentScreen = "diagnostics" }
                                )
                            } else if (currentScreen == "diagnostics") {
                                BLEDiagnosticScreen(
                                    gattClient = gattClient,
                                    onBack = { currentScreen = "connection" }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BluetoothManagerWrapper(modifier: Modifier = Modifier, content: @Composable (isReady: Boolean) -> Unit) {
    val context = LocalContext.current
    var hasPermissions by remember { mutableStateOf(BluetoothPermissionState.hasPermissions(context)) }
    
    val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as android.bluetooth.BluetoothManager?
    val bluetoothAdapter = bluetoothManager?.adapter
    var isBluetoothEnabled by remember { mutableStateOf(bluetoothAdapter?.isEnabled == true) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasPermissions = BluetoothPermissionState.hasPermissions(context)
    }

    val enableBluetoothLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isBluetoothEnabled = bluetoothAdapter?.isEnabled == true
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: android.content.Intent?) {
                if (intent?.action == android.bluetooth.BluetoothAdapter.ACTION_STATE_CHANGED) {
                    val state = intent.getIntExtra(android.bluetooth.BluetoothAdapter.EXTRA_STATE, android.bluetooth.BluetoothAdapter.ERROR)
                    isBluetoothEnabled = state == android.bluetooth.BluetoothAdapter.STATE_ON
                }
            }
        }
        val filter = android.content.IntentFilter(android.bluetooth.BluetoothAdapter.ACTION_STATE_CHANGED)
        context.registerReceiver(receiver, filter)

        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                hasPermissions = BluetoothPermissionState.hasPermissions(context)
                isBluetoothEnabled = bluetoothAdapter?.isEnabled == true
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            context.unregisterReceiver(receiver)
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        if (!hasPermissions) {
            permissionLauncher.launch(BluetoothPermissionState.requiredPermissions.toTypedArray())
        }
    }

    if (bluetoothAdapter == null) {
        Column(
            modifier = modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Bluetooth Unavailable", color = Color.Red, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Text("This device does not support Bluetooth.", color = Color.LightGray)
        }
        return
    }

    if (!hasPermissions) {
        Column(
            modifier = modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Permissions Required", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "The companion app requires Bluetooth permissions to scan for and connect to your scooter.\n\nIf the prompt doesn't appear, please grant permissions in App Settings.",
                color = Color.LightGray,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))
            Row {
                Button(
                    onClick = { permissionLauncher.launch(BluetoothPermissionState.requiredPermissions.toTypedArray()) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5)),
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text("Retry", color = Color.White)
                }
                Button(
                    onClick = {
                        val intent = android.content.Intent(
                            android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            android.net.Uri.fromParts("package", context.packageName, null)
                        )
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF424242))
                ) {
                    Text("Open Settings", color = Color.White)
                }
            }
        }
    } else if (!isBluetoothEnabled) {
        Column(
            modifier = modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Bluetooth is Off", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Please enable Bluetooth to connect to your scooter.",
                color = Color.LightGray,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = {
                    val intent = android.content.Intent(android.bluetooth.BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    enableBluetoothLauncher.launch(intent)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5))
            ) {
                Text("Enable Bluetooth", color = Color.White)
            }
        }
    } else {
        Box(modifier = modifier) {
            content(true)
        }
    }
}

@Composable
fun DashboardScreen(modifier: Modifier = Modifier, isReady: Boolean = false, onNavigateToScanner: () -> Unit = {}) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "S1 PRO COMPANION",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            letterSpacing = 1.5.sp,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        SectionTitle("Bluetooth")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(if (isReady) Color(0xFF4CAF50) else Color(0xFFFFA000), shape = RoundedCornerShape(50))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isReady) "Ready" else "Checking...",
                color = Color.LightGray,
                fontSize = 16.sp
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        SectionTitle("Scooter")
        Text(
            text = "Not connected",
            color = Color.LightGray,
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Button(
            onClick = { /* TODO */ },
            enabled = isReady,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5)),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Scan for Scooter", modifier = Modifier.padding(vertical = 8.dp), color = Color.White)
        }

        Spacer(modifier = Modifier.height(48.dp))

        SectionTitle("Diagnostics")
        Button(
            onClick = onNavigateToScanner,
            enabled = isReady,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF424242)),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("BLE Scanner", modifier = Modifier.padding(vertical = 8.dp), color = Color.White)
        }

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = "Settings",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color.White,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Preview(showBackground = true)
@Composable
fun DashboardPreview() {
    S1ProCompanionTheme(darkTheme = true, dynamicColor = false) {
        Surface(color = Color(0xFF121212)) {
            DashboardScreen(isReady = true)
        }
    }
}