package com.s1pro.companion.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@SuppressLint("MissingPermission")
class BLEScanner(private val context: Context) {
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val scanner = bluetoothManager.adapter?.bluetoothLeScanner
    
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val scanMap = mutableMapOf<String, BLEDevice>()
    private val _scannedDevices = MutableStateFlow<List<BLEDevice>>(emptyList())
    val scannedDevices: StateFlow<List<BLEDevice>> = _scannedDevices.asStateFlow()

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val record = result.scanRecord
            
            val name = record?.deviceName ?: device.name ?: "Unknown Device"
            val address = device.address
            val rssi = result.rssi
            val uuids = record?.serviceUuids?.map { it.uuid.toString() } ?: emptyList()
            
            val manufacturerData = record?.manufacturerSpecificData?.let { sparseArray ->
                if (sparseArray.size() > 0) {
                    val key = sparseArray.keyAt(0)
                    val value = sparseArray.valueAt(0)
                    "0x${Integer.toHexString(key).uppercase()}: ${value.joinToString("") { "%02X".format(it) }}"
                } else null
            }

            val bleDevice = BLEDevice(address, name, rssi, uuids, manufacturerData)
            
            scanMap[address] = bleDevice
            _scannedDevices.value = scanMap.values.sortedByDescending { it.rssi }
        }
    }
    
    fun startScan() {
        if (_isScanning.value || scanner == null) return
        scanMap.clear()
        _scannedDevices.value = emptyList()
        logToFile("Started BLE Scan (Filtered for S1 Pro: 6e401812-b5a3-f393-e0a9-84978b5f7c21)")
        
        val filter = android.bluetooth.le.ScanFilter.Builder()
            .setServiceUuid(android.os.ParcelUuid.fromString("6e401812-b5a3-f393-e0a9-84978b5f7c21"))
            .build()
            
        val settings = android.bluetooth.le.ScanSettings.Builder()
            .setScanMode(android.bluetooth.le.ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
            
        scanner.startScan(listOf(filter), settings, scanCallback)
        _isScanning.value = true
    }
    
    fun stopScan() {
        if (!_isScanning.value || scanner == null) return
        scanner.stopScan(scanCallback)
        logToFile("Stopped BLE Scan. Found ${scanMap.size} devices.")
        _isScanning.value = false
    }

    private fun logToFile(message: String) {
        try {
            val logDir = context.getExternalFilesDir(null)
            if (logDir != null) {
                if (!logDir.exists()) logDir.mkdirs()
                val logFile = File(logDir, "BLE_SCAN_LOG.md")
                val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
                logFile.appendText("[$timestamp] $message\n")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
