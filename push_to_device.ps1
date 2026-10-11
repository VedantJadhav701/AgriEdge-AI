$adb = "C:\Users\HP\AppData\Local\Android\Sdk\platform-tools\adb.exe"

Write-Host "Checking connected Android device..."
& $adb devices

Write-Host "Creating app model directory on device..."
& $adb shell "mkdir -p /sdcard/Android/data/com.agriedge.app/files/models"

Write-Host "Pushing model files to /sdcard/Android/data/com.agriedge.app/files/models/..."

if (Test-Path "C:\Users\HP\projects\agriedge\models\agri_classifier_india_v1_int8_opt.onnx") {
    & $adb push "C:\Users\HP\projects\agriedge\models\agri_classifier_india_v1_int8_opt.onnx" "/sdcard/Android/data/com.agriedge.app/files/models/"
}
if (Test-Path "C:\Users\HP\projects\agriedge\models\agri_classifier_india_v1_int8.onnx") {
    & $adb push "C:\Users\HP\projects\agriedge\models\agri_classifier_india_v1_int8.onnx" "/sdcard/Android/data/com.agriedge.app/files/models/"
}
if (Test-Path "C:\Users\HP\projects\agriedge\models\india_v1_labels.json") {
    & $adb push "C:\Users\HP\projects\agriedge\models\india_v1_labels.json" "/sdcard/Android/data/com.agriedge.app/files/models/"
}
if (Test-Path "C:\Users\HP\projects\agriedge\models\agriedge_slm_q4_k_m.gguf") {
    & $adb push "C:\Users\HP\projects\agriedge\models\agriedge_slm_q4_k_m.gguf" "/sdcard/Android/data/com.agriedge.app/files/models/"
}
if (Test-Path "C:\Users\HP\projects\agriedge\models\SmolLM2-360M-Instruct-Q4_K_M.gguf") {
    & $adb push "C:\Users\HP\projects\agriedge\models\SmolLM2-360M-Instruct-Q4_K_M.gguf" "/sdcard/Android/data/com.agriedge.app/files/models/"
}

Write-Host "Done! All models pushed successfully."
