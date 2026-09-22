# AgriEdge AI 🌾📱

> **Offline Edge AI Crop & Disease Assistant for Android**  
> Powered by ONNX Runtime, SigLIP Vision Model (103 Classes), and `llama.cpp` Native SLM Engine (`SmolLM2-360M-Instruct`).

---

## 🌟 Overview

**AgriEdge AI** is a fully offline, high-performance Android application engineered for farmers and agronomists operating in low or zero-connectivity rural environments. It combines high-accuracy visual leaf disease classification with an on-device Small Language Model (SLM) for instant, natural-language agronomic diagnosis and interactive follow-up guidance.

```
┌─────────────────┐       ┌───────────────────────┐       ┌────────────────────────┐
│  Camera / Photo │ ───>  │ SigLIP INT8 ONNX      │ ───>  │  Diagnosis Result      │
│  (CameraX 1:1)  │       │ (103 Crop Classes)    │       │  Crop + Condition      │
└─────────────────┘       └───────────────────────┘       └───────────┬────────────┘
                                                                      │
                                                                      ▼
┌─────────────────┐       ┌───────────────────────┐       ┌────────────────────────┐
│  Offline Chat   │ <───  │ SmolLM2-360M SLM      │ <───  │  ChatML Prompt         │
│  (AgriSLM Tab)  │       │ (llama.cpp Native C++)│       │  Engine                │
└─────────────────┘       └───────────────────────┘       └────────────────────────┘
```

---

## ✨ Key Features

- **🔒 100% Offline Edge Inference**: No cloud APIs or internet connection required. Runs completely local on the smartphone.
- **📷 Real-Time CameraX & Image Preprocessing**: Captures and processes high-resolution leaf photos using custom `CenterCropPreprocessor` and standard RGB channel normalization ($Mean=[0.5, 0.5, 0.5]$, $Std=[0.5, 0.5, 0.5]$).
- **🌿 103 Indian Crop & Disease Classes**: Multi-crop support covering Rice, Papaya, Banana, Tomato, Chilli, Coffee, Cotton, Tea, Wheat, Maize, Cucumber, and more.
- **⚡ Hardware-Accelerated Vision Model**: ONNX Runtime Android engine running quantized SigLIP INT8 backbone with XNNPACK optimization.
- **🤖 On-Device Agronomic SLM (`llama.cpp`)**: Integrated native C++ engine (`libagriedge_llm.so`) running `SmolLM2-360M-Instruct-Q4_K_M.gguf` via ChatML format for fast, context-aware agricultural advice.
- **💬 Dual Chat Experiences**:
  1. **Diagnosis Follow-Up Chat**: Ask specific questions (treatment, organic sprays, NPK fertilizers) on the current crop diagnosis result.
  2. **AgriSLM Assistant Tab**: Open-ended agricultural Q&A assistant for crop management, soil health, and pest control.

---

## 📊 Supported Crops & Diseases (103 Classes)

AgriEdge AI supports 103 classes across major Indian crops, including:
- **Rice / Paddy**: Bacterial Leaf Streak, Blast, Brown Spot, Tungro, Healthy.
- **Papaya**: Leaf Curl, Ringspot, Anthracnose, Healthy.
- **Banana**: Xanthomonas Wilt, Sigatoka, Bunchy Top, Healthy.
- **Tomato**: Early Blight, Late Blight, Yellow Leaf Curl Virus, Mosaic Virus, Septoria Leaf Spot, Healthy.
- **Coffee**: Leaf Rust, Cercospora Leaf Spot, Healthy.
- **Cotton**, **Chilli**, **Maize**, **Wheat**, **Cucumber**, **Tea**, and many more.

Labels are listed in `models/india_v1_labels.json`.

---

## 🛠️ Architecture & Tech Stack

- **Platform**: Android SDK 29+ (Android 10 - 15)
- **Language**: Kotlin, Modern Jetpack Compose, C++17
- **Vision Engine**: ONNX Runtime Mobile 1.18.0 (XNNPACK Execution Provider)
- **Language Engine**: `llama.cpp` (Embedded C++ JNI Bridge)
- **Models**:
  - `agri_classifier_india_v1_int8_opt.onnx` (~101 MB)
  - `SmolLM2-360M-Instruct-Q4_K_M.gguf` (~270 MB)
  - `india_v1_labels.json` (103 classes)

---

## 🚀 Getting Started

### Prerequisites
1. Android Studio Ladybug (or newer)
2. Android NDK (v27+) and CMake (v3.22.1+)
3. Physical Android device connected via ADB (`arm64-v8a`)

### Building the Project
```bash
# Set JDK 17+ location
export JAVA_HOME="/path/to/jdk-17"

# Build debug APK
cd app
./gradlew assembleDebug
```

### Deploying Models to Device
Push the model files to the application directory on your physical device:
```bash
# Push ONNX classifier & labels
adb push models/agri_classifier_india_v1_int8_opt.onnx /sdcard/Android/data/com.agriedge.app/files/models/
adb push models/india_v1_labels.json /sdcard/Android/data/com.agriedge.app/files/models/

# Push SmolLM2 GGUF SLM
adb push models/SmolLM2-360M-Instruct-Q4_K_M.gguf /sdcard/Android/data/com.agriedge.app/files/models/
```

---

## 📄 License

Distributed under the MIT License. See `LICENSE` for more information.