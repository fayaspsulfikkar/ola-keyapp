package com.s1pro.companion

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.s1pro.companion.bluetooth.BLEGattClient
import com.s1pro.companion.bluetooth.ConnectionState

@Composable
fun ConnectionScreen(
    gattClient: BLEGattClient,
    deviceAddress: String,
    onBack: () -> Unit,
    onProceedToDiagnostics: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by gattClient.connectionState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Scooter Connection",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Address: $deviceAddress",
            fontSize = 16.sp,
            color = Color.Gray,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
        )
        
        Spacer(modifier = Modifier.height(64.dp))
        
        if (state == ConnectionState.CONNECTING || state == ConnectionState.DISCOVERING_SERVICES) {
            CircularProgressIndicator(
                color = Color(0xFF1E88E5),
                modifier = Modifier.size(64.dp),
                strokeWidth = 4.dp
            )
            Spacer(modifier = Modifier.height(32.dp))
        } else if (state == ConnectionState.READY) {
            Text(
                text = "✓",
                fontSize = 64.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4CAF50)
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
        
        val statusText = when (state) {
            ConnectionState.DISCONNECTED -> "Disconnected"
            ConnectionState.CONNECTING -> "Connecting to Scooter..."
            ConnectionState.CONNECTED -> "Connected! Waiting..."
            ConnectionState.DISCOVERING_SERVICES -> "Discovering Services..."
            ConnectionState.READY -> "Ready & Discovered!"
        }
        
        Text(
            text = statusText,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = when (state) {
                ConnectionState.READY -> Color(0xFF4CAF50)
                ConnectionState.DISCONNECTED -> Color(0xFFF44336)
                else -> Color.White
            }
        )
        
        if (state == ConnectionState.READY) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "All services and characteristics have been safely logged to BLE_GATT_LOG.md on your device.",
                color = Color.LightGray,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onProceedToDiagnostics,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5)),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("Proceed to Diagnostics", color = Color.White, fontSize = 16.sp)
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onBack,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF424242)),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Disconnect & Go Back", color = Color.White, fontSize = 16.sp)
        }
    }
}
