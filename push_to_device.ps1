$adb = "C:\Users\HP\AppData\Local\Android\Sdk\platform-tools\adb.exe"

Write-Host "Checking connected Android device..."
& $adb devices

Write-Host "Pushing model files to /sdcard/Download/..."

& $adb push "C:\Users\HP\projects\agriedge\models\agri_classifier_india_v1_int8.onnx" "/sdcard/Download/"
& $adb push "C:\Users\HP\projects\agriedge\models\india_v1_labels.json" "/sdcard/Download/"
& $adb push "C:\Users\HP\projects\agriedge\models\agriedge_slm_q4_k_m.gguf" "/sdcard/Download/"

Write-Host "Done! All models pushed successfully to /storage/emulated/0/Download/"
