You are working on my Android project: AgriEdge.

GOAL
Build AgriEdge into a polished, fully offline Android agricultural AI application.

AgriEdge workflow:

Smartphone Camera
        ↓
Image preprocessing
        ↓
AgriEdge Vision Classifier
        ↓
Crop + Condition
        ↓
Structured prompt
        ↓
SmolLM2-360M-Instruct Q4_K_M
        ↓
Offline explanation
        ↓
User-friendly result screen

IMPORTANT:
This app MUST work completely offline and in airplane mode.
Do NOT add internet permissions.
Do NOT add cloud APIs.
Do NOT download models at runtime.

CURRENT PROJECT STATE

Existing Android project already contains:

1. AndroidManifest.xml
2. ModelManager.kt
3. VisionClassifier.kt
4. ClassifierResult.kt
5. Compose setup
6. CameraX
7. ONNX Runtime Android

Current Android configuration:
- namespace: com.agriedge.app
- minSdk: 29
- targetSdk: 34
- compileSdk: 34
- Kotlin/JVM 17
- Jetpack Compose
- CameraX 1.3.4
- ONNX Runtime Android 1.18.0

Existing manifest intentionally contains CAMERA permission only.
Preserve this offline requirement.

VISION MODEL

The production classifier is:

agri_classifier_india_v1_int8.onnx

Model size: approximately 100 MB.

Labels:

india_v1_labels.json

The classifier is a SINGLE unified classification head.

Input:
- tensor name: pixel_values
- shape: [1, 3, 224, 224]
- float32

Output:
- tensor name: logits
- shape: [1, 103]
- float32

The 103 labels are strings in this format:

crop__condition

Example concept:

tomato__early_blight
rice__brown_spot
banana__healthy

The index of the model output corresponds directly to the label index.

Do NOT implement the old two-head crop/disease classifier.

The existing VisionClassifier.kt already:
- loads ONNX Runtime
- loads 103 labels
- resizes image to 224x224
- normalizes using mean=[0.5,0.5,0.5]
- normalizes using std=[0.5,0.5,0.5]
- runs ONNX inference
- applies softmax
- obtains top prediction
- splits crop and condition using "__"
- returns confidence
- returns top-K
- returns inference time

PRESERVE this logic unless a bug is found.

There is currently a known preprocessing difference:
Python preprocessing uses bicubic resizing, while Android Bitmap.createScaledBitmap(..., true) is bilinear.

Do NOT silently ignore this.
Keep current baseline working first.
Then create a clean abstraction so Android preprocessing can later be changed to bicubic if needed.

SLM MODEL

The second model is:

agriedge_slm_q4_k_m.gguf

Model:
SmolLM2-360M-Instruct

Quantization:
Q4_K_M

Size:
approximately 271 MB.

The model must run locally on Android using llama.cpp through JNI.

Do NOT use Hugging Face Transformers inside Android.
Do NOT use Python.
Do NOT use an HTTP server.
Do NOT call an external LLM API.

Implement:

Kotlin
   ↓
JNI
   ↓
llama.cpp
   ↓
agriedge_slm_q4_k_m.gguf

STAGE 2 REQUIREMENT

Extend ModelManager.kt.

Currently it manages:

agri_classifier_india_v1_int8.onnx
india_v1_labels.json

Add:

agriedge_slm_q4_k_m.gguf

The current ModelManager explicitly leaves the SLM file for Stage 2.
Implement that Stage 2 functionality.

For development, the app may continue supporting adb-pushed models.

For the final release APK, prefer packaging the model files inside the application assets and copying them into the app's private models directory on first launch.

Never download them from the internet.

APP UI

Create a clean, modern agricultural application called:

AgriEdge

Use a professional agriculture visual style:
- white background
- dark green primary color
- subtle green accents
- rounded cards
- clean typography
- minimal clutter
- large camera action
- good spacing
- accessible text

The current theme already uses dark green #1B4332 and white.
Keep this general visual direction.

MAIN SCREEN

Show:

AgriEdge

"Offline Crop & Disease Assistant"

Short description:

"Take a photo of a crop leaf to identify the crop and possible condition."

Main actions:

[ Take Photo ]

[ Choose Image ]

Also show a small status indicator:

● Offline AI Ready

or

● Loading models...

or

● Model Error

CAMERA SCREEN

Use CameraX.

Requirements:
- portrait orientation
- live camera preview
- capture button
- permission handling
- clean camera UI
- no internet requirement
- after capture, pass Bitmap to VisionClassifier

Do not run inference on the main/UI thread.

