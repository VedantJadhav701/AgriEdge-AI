$adb = "C:\Users\HP\AppData\Local\Android\Sdk\platform-tools\adb.exe"

Write-Host "Checking connected Android device..."
& $adb devices

Write-Host "Creating app model directories on device..."
& $adb shell "mkdir -p /sdcard/Android/data/com.agriedge.v2/files/models"
& $adb shell "mkdir -p /sdcard/Android/data/com.agriedge.app/files/models"

Write-Host "Pushing vision classifier models..."
if (Test-Path "C:\Users\HP\projects\agriedge\models\agri_classifier_india_v1_int8_opt.onnx") {
    & $adb push "C:\Users\HP\projects\agriedge\models\agri_classifier_india_v1_int8_opt.onnx" "/sdcard/Android/data/com.agriedge.v2/files/models/"
}
if (Test-Path "C:\Users\HP\projects\agriedge\models\india_v1_labels.json") {
    & $adb push "C:\Users\HP\projects\agriedge\models\india_v1_labels.json" "/sdcard/Android/data/com.agriedge.v2/files/models/"
}

Write-Host "Pushing Qwen2.5-0.5B-Instruct SLM models..."
if (Test-Path "C:\Users\HP\projects\agriedge\models\qwen2.5-0.5b-instruct-q4_k_m.gguf") {
    & $adb push "C:\Users\HP\projects\agriedge\models\qwen2.5-0.5b-instruct-q4_k_m.gguf" "/sdcard/Android/data/com.agriedge.v2/files/models/"
}
if (Test-Path "C:\Users\HP\projects\agriedge\models\agriedge_qwen05b_q4_k_m.gguf") {
    & $adb push "C:\Users\HP\projects\agriedge\models\agriedge_qwen05b_q4_k_m.gguf" "/sdcard/Android/data/com.agriedge.v2/files/models/"
}

Write-Host "Done! All models pushed successfully for AgriEdge v2."
