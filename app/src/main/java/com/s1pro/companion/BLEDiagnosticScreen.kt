package com.s1pro.companion

import android.bluetooth.BluetoothGattCharacteristic
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.s1pro.companion.bluetooth.BLEGattClient
import java.util.UUID

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import android.widget.Toast
import java.io.File

@Composable
fun BLEDiagnosticScreen(
    gattClient: BLEGattClient,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val services by gattClient.discoveredServices.collectAsState()
    val readResults by gattClient.readResults.collectAsState()
    val notificationResults by gattClient.notificationResults.collectAsState()
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain"),
        onResult = { uri ->
            if (uri != null) {
                scope.launch(Dispatchers.IO) {
                    try {
                        val srcFile = File(context.getExternalFilesDir(null), "ProtocolObservation.txt")
                        if (srcFile.exists()) {
                            context.contentResolver.openOutputStream(uri)?.use { out ->
                                srcFile.inputStream().use { input ->
                                    input.copyTo(out)
                                }
                            }
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Logs exported successfully!", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Log file is empty or not found.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    )

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
                text = "GATT DIAGNOSTICS",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Button(
                onClick = { exportLauncher.launch("ProtocolObservation.txt") },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Text("Export", color = Color.White, fontSize = 14.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (services.isEmpty()) {
            Text("No services found or not connected.", color = Color.LightGray)
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(services, key = { it.uuid.toString() }) { service ->
                    ServiceCard(
                        serviceUuid = service.uuid,
                        characteristics = service.characteristics,
                        readResults = readResults,
                        notificationResults = notificationResults,
                        onRead = { charUuid -> gattClient.readCharacteristic(service.uuid, charUuid) },
                        onSubscribe = { charUuid -> gattClient.subscribeToCharacteristic(service.uuid, charUuid) }
                    )
                }
            }
        }
    }
}

@Composable
fun ServiceCard(
    serviceUuid: UUID,
    characteristics: List<BluetoothGattCharacteristic>,
    readResults: Map<String, ByteArray>,
    notificationResults: Map<String, ByteArray>,
    onRead: (UUID) -> Unit,
    onSubscribe: (UUID) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Service:",
                fontSize = 14.sp,
                color = Color.LightGray,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = serviceUuid.toString(),
                fontSize = 16.sp,
                color = Color.White,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = Color(0xFF333333))
            Spacer(modifier = Modifier.height(8.dp))

            characteristics.forEach { char ->
                val charUuid = char.uuid.toString()
                val props = char.properties
                val isReadable = (props and BluetoothGattCharacteristic.PROPERTY_READ) != 0
                val isWritable = (props and BluetoothGattCharacteristic.PROPERTY_WRITE) != 0 || (props and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0
                val isNotifiable = (props and BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0
                val isIndicatable = (props and BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0
                val canSubscribe = isNotifiable || isIndicatable

                val propString = mutableListOf<String>()
                if (isReadable) propString.add("READ")
                if (isWritable) propString.add("WRITE")
                if (isNotifiable) propString.add("NOTIFY")
                if (isIndicatable) propString.add("INDICATE")

                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Char: ${charUuid.split("-")[0]}", // Shorten UUID
                            fontSize = 14.sp,
                            color = Color(0xFF90CAF9),
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                        Text(
                            text = propString.joinToString(", "),
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                        
                        val result = readResults[charUuid]
                        if (result != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Read: " + result.joinToString("") { "%02X ".format(it) },
                                fontSize = 14.sp,
                                color = Color(0xFF4CAF50),
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        }

                        val notifResult = notificationResults[charUuid]
                        if (notifResult != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Stream: " + notifResult.joinToString("") { "%02X ".format(it) },
                                fontSize = 14.sp,
                                color = Color(0xFFFFC107),
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        }
                    }
                    
                    Column(horizontalAlignment = Alignment.End) {
                        if (isReadable) {
                            Button(
                                onClick = { onRead(char.uuid) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF424242)),
                                modifier = Modifier.height(32.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                            ) {
                                Text("Read", fontSize = 12.sp, color = Color.White)
                            }
                        }
                        
                        if (canSubscribe) {
                            if (isReadable) Spacer(modifier = Modifier.height(4.dp))
                            Button(
                                onClick = { onSubscribe(char.uuid) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5)),
                                modifier = Modifier.height(32.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                            ) {
                                Text("Subscribe", fontSize = 12.sp, color = Color.White)
                            }
                        }
                        
                        if (!isReadable && !canSubscribe) {
                            Button(
                                onClick = { },
                                enabled = false,
                                colors = ButtonDefaults.buttonColors(disabledContainerColor = Color.Transparent),
                                modifier = Modifier.height(32.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                            ) {
                                Text("-", fontSize = 12.sp, color = Color.DarkGray)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}