Show a loading state while inference is running.

RESULT SCREEN

After vision inference, display:

Crop
<Top crop name>

Condition
<Top condition>

Confidence
<XX.X%>

Inference
<XX ms>

Also show a visual confidence indicator.

Then send the structured result to the SLM.

SLM INPUT FORMAT

Use a deterministic prompt.

Example:

Crop: tomato

Visual finding: image classifier detected tomato__early_blight

Possible condition: early_blight

Confidence: 92.4%

Question: What does this result mean and what should the farmer check next?

Answer:

The SLM should generate a short, readable explanation.

IMPORTANT:
The SLM must NOT be allowed to invent visual findings that the classifier did not provide.

The SLM receives structured classifier output.
It does NOT receive the image directly.

If confidence is low, clearly communicate uncertainty.

For example:

"Prediction confidence is low. Check the leaf under good lighting or capture another image."

Do not present the classification as confirmed diagnosis.

RESULT UI

Use sections/cards:

┌──────────────────────────────┐
│ Crop                         │
│ Tomato                       │
├──────────────────────────────┤
│ Possible condition           │
│ Early Blight                 │
├──────────────────────────────┤
│ Confidence                   │
│ 92.4%                        │
├──────────────────────────────┤
│ AI Explanation               │
│ ...generated explanation...  │
└──────────────────────────────┘

Also provide:

[ Scan Another Leaf ]

TOP-K

The existing classifier returns top-K predictions.

Add a collapsible section:

"Other possible conditions"

Display top 3 predictions:

1. tomato__early_blight — 92.4%
2. tomato__leaf_spot — 4.1%
3. tomato__healthy — 1.8%

Make this secondary information.
Do not overwhelm the user.

SLM GENERATION UI

Do NOT show raw model tokens, hidden reasoning, or technical prompt text.

Show:

"Generating explanation..."

Then display clean generated text.

If llama.cpp supports token streaming through JNI, implement streaming into the UI.

The UI should not freeze while generating.

Provide:

[Stop ]

if generation is running.

MODEL STATUS

Create a model-status mechanism.

Required states:

Vision model:
- Missing
- Loading
- Ready
- Error

SLM:
- Missing
- Loading
- Ready
- Error

The user should clearly know if the app is ready.

If models are missing:

"AI model files are missing.
Please install the AgriEdge model package."

Do not show a download button.

OFFLINE ACCEPTANCE TEST

The final app must pass this test:

1. Install APK.
2. Put both models on device / package them in APK.
3. Enable airplane mode.
4. Open AgriEdge.
5. Camera works.
6. Capture leaf.
7. Vision model produces crop + condition.
8. SLM produces explanation.
9. No network request occurs.
10. Entire workflow completes locally.

No INTERNET permission may be added.

LLAMA.CPP INTEGRATION

Add a native C++ layer using llama.cpp.

Create a clean JNI interface.

Suggested architecture:

com.agriedge.app.llm.LocalLLM

↓

LocalLLM.kt

↓

native methods

↓

LocalLLM.cpp

↓

llama.cpp

Required operations:

initialize(modelPath)
isReady()
generate(prompt, maxTokens, temperature)
stopGeneration()
release()

Prefer streaming generation if practical.

Keep JNI implementation isolated from Compose UI.

Do not scatter native calls throughout the application.

THREADING

Never run:
- ONNX inference
- model loading
- llama.cpp initialization
- SLM generation

on the Android main thread.

Use Kotlin coroutines.

Recommended:

Dispatchers.IO
for model loading / file operations

Dispatchers.Default
for CPU inference work

UI state:
StateFlow / ViewModel

Create a single App/ViewModel layer coordinating:

Camera image
    ↓
VisionClassifier
    ↓
ClassifierResult
    ↓
SLM prompt builder
    ↓
LocalLLM
    ↓
UI result

ERROR HANDLING

Handle:

- camera permission denied
- camera unavailable
- missing ONNX model
- missing labels
- invalid label count
- ONNX initialization error
- ONNX inference error
- missing GGUF
- llama.cpp initialization failure
- SLM generation failure
- invalid classifier output
- low confidence
- activity recreation
- device rotation if applicable

Do not crash the application.

Use readable user-facing error messages.

DO NOT BREAK EXISTING CLASSIFIER

The existing classifier expects:

agri_classifier_india_v1_int8.onnx

and

india_v1_labels.json

It expects exactly 103 labels.

If the label count is not 103, stop and report an error.

Do not fall back to:
- labels.json
- old v0 classifier
- two-head crop/disease model
- agri_classifier.onnx
- agri_classifier_int8.onnx

