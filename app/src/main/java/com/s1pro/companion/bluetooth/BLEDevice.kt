package com.s1pro.companion.bluetooth

data class BLEDevice(
    val address: String,
    val name: String?,
    val rssi: Int,
    val serviceUuids: List<String>,
    val manufacturerData: String?
)
