# SMS Gateway (Android, Java)

Simple Android app to send OTP or custom SMS messages from your phone.

## Features
- Manual SMS sending from app UI
- Generate 6-digit OTP message automatically
- Embedded HTTP API server on device (`POST /send`)
- API key protection with `X-API-Key` header
- Supports long messages via multipart SMS

## Project structure
- `app/src/main/java/com/example/smsgateway/MainActivity.java`
- `app/src/main/java/com/example/smsgateway/SmsGatewayServer.java`
- `app/src/main/res/layout/activity_main.xml`
- `app/src/main/AndroidManifest.xml`

## Open and run
1. Open this folder in Android Studio.
2. Let Android Studio sync Gradle.
3. Connect an Android phone with SIM and SMS capability.
4. Run app and grant SMS permission.
5. Enter API key and port, then tap **Start API Server**.

## API
Endpoint:
- `POST http://<phone-ip>:8080/send`

Headers:
- `Content-Type: application/json`
- `X-API-Key: <your-api-key>`

Body (custom message):
```json
{
  "phone": "+97695591155",
  "message": "Hello from gateway"
}
```

Body (OTP message):
```json
{
  "phone": "+12025550123",
  "otp": true
}
```

Response:
```json
{
  "ok": true,
  "message": "SMS sent"
}
```

cURL example:
```bash
curl -X POST "http://<phone-ip>:8080/send" \
  -H "Content-Type: application/json" \
  -H "X-API-Key: change-this-api-key" \
  -d '{"phone":"+12025550123","otp":true}'
```

## Notes
- `gradle-wrapper.jar` and `gradlew` scripts are not included yet. Android Studio can still import and generate required files when syncing.
- Keep phone and caller system on reachable network.
- For production use, keep a strong API key and avoid exposing this endpoint to public internet.
