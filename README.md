# BTTH1 - Walkie-Talkie Offline (Wi-Fi Direct)

Android mini project implemented with Java + XML.

## Features

- Discover nearby devices using Wi-Fi Direct.
- Connect peer-to-peer without Internet/router.
- Auto role split: Group Owner (server) / Client.
- Push-to-talk voice streaming over local socket (`port 8888`).
- Lifecycle cleanup for receiver, socket, and audio threads.

## Main source layout

- `app/src/main/java/com/example/btth1/MainActivity.java`
- `app/src/main/java/com/example/btth1/wifi/WiFiDirectBroadcastReceiver.java`
- `app/src/main/java/com/example/btth1/network/ServerClass.java`
- `app/src/main/java/com/example/btth1/network/ClientClass.java`
- `app/src/main/java/com/example/btth1/audio/Constants.java`
- `app/src/main/java/com/example/btth1/audio/SendAudioThread.java`
- `app/src/main/java/com/example/btth1/audio/ReceiveAudioThread.java`
- `app/src/main/java/com/example/btth1/adapter/PeerListAdapter.java`

## Quick check

Run from repo root:

```powershell
.\gradlew.bat :app:build
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:lintDebug
```

## Manual test flow (2 devices)

1. Install debug APK on both devices and grant requested permissions.
2. Open app on both devices.
3. Tap **Discover Devices** on one/both devices.
4. Tap a peer from the list to connect.
5. Hold **Hold To Talk** to send voice and release to stop.

## Notes

- Android 13+ needs `NEARBY_WIFI_DEVICES`.
- Android 12 and below uses `ACCESS_FINE_LOCATION` for discovery.
- Audio uses PCM 16-bit, mono, 16kHz.

