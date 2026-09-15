# S1 Pro Companion — Antigravity Project Brief

## Project Overview

**Project name:** S1 Pro Companion  
**Platform:** Android  
**Language:** Kotlin  
**Primary goal:** Build an independent Android companion app for an Ola S1 Pro 1st Generation, with a cleaner UI, useful widgets, diagnostics, and—only if technically confirmed—Bluetooth vehicle controls.

## Vehicle Information

- Vehicle: Ola S1 Pro 1st Generation
- Dash / MoveOS version: 5.0.0
- Official Ola Electric Android app: v5.4.1 (1344)
- Testing device: Physical Android phone
- Developer Options: Enabled
- USB debugging: Enabled
- Phone can remain connected to the scooter through Bluetooth during testing.

---

# Critical Project Rules

This project must be developed incrementally.

Do **not** attempt to build the complete application at once.

For every phase:

1. Define the objective.
2. Inspect the current project.
3. Make a short implementation plan.
4. Implement the smallest useful change.
5. Build the project.
6. Run relevant tests.
7. Install/test on the physical Android phone when appropriate.
8. Document the result.
9. Report what passed, failed, and remains unknown.
10. Only then continue to the next phase.

## Never invent the scooter protocol

Do not guess:

- BLE service UUIDs
- Characteristic UUIDs
- Command formats
- Encryption keys
- Authentication mechanisms
- Lock/unlock packets
- Vehicle-state values

All Ola-specific protocol information must come from evidence collected from the actual scooter, phone, or legitimate technical documentation.

Do not assume information found online applies to this exact MoveOS 5.0.0 scooter.

---

# Safety Rules

This is a physical vehicle.

Treat lock, unlock, boot, and other vehicle-control operations as safety-sensitive.

During protocol investigation:

- Do not send arbitrary BLE writes to unknown characteristics.
- Do not guess command bytes.
- Do not repeatedly write random values.
- Do not attempt firmware modification.
- Do not attempt ECU modification.
- Do not attempt to bypass immobilizer/security systems.
- Do not disable vehicle safety systems.
- Do not modify vehicle firmware.
- Do not interfere with riding functionality.
- Do not send control commands while the scooter is being ridden.
- Never assume a writable characteristic is safe to write.
- Prefer read-only discovery first.
- Require explicit user confirmation before testing a newly discovered control command.

Only test against the owner's scooter.

Keep the official Ola application installed as a fallback.

---

# Target Features

The final application may include the following, subject to technical feasibility:

1. Connect to scooter
2. Disconnect from scooter
3. Bluetooth connection state
4. Battery percentage
5. Estimated range
6. Charging state
7. Odometer / distance
8. Lock state
9. Lock
10. Unlock
11. Boot/trunk control where supported
12. Other safe vehicle-status functions discovered through investigation
13. Android home-screen widget
14. Quick access / Quick Settings
15. Notification status
16. Biometric authentication
17. Offline-first Bluetooth operation
18. Developer diagnostics
19. BLE/GATT inspection
20. Exportable diagnostic logs

Do not assume all features are possible.

First determine what the physical scooter actually exposes through Bluetooth.

---

# Technology

Use:

- Kotlin
- Native Android APIs
- Android Studio-compatible Gradle project
- Jetpack Compose
- Android Bluetooth LE APIs
- Kotlin Coroutines
- Flow / StateFlow where useful
- ViewModel
- DataStore or Room only when necessary
- Android App Widgets / Glance where appropriate
- Android Biometric APIs where appropriate

Avoid unnecessary dependencies.

Keep the application lightweight.

Before selecting versions, inspect the locally installed:

- JDK
- Android SDK
- Build Tools
- Gradle
- Android Gradle Plugin

Do not unnecessarily upgrade the development environment.

---

# Suggested Architecture

Use a clean but practical architecture.

```text
app/
└── src/
    └── main/
        └── java/.../
            ├── ui/
            ├── bluetooth/
            ├── vehicle/
            ├── security/
            ├── widget/
            ├── data/
            └── domain/
```

## bluetooth/

Responsible for:

- BLE scanning
- BLE connection
- GATT discovery
- Reading characteristics
- Notifications / indications
- Connection state
- Reconnection
- BLE diagnostics
- BLE logging

## vehicle/

Responsible for:

- Vehicle abstraction
- Vehicle state
- Battery
- Range
- Charging
- Odometer
- Lock state
- Boot state

## security/

Responsible for:

- Biometric authentication
- Sensitive-action authorization
- Confirmation flows

## ui/

Responsible for:

- Dashboard
- Connection screen
- Vehicle information
- Diagnostics
- Settings
- Logs

## widget/

Responsible for:

- Home-screen widget
- Vehicle status
- Safe quick actions

## data/

Responsible for:

