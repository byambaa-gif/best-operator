# Tsagadai SMS Gateway

Android SMS gateway that pulls queued OTP messages from the Tsagadai Render backend and sends them through the phone SIM.

## Reliable operation

- Runs as a foreground service with a permanent status notification.
- Polls the backend and sends a heartbeat every 20 seconds.
- Uses `START_STICKY` so Android can recreate the service after process termination.
- Restarts after phone reboot or app replacement when the gateway was previously enabled.
- Stores its backend URL, enabled state, device credentials, status, and latest delivery result.

## Install and start

1. Open the project in Android Studio and use the embedded JDK 17 or newer.
2. Install it on the Samsung.
3. Allow SMS and notification permissions.
4. Tap **Connect to Backend** once.
5. Confirm the permanent Tsagadai SMS Gateway notification says `ONLINE`.

The app screen may then be closed; do not press **Disconnect**.

## Required Samsung settings

1. Open **Settings > Apps > SMS Gateway > Battery** and select **Unrestricted** or enable background activity.
2. Open **Settings > Battery and device care > Battery > Background usage limits** and add the gateway to **Never sleeping apps**.
3. Keep the phone connected to power with Wi-Fi or mobile data enabled.
4. Maintain SIM balance and verify outgoing SMS remains available.

If the app is force-stopped in Android Settings, Android blocks automatic restart. Open it and tap **Connect to Backend** again.

## Build

```powershell
.\gradlew.bat assembleDebug
```