The India v1 model is the production classifier.

PROJECT STRUCTURE

Organize the project approximately as:

app/
 ├── src/main/
 │   ├── java/com/agriedge/app/
 │   │   ├── MainActivity.kt
 │   │   ├── AgriEdgeApplication.kt
 │   │   ├── classifier/
 │   │   │   ├── VisionClassifier.kt
 │   │   │   ├── ClassifierResult.kt
 │   │   │   └── ClassifierState.kt
 │   │   ├── model/
 │   │   │   └── ModelManager.kt
 │   │   ├── llm/
 │   │   │   ├── LocalLLM.kt
 │   │   │   └── LLMState.kt
 │   │   ├── ui/
 │   │   │   ├── HomeScreen.kt
 │   │   │   ├── CameraScreen.kt
 │   │   │   ├── ResultScreen.kt
 │   │   │   └── Components.kt
 │   │   └── viewmodel/
 │   │       └── AgriEdgeViewModel.kt
 │   │
 │   ├── cpp/
 │   │   ├── CMakeLists.txt
 │   │   ├── LocalLLM.cpp
 │   │   └── JNI bridge
 │   │
 │   └── assets/
 │       ├── agri_classifier_india_v1_int8.onnx
 │       ├── india_v1_labels.json
 │       └── agriedge_slm_q4_k_m.gguf

Do not blindly create duplicate classes.
First inspect the existing project and reuse existing code.

BUILD CONFIGURATION

The current Gradle project uses:

- Android application plugin
- Kotlin Android plugin
- Compose
- CameraX
- ONNX Runtime Android 1.18.0

Keep these working.

For llama.cpp:
- add externalNativeBuild/CMake only where required
- keep native code isolated
- configure ABI deliberately
- initially target arm64-v8a
- do not unnecessarily build every ABI

The target device is an Android edge device, so prioritize:

arm64-v8a
CPU inference
low memory usage
low startup time
low latency

PERFORMANCE

Measure and display:

Vision inference time
SLM generation time
Total pipeline time

Do not fake benchmark values.

Use actual measured values from the device.

Avoid keeping large Bitmaps in memory.

Release ONNX sessions correctly.

Release llama.cpp resources correctly.

Avoid memory leaks.

IMPORTANT MODEL FACTS

Vision model:
~100 MB INT8 ONNX
103 unified crop-condition classes
224x224 input

SLM:
SmolLM2-360M-Instruct
Q4_K_M GGUF
~271 MB

The complete local AI stack is therefore approximately:

~100 MB vision model
+
~271 MB language model
+
runtime/application overhead

Design the app for this resource constraint.

FINAL UI GOAL

The application should feel like a real agricultural mobile product, not a developer demo.

User should only need to:

1. Open app.
2. Take leaf photo.
3. Wait for analysis.
4. See crop.
5. See possible condition.
6. See confidence.
7. Read short offline AI explanation.

Keep technical information hidden behind an optional "Technical details" section.

TECHNICAL DETAILS SCREEN

Optional expandable section:

Model:
AgriEdge India v1

Vision:
103-class INT8 ONNX classifier

Input:
224 × 224

Vision inference:
XX ms

Language model:
SmolLM2-360M Q4_K_M

LLM generation:
XX ms

Runtime:
ONNX Runtime + llama.cpp

Network:
Offline

Do not expose raw logits unless useful for debugging.

IMPLEMENTATION METHOD

Before modifying code:

1. Inspect every existing source file.
2. Identify what is already implemented.
3. Do not rewrite working VisionClassifier.kt unnecessarily.
4. Identify missing MainActivity/UI/ViewModel/JNI pieces.
5. Implement incrementally.
6. Build after each major stage.
7. Fix compile errors.
8. Test model loading.
9. Test ONNX inference.
10. Test llama.cpp initialization.
11. Test SLM generation.
12. Test complete camera → classifier → SLM pipeline.

FIRST DELIVERABLE

Do NOT immediately dump huge amounts of code.

First give me:

1. Current project assessment.
2. Files that already work.
3. Files that need modification.
4. Files that need creation.
5. Exact llama.cpp integration plan.
6. Exact model packaging plan.
7. UI architecture.
8. Step-by-step implementation order.

Then implement Stage 2 and Stage 3.

Most important requirement:

BUILD A REAL FULLY OFFLINE AGRIEDGE APP.

No cloud.
No API.
No internet.
No fake inference.
No fake latency.
No placeholder AI output in the final implementation.