- Local persistence
- User preferences
- Discovered-device information

## domain/

Responsible for:

- Use cases
- Interfaces
- Domain models

Do not over-engineer the first prototype.

---

# PHASE 0 — Development Environment

Before significant coding:

1. Inspect the operating system.
2. Inspect JDK version.
3. Inspect Android SDK versions.
4. Inspect Android Build Tools.
5. Inspect Gradle.
6. Check ADB.
7. Detect connected Android devices.
8. Determine the phone's Android version.
9. Verify that the phone is visible through ADB.
10. Do not install large components unless necessary.

Create:

```text
DEVELOPMENT.md
PROJECT_PLAN.md
```

Do not begin BLE protocol investigation yet.

### First task output

Report:

1. Detected OS
2. JDK version
3. Android SDK versions
4. Gradle version
5. ADB status
6. Connected Android device
7. Android version
8. Available Build Tools
9. Recommended project configuration

Wait for user confirmation after the environment and project skeleton are ready.

---

# PHASE 1 — Basic Android Project

Create a minimal native Kotlin Android application.

Requirements:

- Builds successfully.
- Installs on the physical phone.
- Launches successfully.
- Uses Jetpack Compose.
- Uses a clean dark interface.
- Application name: `S1 Pro Companion`

Initial UI:

```text
S1 PRO COMPANION

Bluetooth
● Checking...

Scooter
Not connected

[ Scan for Scooter ]

Diagnostics
[ BLE Scanner ]

Settings
```

Do not implement scooter control.

Build and install on the physical phone.

---

# PHASE 2 — Bluetooth Permissions

Implement modern Android Bluetooth permission handling.

Likely permissions include:

- `BLUETOOTH_SCAN`
- `BLUETOOTH_CONNECT`

Use the correct runtime permission flow for the Android version being targeted.

Do not request unnecessary permissions.

Handle:

- Bluetooth disabled
- Permission denied
- Permission permanently denied
- Bluetooth unavailable
- Unsupported device

Do not proceed until scanning works.

---

# PHASE 3 — BLE Scanner

Implement a safe BLE scanner.

Display:

- Device name
- Address where Android permits access
- RSSI
- Advertised service UUIDs where available
- Manufacturer data where available

Example:

```text
BLE DEVICES

OLA / UNKNOWN
RSSI: -54 dBm
Services: ...

[ CONNECT ]

Other Device
RSSI: -72 dBm
```

Do not automatically connect to devices.

Do not write anything.

Add logging.

Create:

```text
BLE_SCAN_LOG.md
```

---

# PHASE 4 — Identify the Actual Scooter

The user will identify which discovered device corresponds to the S1 Pro.

Do not automatically assume a device is the scooter based only on its name.

Allow the user to select the device.

Store the selected device identifier appropriately.

Display:

```text
Selected scooter:
<device information>

Connection:
Disconnected

[ Connect ]
```

---

# PHASE 5 — GATT Connection

Implement BLE GATT connection.

Requirements:

- Connect
- Disconnect
- Connection state
- Timeout handling
- Reconnection handling
- Lifecycle-safe connection management
- Clean resource release

After connecting:

1. Discover services.
2. Do not write to characteristics.

Display:

```text
S1 PRO

Connected

GATT SERVICES

Service:
UUID

Characteristic:
UUID
Properties:
READ
WRITE
NOTIFY
```

Create an exportable GATT diagnostic report.

Possible filename:

```text
gatt_snapshot_<timestamp>.json
```

---

# PHASE 6 — Characteristic Inspection

Build a diagnostic interface.

For every characteristic show:

- Service UUID
- Characteristic UUID
- Properties
- Descriptor UUIDs
- Read capability
- Write capability
- Notification capability

Allow READ only on characteristics explicitly marked readable.

Do not expose arbitrary WRITE functionality in the normal UI.

Keep writes disabled by default.

Every operation must be logged.

---

# PHASE 7 — Notification Observation

Investigate characteristics supporting notifications/indications.

Subscribe only where technically appropriate.

Record:

```text
timestamp
direction
service UUID
characteristic UUID
payload length
payload bytes
connection state
```

Do not interpret bytes yet.

Example:

```text
2026-09-15T...
Characteristic: <UUID>
Notification:
AA 01 7F 00 ...
```

Keep raw data unchanged.

---

# PHASE 8 — Protocol Observation System

Create structured BLE logging.

Every observed event should contain:

```text
timestamp
direction:
  phone -> scooter
  scooter -> phone

service UUID
characteristic UUID

operation:
  connect
  disconnect
  read
  notification
  write

payload:
  hexadecimal

payload length
result/status
```

Make logs exportable.

Suggested files:

```text
ProtocolObservation.json
ProtocolObservation.txt
```

Do not automatically interpret unknown packets.

---

