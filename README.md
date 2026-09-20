# iTantra

**Indian Multilingual TTS & STT Aided Neural Transceiver Radio Access for Low Bitrate Links**

iTantra is an Android proof-of-concept (POC) focused on **offline Hindi speech-to-text (STT)** and **local text communication** between two phones over a LAN (Wi-Fi/hotspot) using WebSockets.

---

## Current POC Scope

This repository currently implements:

- Android app built with **Kotlin + Jetpack Compose**
- **MVVM-style** presentation flow (ViewModel + repository)
- **Offline Hindi STT** on-device (no cloud inference)
- **Vakyansh Hindi Wav2Vec2 ONNX** model inference via ONNX Runtime
- Push-to-talk capture using **AudioRecord**
- **16 kHz, mono, PCM16** audio processing
- **Text-only** message transmission over WebSocket
- Local communication over **same Wi-Fi/hotspot network**
- Same APK installed on both phones (host and join modes)

---

## Key Features

- **On-device Hindi STT:** Speech is transcribed locally on the sender phone.
- **No audio over network:** Only recognized text is sent.
- **Host/Join networking model:** One phone runs a WebSocket server, second phone connects as client.
- **Simple chat UI:** Transcribed and typed messages are displayed in chat bubbles.
- **No external backend required:** Works on local LAN.

---

## Architecture (Current POC)

```mermaid
flowchart LR
  subgraph A[Phone A (Same APK)]
    A1[Push-to-Talk] --> A2[Android AudioRecord]
    A2 --> A3[PCM16 16 kHz Mono Audio]
    A3 --> A4[Preprocessing\n(short→float, mean normalization)]
    A4 --> A5[Vakyansh Hindi Wav2Vec2 ONNX]
    A5 --> A6[ONNX Runtime (on-device)]
    A6 --> A7[Recognized Hindi Text]
    A7 --> A8[WebSocket Client/Server Side Sender]
  end

  A8 --> N[Local Wi-Fi / Hotspot LAN]
  N --> B1[WebSocket Server/Client on Phone B]

  subgraph B[Phone B (Same APK)]
    B1 --> B2[Communication Repository]
    B2 --> B3[TextMessage JSON Parsing]
    B3 --> B4[Chat UI Display]
  end

  note1["STT executes locally on sending phone"]
  note2["Only text crosses the network"]
  A6 -.-> note1
  N -.-> note2
```

### Communication path implemented

1. User holds Push-to-Talk on sender phone.
2. AudioRecord captures PCM16 audio at 16 kHz mono.
3. Local ONNX model inference converts speech to Hindi text.
4. `TextMessage` JSON is sent via WebSocket over local Wi-Fi/hotspot.
5. Receiver phone parses JSON and renders text in chat UI.

---

## Repository Architecture

```text
Itantra/
├── README.md
├── iTantra-POC.apk
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradlew
├── gradlew.bat
├── gradle/
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
└── app/
    ├── build.gradle.kts
    ├── proguard-rules.pro
    └── src/
        └── main/
            ├── AndroidManifest.xml
            ├── assets/
            │   ├── onnx/
            │   │   ├── vakyansh_hindi.onnx
            │   │   └── vakyansh_hindi.onnx.data
            │   └── vakyansh-hindi/
            │       └── vocab.json
            ├── java/com/itantara/app/
            │   ├── MainActivity.kt
            │   ├── data/
            │   │   ├── model/TextMessage.kt
            │   │   └── repository/CommunicationRepository.kt
            │   ├── network/
            │   │   ├── ConnectionState.kt
            │   │   ├── ITantraWebSocketClient.kt
            │   │   ├── ITantraWebSocketServer.kt
            │   │   └── LocalNetworkUtils.kt
            │   ├── presentation/
            │   │   ├── communication/
            │   │   │   ├── CommunicationScreen.kt
            │   │   │   └── CommunicationViewModel.kt
            │   │   ├── connection/
            │   │   │   ├── ConnectionScreen.kt
            │   │   │   └── ConnectionViewModel.kt
            │   │   ├── navigation/
            │   │   │   ├── NavGraph.kt
            │   │   │   └── Screen.kt
            │   │   ├── setup/SetupScreen.kt
            │   │   └── theme/Theme.kt
            │   └── speech/
            │       ├── SpeechToTextEngine.kt
            │       └── VakyanshOnnxSttEngine.kt
            └── res/
                ├── drawable/
                ├── mipmap-*/
                └── values/
```

### Important directories and files

