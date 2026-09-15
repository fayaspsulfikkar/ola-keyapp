# S1 Pro Companion — Project Plan

## Overview
This plan outlines the steps to build an independent Android companion app for the Ola S1 Pro 1st Generation, focusing on discovery, observation, and safe implementation.

## Phases

### Phase 0: Development Environment (Current)
- Inspect environment tools (Java, Android SDK, Gradle, ADB).
- Status: Initial inspection complete. Installing Android Studio and creating project skeleton.

### Phase 1: Basic Android Project
- Create a minimal native Kotlin Android application with Jetpack Compose.
- Build and deploy to the physical phone.

### Phase 2: Bluetooth Permissions
- Implement modern Android Bluetooth permission handling (`BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT`).
- Safely handle permission denials.

### Phase 3: BLE Scanner
- Implement a read-only BLE scanner to detect nearby devices.

### Phase 4: Identify the Actual Scooter
- Allow the user to select the S1 Pro from the list of scanned devices.

### Phase 5: GATT Connection
- Establish a GATT connection with the selected scooter.
- Export GATT diagnostic reports without attempting arbitrary writes.

### Phase 6-8: Characteristic Inspection & Protocol Observation
- Inspect readable characteristics.
- Subscribe to notifications.
- Build a structured BLE logging system.

### Phase 9-10: Observation & Protocol Analysis
- Run controlled actions via the official app while capturing observations.
- Analyze patterns without guessing commands.

### Phase 11-12: Vehicle State & Read-Only Dashboard
- Create a `VehicleState` model based on confirmed facts.
- Build a dark, premium dashboard displaying battery, range, and lock state.

### Phase 13-14: Lock/Unlock Investigation & Control Security
- Safely investigate control characteristics.
- Implement biometric authentication before sending confirmed commands.

### Phase 15-17: Widgets & Proximity Features
- Build an Android home-screen widget.
- Implement safe Quick Settings tiles.
- Investigate proximity detection.

### Phase 18-22: Polish, Testing & Error Handling
- Enforce offline-first design.
- Complete automated and physical-device tests.
- Build developer diagnostics screens and finalize UI.

### Phase 23-25: Release
- Ensure comprehensive documentation.
- Commit all changes cleanly via Git.
- Build and verify the release APK.