# PHASE 9 — Observe the Official Ola App

The user will perform controlled actions using the official Ola Electric application.

Potential observations:

1. Open official app.
2. Connect to scooter.
3. View vehicle status.
4. Lock scooter.
5. Unlock scooter.
6. Open boot/trunk if available.

Important:

Do not assume that the custom application can automatically observe another application's encrypted BLE traffic.

If packet capture, Android debugging, a BLE sniffer, or another legitimate diagnostic method is required, explain exactly what is needed.

Do not claim that ordinary application-level BLE logs reveal traffic generated by another application.

---

# PHASE 10 — Protocol Analysis

After collecting observations, analyze patterns.

Look for:

- Repeated UUIDs
- Read operations
- Notifications
- State changes
- Command/response relationships
- Challenge/response patterns
- Timestamp relationships
- Payload lengths
- Repeated prefixes
- Counters
- Nonces
- Checksums
- Authentication structures
- Encrypted payloads

Classify findings:

### FACT

Directly observed.

### INFERENCE

Strongly supported by observations.

### HYPOTHESIS

Plausible but unconfirmed.

### UNKNOWN

Insufficient evidence.

Never convert a hypothesis into production code as if it were confirmed.

---

# PHASE 11 — Vehicle State Model

Create:

```text
VehicleState
```

Possible fields:

```text
connectionState
batteryPercentage
rangeKm
charging
lockState
bootState
odometerKm
lastUpdated
signalStrength
```

Every field must have a source.

Example:

```text
batteryPercentage:
source = BLE characteristic X
status = CONFIRMED

lockState:
source = unknown
status = NOT_IMPLEMENTED
```

Do not fabricate values.

---

# PHASE 12 — Read-Only Dashboard

Once confirmed read/notification data exists, create the main dashboard.

Example:

```text
S1 PRO

● Connected

73%
Battery

92 km
Estimated Range

Not Charging

83,949 km
Distance

LOCKED
```

Use:

- Dark interface
- Minimal design
- Large readable information
- Clear connection state
- Clear stale-data indication

Do not copy Ola's exact proprietary UI or branding.

---

# PHASE 13 — Lock/Unlock Investigation

Only begin this phase after the BLE protocol has been understood sufficiently.

Determine:

1. Which characteristic is involved.
2. Whether authentication exists.
3. Whether the command is encrypted.
4. Whether a session/key is required.
5. Whether the command changes between sessions.
6. Whether a response confirms success/failure.
7. Whether the official app communicates through the same BLE interface.

Never guess the command.

If the protocol cannot be safely reproduced, stop and explain why.

If a sufficiently confirmed protocol exists, implement:

```text
VehicleControlRepository

lock()
unlock()
```

Do not expose arbitrary BLE writes.

---

# PHASE 14 — Control Security

Lock/unlock must require deliberate user action.

Recommended unlock flow:

```text
UNLOCK
   ↓
Biometric authentication
   ↓
Confirmation
   ↓
BLE command
   ↓
Confirmation response
   ↓
Update UI
```

Example:

```text
Unlock S1 Pro?

[ Cancel ] [ Unlock ]
```

Do not implement accidental unlock through a simple widget tap.

---

# PHASE 15 — Home-Screen Widget

Create an Android home-screen widget after core functionality is stable.

Example:

```text
S1 PRO
● Connected

73%     92 km

LOCKED

[ LOCK ]
[ UNLOCK ]
```

Widget requirements:

- Show current state
- Show connection state
- Refresh safely
- Indicate stale data
- Require authentication for sensitive actions
- Never send arbitrary BLE commands

If a widget cannot safely maintain a BLE connection, route the action through the main application.

---

# PHASE 16 — Quick Settings / Notification

Investigate:

- Quick Settings tile
- Notification status

Possible tile:

```text
S1 Pro
LOCKED
```

Tapping should open the secure control flow.

Do not automatically unlock merely because a tile was pressed.

---

# PHASE 17 — Proximity Features

Only after basic functionality works.

Start with detection:

```text
S1 Pro detected nearby
```

Do not initially implement automatic unlocking.

A later feature could be:

```text
S1 Pro detected

[ Open Companion ]
```

Only consider automated control after extensive testing and safety review.

---

# PHASE 18 — Offline-First Design

Bluetooth functionality that genuinely works without internet should remain available offline.

Separate:

```text
LOCAL BLUETOOTH FEATURES

Lock
Unlock
Battery
Connection
Vehicle status
```

from:

```text
OPTIONAL INTERNET FEATURES

Cloud telemetry
Maps
Account information
Remote services
```

Do not assume Ola cloud APIs exist or are accessible.

---

# PHASE 19 — Error Handling

Handle:

