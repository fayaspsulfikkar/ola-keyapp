package com.s1pro.companion

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.s1pro.companion.bluetooth.BLEDevice
import com.s1pro.companion.bluetooth.BLEScanner

@Composable
fun ScannerScreen(
    scanner: BLEScanner,
    onBack: () -> Unit,
    onConnect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isScanning by scanner.isScanning.collectAsState()
    val devices by scanner.scannedDevices.collectAsState()

    DisposableEffect(Unit) {
        scanner.startScan()
        onDispose {
            scanner.stopScan()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF424242))) {
                Text("Back", color = Color.White)
            }
            Text(
                text = "BLE DIAGNOSTICS",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            if (isScanning) {
                CircularProgressIndicator(
                    color = Color(0xFF1E88E5),
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Spacer(modifier = Modifier.width(24.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = if (devices.isEmpty() && isScanning) "Searching for S1 Pro..." 
                   else if (devices.isEmpty()) "No scooters found."
                   else "Found ${devices.size} S1 Pro match(es)!",
            color = if (devices.isNotEmpty()) Color(0xFF4CAF50) else Color.LightGray,
            fontWeight = if (devices.isNotEmpty()) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(devices, key = { it.address }) { device ->
                DeviceCard(device = device, onConnect = onConnect)
            }
        }
    }
}

@Composable
fun DeviceCard(device: BLEDevice, onConnect: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = device.name ?: "Unknown Device",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "${device.rssi} dBm",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (device.rssi > -70) Color(0xFF4CAF50) else Color(0xFFFFA000)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = device.address,
                fontSize = 14.sp,
                color = Color.Gray,
                fontFamily = FontFamily.Monospace
            )
            
            if (device.serviceUuids.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = "Services:", fontSize = 14.sp, color = Color.LightGray, fontWeight = FontWeight.SemiBold)
                device.serviceUuids.forEach { uuid ->
                    Text(
                        text = uuid,
                        fontSize = 12.sp,
                        color = Color.Gray,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            if (device.manufacturerData != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Manufacturer Data:", fontSize = 14.sp, color = Color.LightGray, fontWeight = FontWeight.SemiBold)
                Text(
                    text = device.manufacturerData,
                    fontSize = 12.sp,
                    color = Color.Gray,
                    fontFamily = FontFamily.Monospace
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { onConnect(device.address) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5))
            ) {
                Text("Connect", color = Color.White)
            }
        }
    }
}
