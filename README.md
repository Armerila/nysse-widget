# Tram/Bus departures Widget for Android
A simple Android home screen widget that displays real-time tram and bus departure times using DigiTransit API

Currently has only one hard coded stop

### In the unlikely case someone wants to try this

#### API key
- Go to: https://portal-api.digitransit.fi/
- Register
- Subscribe to "Routing API"
- Copy your key

#### Add Your API Key to the Code
Open this file: `app/src/main/java/com/example/nyssewidget/DigitransitApiService.kt`

Edit this line:
```kotlin
private val API_KEY = "YOUR_API_KEY_HERE"
```
Or

Add this to your local.properties:
```kotlin
DIGITRANSIT_API_KEY=<YOUR_API_KEY_HERE>
```

#### Open and run in Android Studio
- Open Android Studio
- Navigate to and select the `NysseWidget` folder
- Sync gradle
- Run on emulator or with connected physical device (at your own risk!)