- Scooter unavailable
- Bluetooth disabled
- Permission denied
- Connection timeout
- GATT errors
- Authentication failure
- Scooter connected to another phone
- Scooter disconnected
- App killed
- Phone Bluetooth reset
- Scooter powered down
- Unsupported protocol
- Stale state

Normal users should see understandable messages.

Example:

```text
Unable to connect to S1 Pro.

Make sure the scooter is nearby
and Bluetooth is enabled.
```

Keep raw technical errors in diagnostic logs.

---

# PHASE 20 — Testing

Create automated tests for:

- BLE state machine
- Vehicle-state parsing
- Payload parser
- Repository
- Authentication flow
- Widget state
- Error handling

Create physical-device tests for:

1. Bluetooth disabled
2. Bluetooth enabled
3. Scooter nearby
4. Scooter unavailable
5. App connected
6. App disconnected
7. Phone restart
8. Scooter restart
9. Permission revoked
10. Lock
11. Unlock
12. Widget
13. Biometric authentication
14. Network disabled
15. Multiple BLE devices nearby

Maintain:

```text
TEST_PLAN.md
```

For every test record:

```text
Test
Expected result
Actual result
Status
Notes
```

---

# PHASE 21 — Developer Diagnostics

Create a developer diagnostics screen.

Show:

```text
App version
Android version
Bluetooth state
Permission state
Connected device
RSSI
GATT state
Services
Characteristics
Last BLE operation
Last error
```

Provide:

```text
[ Export Diagnostics ]
```

Never log private keys, authentication secrets, or sensitive tokens.

---

# PHASE 22 — UI Polish

Once functionality is proven:

Style:

- Dark
- Minimal
- Premium
- Fast
- Large readable status
- Few buttons
- Clear connection state
- Clear battery state

Prioritize:

1. Connection
2. Battery
3. Range
4. Lock state
5. Important actions

Avoid unnecessary animations.

Do not copy Ola's proprietary branding.

---

# PHASE 23 — Documentation

Maintain:

```text
README.md
ARCHITECTURE.md
DEVELOPMENT.md
BLE_DISCOVERY.md
GATT_SNAPSHOT.md
PROTOCOL_OBSERVATIONS.md
SECURITY.md
TEST_PLAN.md
CHANGELOG.md
PROJECT_PLAN.md
```

Documentation must distinguish confirmed facts from assumptions.

---

# PHASE 24 — Git

Initialize Git.

Use meaningful commits.

Examples:

```text
initial Android project
add BLE permission handling
add BLE scanner
add GATT discovery
add diagnostic logging
add vehicle state model
add dashboard
add confirmed vehicle control
add widget
add biometric security
```

Do not create giant commits containing unrelated changes.

---

# PHASE 25 — Release Build

Only after all functionality is tested:

1. Create a release APK.
2. Verify build succeeds.
3. Install APK on the physical phone.
4. Verify app launches.
5. Verify BLE permissions.
6. Verify scooter connection.
7. Verify read-only functions.
8. Verify control functions only if protocol is confirmed.
9. Verify widget.
10. Verify biometric authentication.
11. Remove/disable debug-only controls.
12. Ensure no arbitrary BLE write interface remains exposed.

---

# Agent Operating Procedure

For every task:

1. Explain the objective.
2. Inspect the current project.
3. Create a short implementation plan.
4. Implement the smallest useful change.
5. Build.
6. Run tests.
7. Install on the physical device when appropriate.
8. Verify.
9. Update documentation.
10. Report:
   - What changed
   - What was tested
   - What passed
   - What failed
   - What remains unknown
   - Recommended next step

Do not silently skip failed tests.

If physical-device interaction is required, tell the user exactly what they must do.

Do not continue past a failed milestone unless the failure is understood.

---

# FIRST TASK FOR ANTIGRAVITY

DO NOT START IMPLEMENTING THE COMPLETE APPLICATION.

First inspect the development environment.

Then create a project plan.

Then report:

1. Detected OS
2. JDK version
3. Android SDK version(s)
4. Gradle version
5. ADB status
6. Connected Android device
7. Android version of the phone
8. Available Android Build Tools
9. Recommended project configuration

Then create:

```text
DEVELOPMENT.md
PROJECT_PLAN.md
```

Do not begin BLE protocol reverse engineering yet.

Wait for my confirmation after the environment and project skeleton are ready.

---

# Project Principle

The development sequence is:

```text
DISCOVER
   ↓
OBSERVE
   ↓
VERIFY
   ↓
UNDERSTAND
   ↓
IMPLEMENT
   ↓
TEST
   ↓
POLISH
```

Never:

```text
GUESS
   ↓
SEND UNKNOWN BLE COMMAND
   ↓
HOPE
```

The primary objective is to build a reliable, lightweight, independent companion application for the owner's S1 Pro while treating the scooter's Bluetooth protocol and physical controls carefully.
