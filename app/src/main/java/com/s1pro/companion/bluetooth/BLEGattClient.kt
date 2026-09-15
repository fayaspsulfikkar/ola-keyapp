package com.s1pro.companion.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothProfile
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    DISCOVERING_SERVICES,
    READY
}

@SuppressLint("MissingPermission")
class BLEGattClient(private val context: Context) {
    private var bluetoothGatt: BluetoothGatt? = null
    private var currentDevice: BluetoothDevice? = null
    private var isIntentionalDisconnect = false
    
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _discoveredServices = MutableStateFlow<List<android.bluetooth.BluetoothGattService>>(emptyList())
    val discoveredServices: StateFlow<List<android.bluetooth.BluetoothGattService>> = _discoveredServices.asStateFlow()

    private val _readResults = MutableStateFlow<Map<String, ByteArray>>(emptyMap())
    val readResults: StateFlow<Map<String, ByteArray>> = _readResults.asStateFlow()

    private val _notificationResults = MutableStateFlow<Map<String, ByteArray>>(emptyMap())
    val notificationResults: StateFlow<Map<String, ByteArray>> = _notificationResults.asStateFlow()

    private val protocolLogger = ProtocolLogger(context)

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    _connectionState.value = ConnectionState.CONNECTED
                    logToFile("Connected to GATT Server. Starting service discovery...")
                    protocolLogger.logEvent("phone -> scooter", "N/A", "N/A", "connect", null, status)
                    _connectionState.value = ConnectionState.DISCOVERING_SERVICES
                    gatt.discoverServices()
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    _connectionState.value = ConnectionState.DISCONNECTED
                    logToFile("Disconnected from GATT Server safely.")
                    protocolLogger.logEvent("phone -> scooter", "N/A", "N/A", "disconnect", null, status)
                    gatt.close()
                    bluetoothGatt = null
                }
            } else {
                _connectionState.value = ConnectionState.DISCONNECTED
                logToFile("GATT Error status $status. Disconnected.")
                protocolLogger.logEvent("phone -> scooter", "N/A", "N/A", "disconnect_error", null, status)
                gatt.close()
                bluetoothGatt = null
                
                if (!isIntentionalDisconnect && currentDevice != null) {
                    logToFile("Unexpected disconnect. Attempting auto-reconnect...")
                    connect(currentDevice!!.address)
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                _discoveredServices.value = gatt.services
                _connectionState.value = ConnectionState.READY
                logToFile("Services discovered successfully!")
                
                val builder = StringBuilder()
                builder.append("\n=== DEVICE: ${gatt.device.address} ===\n")
                gatt.services.forEach { service ->
                    builder.append("Service UUID: ${service.uuid}\n")
                    service.characteristics.forEach { char ->
                        builder.append("  |- Characteristic UUID: ${char.uuid}\n")
                        builder.append("     |- Properties: ${char.properties}\n")
                    }
                }
                builder.append("=========================================\n")
                logToFile(builder.toString())
            } else {
                logToFile("Service discovery failed with status: $status")
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: android.bluetooth.BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int
        ) {
            handleCharacteristicRead(characteristic.service.uuid.toString(), characteristic.uuid.toString(), value, status)
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: android.bluetooth.BluetoothGattCharacteristic,
            status: Int
        ) {
            handleCharacteristicRead(characteristic.service.uuid.toString(), characteristic.uuid.toString(), characteristic.value ?: ByteArray(0), status)
        }

        private fun handleCharacteristicRead(serviceUuid: String, uuid: String, value: ByteArray, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val hexValue = value.joinToString("") { "%02X ".format(it) }
                logToFile("READ success for $uuid: $hexValue")
                protocolLogger.logEvent("scooter -> phone", serviceUuid, uuid, "read", value, status)
                
                val currentMap = _readResults.value.toMutableMap()
                currentMap[uuid] = value
                _readResults.value = currentMap
            } else {
                logToFile("READ failed for $uuid with status $status")
                protocolLogger.logEvent("scooter -> phone", serviceUuid, uuid, "read_fail", value, status)
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: android.bluetooth.BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            handleCharacteristicChanged(characteristic.service.uuid.toString(), characteristic.uuid.toString(), value)
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: android.bluetooth.BluetoothGattCharacteristic
        ) {
            handleCharacteristicChanged(characteristic.service.uuid.toString(), characteristic.uuid.toString(), characteristic.value ?: ByteArray(0))
        }

        private fun handleCharacteristicChanged(serviceUuid: String, uuid: String, value: ByteArray) {
            protocolLogger.logEvent("scooter -> phone", serviceUuid, uuid, "notification", value)
            
            val currentMap = _notificationResults.value.toMutableMap()
            currentMap[uuid] = value
            _notificationResults.value = currentMap
        }
    }

    @SuppressLint("MissingPermission")
    fun subscribeToCharacteristic(serviceUuid: java.util.UUID, charUuid: java.util.UUID) {
        val service = bluetoothGatt?.getService(serviceUuid)
        val characteristic = service?.getCharacteristic(charUuid)
        if (characteristic != null) {
            logToFile("Requesting SUBSCRIBE for characteristic $charUuid...")
            bluetoothGatt?.setCharacteristicNotification(characteristic, true)
            
            val descriptor = characteristic.getDescriptor(java.util.UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
            if (descriptor != null) {
                val properties = characteristic.properties
                val value = if ((properties and android.bluetooth.BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
                    android.bluetooth.BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                } else if ((properties and android.bluetooth.BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0) {
                    android.bluetooth.BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
                } else {
                    null
                }
                
                if (value != null) {
                    descriptor.value = value
                    bluetoothGatt?.writeDescriptor(descriptor)
                    logToFile("Wrote CCCD descriptor to enable notifications/indications.")
                }
            } else {
                logToFile("SUBSCRIBE failed: CCCD Descriptor 2902 not found on $charUuid")
            }
        } else {
            logToFile("SUBSCRIBE failed: Characteristic $charUuid not found.")
        }
    }

    fun readCharacteristic(serviceUuid: java.util.UUID, charUuid: java.util.UUID) {
        val service = bluetoothGatt?.getService(serviceUuid)
        val characteristic = service?.getCharacteristic(charUuid)
        if (characteristic != null) {
            logToFile("Requesting READ for characteristic $charUuid...")
            try {
                bluetoothGatt?.readCharacteristic(characteristic)
            } catch (e: SecurityException) {
                logToFile("READ failed: Blocked by Android SecurityException (Likely a protected HID service).")
                val currentMap = _readResults.value.toMutableMap()
                currentMap[charUuid.toString()] = "BLOCKED BY OS".toByteArray()
                _readResults.value = currentMap
            } catch (e: Exception) {
                logToFile("READ failed with exception: ${e.message}")
            }
        } else {
            logToFile("READ failed: Characteristic $charUuid not found.")
        }
    }

    fun connect(address: String) {
        if (_connectionState.value == ConnectionState.CONNECTING || _connectionState.value == ConnectionState.READY) {
            return
        }
        
        isIntentionalDisconnect = false
        
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as android.bluetooth.BluetoothManager
        val device = bluetoothManager.adapter.getRemoteDevice(address)
        currentDevice = device
        
        _connectionState.value = ConnectionState.CONNECTING
        logToFile("Initiating connection to $address...")
        
        bluetoothGatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
    }

    fun disconnect() {
        isIntentionalDisconnect = true
        bluetoothGatt?.disconnect()
    }

    private fun logToFile(message: String) {
        try {
            val logDir = context.getExternalFilesDir(null)
            if (logDir != null) {
                if (!logDir.exists()) logDir.mkdirs()
                val logFile = File(logDir, "BLE_GATT_LOG.md")
                val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
                logFile.appendText("[$timestamp] $message\n")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
