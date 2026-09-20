# iTantra — Offline Voice-to-Text Communication POC

[![SIH](https://img.shields.io/badge/Smart%20India%20Hackathon-SIH26173-orange.svg)](https://sih.gov.in)
[![Android](https://img.shields.io/badge/Platform-Android-brightgreen.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-purple.svg)](https://kotlinlang.org)
[![Offline STT](https://img.shields.io/badge/STT-Vakyansh%20ONNX-blue.svg)]()
[![Local Transport](https://img.shields.io/badge/Transport-Local%20WebSocket-informational.svg)]()

> **iTantra converts spoken language into text on the device and sends only the text over a local network, reducing the amount of information that must cross a constrained communication link.**

## 🎯 What This POC Actually Demonstrates

This repository contains a working Android proof-of-concept built around:

**Speech → Local STT → Text → Local WebSocket → Text**

The same APK runs on two Android phones.

1. Hold the microphone button and speak.
2. The phone captures **16 kHz mono PCM** using Android `AudioRecord`.
3. **Vakyansh Hindi Wav2Vec2** runs locally through **ONNX Runtime**.
4. The resulting text is serialized as a JSON message.
5. The text is sent over a **local WebSocket connection**.
6. The receiving phone parses the message and displays it.
7. Direct text messaging is also supported.

> **Current scope:** the APK proves local Hindi STT and local text transport. Multilingual STT, offline translation, offline TTS, VAD and constrained-radio transport are future extensions.

## ✅ Current POC Capabilities

| Capability | Status |
|---|:---:|
| Android application | ✅ |
| Same APK on two phones | ✅ |
| Hold-to-talk interaction | ✅ |
| 16 kHz mono PCM capture | ✅ |
| On-device Vakyansh Hindi STT | ✅ |
| ONNX Runtime inference | ✅ |
| Local WebSocket server | ✅ |
| Local WebSocket client | ✅ |
| Text-only communication payload | ✅ |
| Host / Join connection model | ✅ |
| Local IP discovery | ✅ |
| Message serialization | ✅ |
| Conversation history | ✅ |
| Connection status UI | ✅ |
| Direct text messaging | ✅ |
| Offline neural TTS | 🔄 Future |
| Multilingual offline STT | 🔄 Future |
| Offline translation | 🔄 Future |
| VAD | 🔄 Future |
| Payload compression | 🔄 Future |
| Low-bandwidth radio transport | 🔄 Future |
| Mesh networking | 🔄 Future |
| Emergency messaging | 🔄 Future |

---

# 🏗️ Architecture

## End-to-End

```mermaid
flowchart LR
    U[User] --> PTT[Push-to-Talk]
    PTT --> AR[AudioRecord]
    AR --> PCM[16 kHz Mono PCM]
    PCM --> STT[Vakyansh Hindi STT]
    STT --> ORT[ONNX Runtime]
    ORT --> TXT[Recognized Text]
    TXT --> JSON[TextMessage JSON]
    JSON --> WS[Local WebSocket]
    WS --> RX[Receiving Phone]
    RX --> UI[Conversation UI]
```

## Two-Phone Architecture

```mermaid
flowchart LR
    subgraph A[Phone A — Sender]
        AUI[Compose UI]
        AVM[CommunicationViewModel]
        ASTT[VakyanshOnnxSttEngine]
        AWS[WebSocket Client]
        AUI --> AVM --> ASTT --> AWS
    end

    subgraph N[Local Network]
        WIFI[Wi-Fi / Hotspot / LAN]
        IP[Local IP]
    end

    subgraph B[Phone B — Receiver]
        BWS[WebSocket Server]
        BREPO[CommunicationRepository]
        BUI[Conversation UI]
        BWS --> BREPO --> BUI
    end

    AWS --> WIFI --> IP --> BWS
```

## Android Architecture

```mermaid
flowchart TB
    UI[Jetpack Compose UI]
    CVM[CommunicationViewModel]
    KVM[ConnectionViewModel]
    REPO[CommunicationRepository]
    MODEL[TextMessage]
    STATE[ConnectionState]
    STT[SpeechToTextEngine]
    VAK[VakyanshOnnxSttEngine]
    ORT[ONNX Runtime]
    CLIENT[ITantraWebSocketClient]
    SERVER[ITantraWebSocketServer]
    NET[LocalNetworkUtils]

    UI --> CVM
    UI --> KVM
    CVM --> STT
    STT --> VAK --> ORT
    CVM --> REPO
    KVM --> REPO
    REPO --> MODEL
    REPO --> STATE
    REPO --> CLIENT
    REPO --> SERVER
    REPO --> NET
```

## Speech Recognition Pipeline

```mermaid
flowchart TB
    MIC[Microphone]
    AR[AudioRecord]
    PCM[PCM16 samples]
    PRE[Pre-processing]
    TENSOR[ONNX input tensor]
    VAK[Vakyansh Hindi Wav2Vec2]
    LOGITS[Model logits]
    DEC[Greedy decoding]
    VOCAB[Hindi vocabulary]
    TEXT[Transcribed text]

    MIC --> AR --> PCM --> PRE --> TENSOR --> VAK --> LOGITS --> DEC --> VOCAB --> TEXT
```

## Local Communication Sequence

```mermaid
sequenceDiagram
    participant A as Phone A
    participant S as Local WebSocket Server
    participant B as Phone B

    A->>A: Capture speech
    A->>A: Run local Vakyansh STT
    A->>A: Build TextMessage JSON
    A->>S: Send text
    S->>B: Forward WebSocket message
    B->>B: Parse message
    B->>B: Display text
```

---

# 📡 Local Communication

The host phone starts a WebSocket server on port **8765**. The joining phone connects using:

```text
ws://<host-local-ip>:8765
```

The communication layer sends **text messages**, not recorded audio.

Example payload:

```json
{
  "senderId": "7a31c2f1",
  "text": "namaskaram"
}
```

This separation means the speech engine can evolve independently from the transport layer.

---

# 🧠 Why Text Instead of Audio?

Traditional voice communication:

```text
Microphone → Audio Stream → Network → Speaker
```

iTantra POC:

```text
Microphone → Local STT → Text → Network → Text
```

The proof-of-concept therefore moves the speech processing to the endpoints and leaves the communication link responsible for the information itself.

---

# 📱 Application Flow

```text
Setup
  ↓
Connection
  ↓
Communication
```

### Connection

One phone acts as **Host** and starts the local WebSocket server.

The other phone acts as **Join** and connects using the host's local IP address.

### Communication

The communication screen provides:

- connection status
- conversation history
- text input
- send button
- hold-to-talk microphone button
- transcription status

---

# 🗂️ Project Structure

```text
app/src/main/
├── assets/
│   ├── onnx/
│   │   ├── vakyansh_hindi.onnx
│   │   └── vakyansh_hindi.onnx.data
│   └── vakyansh-hindi/
│       └── vocab.json
│
└── java/com/itantara/app/
    ├── MainActivity.kt
    ├── data/
    │   ├── model/
    │   └── repository/
    │       └── CommunicationRepository.kt
    ├── network/
    │   ├── ConnectionState.kt
    │   ├── ITantraWebSocketClient.kt
    │   ├── ITantraWebSocketServer.kt
    │   └── LocalNetworkUtils.kt
    ├── speech/
    │   ├── SpeechToTextEngine.kt
    │   └── VakyanshOnnxSttEngine.kt
    └── presentation/
        ├── setup/
        ├── connection/
        ├── communication/
        ├── navigation/
        └── theme/
```

---

# ⚙️ Technology Stack

| Layer | Technology |
|---|---|
| Platform | Android |
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| State | Kotlin StateFlow |
| Audio capture | Android AudioRecord |
| STT | Vakyansh Hindi Wav2Vec2 |
| ML runtime | ONNX Runtime Android |
| Transport | WebSocket |
| WebSocket library | Java-WebSocket 1.5.6 |
| Network | Local Wi-Fi / hotspot / LAN |
| Serialization | JSON |
| Minimum SDK | 26 |
| Target SDK | 37 |

---

# 🧪 What the POC Proves

### Local STT
The Vakyansh model is packaged into the Android application and loaded locally through ONNX Runtime.

### Text-only transport
The recorded audio is **not** sent to the peer. Only the recognition result is transmitted.

### Same APK, two roles
The same build can act as host or joiner.

### Transport independence
The speech engine and networking layer are separated, allowing the transport to be replaced later without redesigning the speech pipeline.

---

# 🚀 Run the POC

## Requirements

- Android Studio
- JDK 17
- Android SDK
- Two Android phones
- Both phones connected to the same Wi-Fi network or hotspot
- Microphone permission

## Build

Linux/macOS:

```bash
./gradlew assembleDebug
```

Windows:

```powershell
gradlew.bat assembleDebug
```

Application ID:

```text
com.itantara.app
```

## Judge Demo

**Phone A**

1. Install the APK.
2. Open the app.
3. Select **Host**.
4. Start the local server.
5. Note the displayed local IP.

**Phone B**

1. Install the same APK.
2. Select **Join**.
3. Enter Phone A's local IP.
4. Connect.

**Demonstrate**

1. Open Communication on both phones.
2. Hold the microphone button on Phone A.
3. Speak a Hindi sentence.
4. Release.
5. Local Vakyansh STT produces text.
6. The text is sent over the local WebSocket.
7. Phone B displays the received text.

---

# 📥 Download the POC APK

## **[Download iTantra POC APK](https://github.com/theguy1234567/Itantra/blob/main/iTantra-POC.apk)**

The APK is included directly in this repository as `iTantra-POC.apk`.

---

# 🔭 Future Scope

### 1. Multilingual Offline STT
Extend the local STT layer beyond Hindi to the targeted Indian languages.

### 2. Offline Translation
Add an on-device translation layer so messages can be translated without Internet access.

### 3. Offline TTS
Convert received text back into speech locally on the receiving phone.

### 4. Voice Activity Detection
Detect speech boundaries automatically and reduce unnecessary inference.

### 5. Payload Compression
Compress text and add fragmentation, acknowledgement and retransmission for very small links.

### 6. Low-Bandwidth Radio
Add transport adapters for LoRa-class, custom RF, satellite and other constrained links.

### 7. Store-and-Forward Mesh
Allow nearby devices to relay text messages beyond direct range.

### 8. Emergency Priority Messages
Add prioritised alert packets for mission-critical communication.

### 9. Resource Optimisation
Measure and optimise model size, RAM, CPU, inference latency, battery consumption and payload size.

### 10. Full iTantra Transceiver
Combine:
```text
Offline STT
+ Offline Translation
+ Text Compression
+ Low-Bandwidth Transport
+ Offline TTS
+ Emergency Messaging
```
into the complete multilingual neural transceiver envisioned by the project.

---

# 🧱 POC vs. Full System

| Capability | Current POC | Future System |
|---|:---:|:---:|
| Android app | ✅ | ✅ |
| Push-to-talk | ✅ | ✅ |
| **Offline local STT** | ✅ **Hindi / Vakyansh ONNX** | ✅ Multilingual |
| **16 kHz PCM audio capture** | ✅ | ✅ |
| **On-device ONNX inference** | ✅ | ✅ |
| **Local WebSocket server/client** | ✅ | Development transport |
| **Text-only communication** | ✅ | ✅ |
| **Host / Join two-phone flow** | ✅ | ✅ |
| **Local IP discovery** | ✅ | ✅ |
| **Conversation UI / message history** | ✅ | ✅ |
| Offline translation | — | ✅ |
| Offline TTS | — | ✅ |
| VAD | — | ✅ |
| Payload compression | — | ✅ |
| Low-bandwidth radio | — | ✅ |
| Mesh networking | — | ✅ |
| Emergency messaging | — | ✅ |
| **Fully Internet-independent STT + text transport** | ✅ | ✅ Full pipeline |

---

# 📌 Problem–Solution Summary

**Problem:** Conventional voice communication keeps a continuous audio stream in the communication path.

**POC approach:** iTantra performs speech recognition at the endpoint and sends the resulting information as text.

```text
Speech
  ↓
Local STT
  ↓
Text
  ↓
Local Communication Link
  ↓
Text
```

**Long-term direction:** replace the development transport and expand the local model stack so the same architecture can operate across much more constrained communication links.

---

<p align="center">
  <b>iTantra</b><br>
  <i>Voice transformed into information that can travel beyond the network.</i>
</p>