# AgriEdge AI: Architecture & Source Bundle for Claude Analysis 🌾📱

Welcome! This bundle contains the core source code, architecture specifications, and documentation for **AgriEdge AI** — a 100% offline edge AI application running on Android that provides leaf disease diagnosis and on-device agronomic conversational assistance.

---

## 📂 Bundle Directory Structure

```
claude-files/
├── docs/
│   ├── AGRIEDGE_SYSTEM_AND_CLASSES.md   # Comprehensive technical report, 103 classes, root cause & fixes
│   ├── readme.md                        # Project overview, setup, and features
│   └── india_v1_labels.json             # 103 target class labels across 17 crops
├── vision/
│   ├── VisionClassifier.kt              # ONNX Runtime Mobile, XNNPACK 4T, NCHW preprocessor, Top-K softmax
│   └── ClassifierResult.kt              # Data models for vision predictions
├── llm/
│   ├── LocalLLM.kt                      # Kotlin wrapper for native llama.cpp JNI engine & ChatML formatting
│   ├── LLMState.kt                      # State machine for model loading and streaming generation
│   ├── LocalLLM.cpp                     # C++ JNI bridge with llama.cpp context, sampling, and callbacks
│   └── CMakeLists.txt                   # Native build definition linking libllama and C++17 JNI library
├── core/
│   ├── AgriEdgeViewModel.kt             # Main state coordinator: Camera/Gallery -> Vision -> LLM Diagnosis
│   ├── ModelManager.kt                  # Scoped storage handler, timestamp invalidation, and model discovery
│   └── AgriEdgeApplication.kt           # Android Application class
├── ui/
│   ├── MainActivity.kt                  # Activity entry point and permissions
│   ├── AgriEdgeAppScreen.kt             # Root navigation shell and bottom navigation bar
│   ├── HomeScreen.kt                    # Camera launcher, image picker, model readiness indicator
│   ├── ResultScreen.kt                  # Disease diagnosis card, top-3 candidates, follow-up offline chat
│   ├── AgriSlmChatScreen.kt             # Free-form agronomic offline conversational assistant
│   ├── CameraScreen.kt                  # CameraX square preview & capture implementation
│   ├── Components.kt                    # Reusable Jetpack Compose UI widgets
│   ├── TechnicalDetailsScreen.kt        # Diagnostics screen (FPS, inference ms, memory, thread count)
│   └── theme/Theme.kt                   # Material 3 theme styling
└── build/
    ├── build.gradle.kts                 # Android Gradle build config, NDK, ONNX Runtime dependencies
    └── push_to_device.ps1               # ADB deployment script for models on physical Android devices
```

---

## ⚡ High-Level Technical Summary

1. **Vision Inference Engine**:
   - Model: Quantized SigLIP INT8 Vision Transformer (`101.37 MB`).
   - Runtime: ONNX Runtime Mobile (`ai.onnxruntime:onnxruntime-android`) with `XNNPACK` execution provider and 4 intra-op threads.
   - Preprocessing: Center-crop square aspect ratio, bilinear resize to $224 \times 224$, RGB channel normalization ($Mean=[0.5, 0.5, 0.5]$, $Std=[0.5, 0.5, 0.5]$), converted to NCHW `FloatBuffer`.
   - Output: 103 classes spanning 17 Indian crop varieties.

2. **Agronomic Small Language Model (SLM)**:
   - Model: `SmolLM2-360M-Instruct-Q4_K_M.gguf` (`270.59 MB`).
   - Runtime: Embedded `llama.cpp` compiled into `libagriedge_llm.so` via Android NDK / CMake.
   - Interface: JNI bridge in `LocalLLM.cpp` invoking `llama_decode`, KV cache, top-p/temperature sampling, and native-to-JVM token callbacks.
   - Formatting: ChatML (`<|im_start|>system...`, `<|im_start|>user...`, `<|im_start|>assistant...`).

3. **Android Storage & Scoped Storage Compliance**:
   - Tested on Android 11+ / Android 14.
   - Models are deployed to app-specific external storage (`/sdcard/Android/data/com.agriedge.app/files/models/`), which grants direct POSIX access without requiring `MANAGE_EXTERNAL_STORAGE` permissions.
   - `ModelManager.kt` checks file sizes and `lastModified` timestamps to sync into the app's internal sandbox.

---

## 🎯 Recommended Areas for Claude Analysis

- **Performance & Latency**: Opportunities for further memory reduction, KV cache reuse, or SIMD acceleration in `LocalLLM.cpp`.
- **Pre/Post-Processing**: Numerical stability and edge cases in `VisionClassifier.kt` (e.g. non-standard color spaces or EXIF orientations).
- **Concurrency & Lifecycle**: Coroutine scoping, cancellation safety, and memory management during large bitmap processing and llama.cpp text streaming.
- **UI/UX Architecture**: State hoisting, Jetpack Compose performance, and error handling in `AgriEdgeViewModel.kt`.
