package com.s1pro.companion.bluetooth

import android.content.Context
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProtocolLogger(private val context: Context) {

    fun logEvent(
        direction: String,
        serviceUuid: String,
        characteristicUuid: String,
        operation: String,
        payload: ByteArray? = null,
        status: Int? = null
    ) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
        val hexPayload = payload?.joinToString("") { "%02X ".format(it) }?.trim() ?: "N/A"
        val length = payload?.size ?: 0
        val statusStr = status?.toString() ?: "N/A"
        
        val serviceShort = serviceUuid.split("-").firstOrNull() ?: serviceUuid
        val charShort = characteristicUuid.split("-").firstOrNull() ?: characteristicUuid

        val logEntry = "$timestamp | $direction | Srv: $serviceShort | Char: $charShort | Op: $operation | Len: $length | Status: $statusStr | Payload: $hexPayload\n"

        try {
            val logDir = context.getExternalFilesDir(null)
            if (logDir != null) {
                if (!logDir.exists()) logDir.mkdirs()
                val obsFile = File(logDir, "ProtocolObservation.txt")
                obsFile.appendText(logEntry)
                Log.d("ProtocolLogger", "Logged: $logEntry")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun clearLogs() {
        try {
            val logDir = context.getExternalFilesDir(null)
            if (logDir != null) {
                val obsFile = File(logDir, "ProtocolObservation.txt")
                if (obsFile.exists()) {
                    obsFile.delete()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
