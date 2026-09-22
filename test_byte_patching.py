import os

orig_path = r"C:\Users\HP\projects\agriedge\models\agri_classifier_india_v1_int8.onnx"
fixed_path = r"C:\Users\HP\projects\agriedge\models\agri_classifier_india_v1_int8_fixed.onnx"

print("Checking fixed model existence:")
print("Original exists:", os.path.exists(orig_path))
print("Fixed exists:", os.path.exists(fixed_path))
if os.path.exists(fixed_path):
    print("Fixed file size:", os.path.getsize(fixed_path))