| Path | Purpose |
|---|---|
| `/home/runner/work/Itantra/Itantra/app/` | Android application module |
| `/home/runner/work/Itantra/Itantra/app/src/main/java/com/itantara/app/presentation/` | UI screens, navigation, and ViewModels |
| `/home/runner/work/Itantra/Itantra/app/src/main/java/com/itantara/app/data/repository/CommunicationRepository.kt` | Connection state and message flow orchestration |
| `/home/runner/work/Itantra/Itantra/app/src/main/java/com/itantara/app/network/` | WebSocket server/client and local IP utility |
| `/home/runner/work/Itantra/Itantra/app/src/main/java/com/itantara/app/speech/` | Audio capture + ONNX STT pipeline |
| `/home/runner/work/Itantra/Itantra/app/src/main/assets/onnx/` | Hindi ONNX model + external data file |
| `/home/runner/work/Itantra/Itantra/app/src/main/assets/vakyansh-hindi/vocab.json` | Token vocabulary for decoding model output |
| `/home/runner/work/Itantra/Itantra/app/src/main/res/` | Android resources (icons, strings, themes) |
| `/home/runner/work/Itantra/Itantra/app/build.gradle.kts` | App dependencies and Android build config |
| `/home/runner/work/Itantra/Itantra/build.gradle.kts` + `/home/runner/work/Itantra/Itantra/settings.gradle.kts` | Root Gradle/plugin/module configuration |

---

## Technology Stack

| Layer | Technology |
|---|---|
| Platform | Android |
| Language | Kotlin |
| UI | Jetpack Compose (Material 3) |
| Pattern | ViewModel + Repository (MVVM-style layering) |
| Networking | Java-WebSocket (`org.java-websocket:Java-WebSocket:1.5.6`) |
| Speech Inference | ONNX Runtime Android (`com.microsoft.onnxruntime:onnxruntime-android:1.19.0`) |
| Audio Capture | Android `AudioRecord` |
| Build System | Gradle 8.7, Android Gradle Plugin 8.4.1, Kotlin plugin 1.9.22 |

---

## Speech-to-Text Pipeline

Implemented pipeline:

1. **Microphone input** via Android `AudioRecord`
2. Capture in **16 kHz mono PCM16**
3. Preprocessing: short-to-float normalization + mean subtraction
4. Inference with **Vakyansh Hindi Wav2Vec2 ONNX** model
5. Decode token IDs using local `vocab.json`
6. Output recognized **Hindi Devanagari text**

> Inference is performed fully **on-device** in `VakyanshOnnxSttEngine`.

---

## Communication Architecture

- **Host mode:** starts WebSocket server on `0.0.0.0` (default port `8765`)
- **Join mode:** connects to host using `ws://<host-ip>:<port>`
- Operates on **local Wi-Fi/hotspot LAN**
- Only **text JSON messages** are transmitted
- Same APK supports both roles (host/client)

---

## Message Format

`TextMessage` JSON used by both sender and receiver:

```json
{
  "messageId": "<uuid>",
  "senderId": "<device-id>",
  "timestamp": 1720000000000,
  "messageType": "TEXT",
  "text": "नमस्ते"
}
```

Source: `TextMessage.kt` (`toJson()` / `fromJson()`).

---

## Setup Requirements

From project Gradle configuration:

- **Android SDK**
  - `compileSdk = 37`
  - `targetSdk = 37`
  - `minSdk = 26`
- **Gradle wrapper:** `8.7`
- **Android Gradle Plugin:** `8.4.1`
- **Kotlin plugin:** `1.9.22`

Device/runtime requirements:

- Two Android phones
- Same local Wi-Fi network or hotspot
- Microphone permission (`RECORD_AUDIO`)

---

## Build Instructions

From repository root:

```bash
./gradlew.bat assembleDebug
```

(Equivalent on macOS/Linux: `./gradlew assembleDebug`)

Expected debug APK output (default Android path):

```text
/home/runner/work/Itantra/Itantra/app/build/outputs/apk/debug/app-debug.apk
```

---

## Running the POC

1. Install the same APK on both Android phones.
2. Connect both phones to the same local Wi-Fi/hotspot.
3. On phone A, choose **Host Room** (server mode).
4. On phone B, choose **Join Room**, enter host IP/port, connect.
5. Open chat on both devices.
6. Hold Push-to-Talk and speak Hindi on sender phone.
7. Sender phone performs local STT.
8. Recognized text is sent via WebSocket.
9. Receiver phone displays the text message.

---

## Project Limitations / Current Scope

The following are **not implemented in the current repository POC**:

- Full 10-language support (current implementation is Hindi-focused)
- Text-to-Speech (TTS)
- Cloud/server backend communication
- Internet-dependent inference/services
- Audio streaming over network (only text messages are sent)
- Bluetooth communication / Wi-Fi Direct specific transport
- Hardware radio integration

---

## APK

A prebuilt APK is present in the repository:

- `/home/runner/work/Itantra/Itantra/iTantra-POC.apk`

From GitHub, it can be downloaded from the repository file view (`iTantra-POC.apk`) using **Download raw file**.

---

## Research References

Project references already reflected in this repository:

- Vakyansh Hindi ONNX model assets under `app/src/main/assets/onnx/`
- Vakyansh Hindi vocabulary under `app/src/main/assets/vakyansh-hindi/vocab.json`
- ONNX Runtime Android dependency in `app/build.gradle.kts`
- Java-WebSocket dependency in `app/build.gradle.kts`

---

## Notes

This README documents the **current working POC** in this repository. It does not claim full implementation of the larger SIH problem scope.
