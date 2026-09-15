# Ola S1 Pro BLE Protocol Companion & Analysis

An open-source Android application and reverse-engineering journey into the Bluetooth Low Energy (BLE) protocol of the **Ola S1 Pro (MoveOS 5.0.0)** electric scooter. 

This repository documents the attempt to build a third-party companion app to read vehicle telemetry (Battery, Range, Odometer) and control the vehicle, ultimately culminating in the discovery of the scooter's highly secure AES-encrypted Challenge-Response protocol.

---

## 🚀 The Journey: Reverse-Engineering the Ola S1 Pro

### 1. Bluetooth Service Discovery
The Ola S1 Pro broadcasts multiple Bluetooth profiles:
- **Classic Bluetooth (HID / A2DP)**: Used for media, calls, and standard OS media controls.
- **BLE GATT (`OLAS1`)**: Used by the official Ola Electric app for lock/unlock and telemetry.

Upon connecting to the `OLAS1` peripheral, we discovered a Custom UART Service (`6e400001-b5a3-f393-e0a9-e50e24dcca9e`) with two key characteristics:
- **TX Channel (`...0002`)**: Pushes data from the scooter to the phone.
- **RX Channel (`...0003`)**: Receives commands from the phone.

### 2. The Unencrypted Heartbeat
By subscribing to the TX channel, we intercepted a continuous stream of raw Hex packets broadcasted by the scooter every few seconds. We successfully cracked the core framing protocol:

```text
13 06 6A A9 A7 09 01 02 20 00 6B 00 27 01 F4 01 00 01 1E E2
```
- **Byte 0 (`13`)**: Payload length (19 bytes).
- **Byte 1 (`06`)**: Rolling Sequence Number to prevent packet loss.
- **Bytes 2-5 (`6A A9 A7 09`)**: Big-Endian 32-bit UNIX Timestamp.
- **Bytes 18-19 (`1E E2`)**: Cryptographic CRC-16 Checksum.

However, despite parsing these packets, the physical dashboard numbers (e.g., 100% Battery, 135km Range, 83,991km Odometer) were completely absent from the payload. The static `01 02` message was simply a **Keep-Alive Heartbeat**.

### 3. The Cryptographic Wall
To find the exact command to request the battery state, we captured an **Android Bluetooth HCI Snoop Log** while operating the Official Ola Electric App. 

We wrote a custom BTSnoop parser and extracted the exact packet sequence sent from the phone to the scooter's RX channel:

1. **The Handshake**: `13 43 6A A9 AC ... 31 30 32 41 45 ...` -> Transmits the partial Device MAC address in plaintext ASCII (`102AE0DA1`).
2. **The Encrypted Payload**: `12 46 6A A9 AC 24 E5 DC 9F 0D F3 6F F8 3B 50 11 01 F3 5B` -> The payload switches to a highly-entropic, randomized stream of bytes. 

**Conclusion**: The Ola S1 Pro uses an **AES-Encrypted Challenge-Response Protocol**. The scooter will *only* broadcast vehicle telemetry or accept Lock/Unlock commands if it receives a valid encrypted request. The encryption keys are securely generated and stored inside the Android Keystore by the official Ola app.

### 💡 Lessons Learned
- **Security is paramount**: Ola Electric has implemented a highly robust, encrypted communication channel. It is **cryptographically impossible** for a third-party app to independently control the scooter or read telemetry without root access to steal the private keys.
- **Protection against theft**: This architecture makes the scooter highly resilient against Bluetooth relay attacks and digital spoofing.
- **GATT Architecture**: The complete absence of standard BLE services (like the `180F` Battery Service) confirms Ola's strategy of funneling all data exclusively through their proprietary, encrypted UART tunnel.

---

## 📱 The Companion App
Even though independent control is blocked by encryption, we built a fully functional BLE Diagnostic tool for Android.

### Features
- **BLE Scanner**: Filters and identifies the `OLAS1` peripheral.
- **GATT Client**: Connects to the scooter and performs service discovery.
- **Notification Subscriptions**: Hooks into the Custom UART TX channel to read incoming packets.
- **Diagnostic Dashboard**: Displays real-time streaming Hex data and translates payloads.
- **Protocol Logger**: Automatically logs all BLE events (Connect, Disconnect, Notify, Write) with high-precision timestamps to a structured `ProtocolObservation.txt` file.
- **Export System**: Uses Android's `ActivityResultContracts.CreateDocument` to seamlessly export raw logs directly to Google Drive or local storage.

### Tech Stack
- **Language**: Kotlin
- **UI Toolkit**: Jetpack Compose (Material 3)
- **Architecture**: Reactive UI with `StateFlow` and Coroutines.
- **Permissions**: Fully handles modern Android 12+ `BLUETOOTH_SCAN` and `BLUETOOTH_CONNECT` runtime permissions.

---

## 🛠️ Build Instructions
1. Clone the repository.
2. Open the project in Android Studio.
3. Build and run the app on a physical Android device (BLE cannot be tested on an emulator).
4. Ensure Location and Bluetooth permissions are granted.

*Note: This project is strictly for diagnostic and educational purposes. Always use the Official Ola Electric application to control your vehicle.*
