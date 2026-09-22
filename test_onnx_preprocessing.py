import onnxruntime as ort
import json
import numpy as np

model_path = r"C:\Users\HP\projects\agriedge\models\agri_classifier_india_v1_int8.onnx"
labels_path = r"C:\Users\HP\projects\agriedge\models\india_v1_labels.json"

with open(labels_path, 'r') as f:
    labels = json.load(f)

print(f"Loaded {len(labels)} labels.")
session = ort.InferenceSession(model_path)

input_name = session.get_inputs()[0].name
output_name = session.get_outputs()[0].name

print(f"Input name: {input_name}, shape: {session.get_inputs()[0].shape}, type: {session.get_inputs()[0].type}")
print(f"Output name: {output_name}, shape: {session.get_outputs()[0].shape}")

# Test 1: Random noise [0, 1]
dummy_0_1 = np.random.rand(1, 3, 224, 224).astype(np.float32)
out1 = session.run([output_name], {input_name: dummy_0_1})[0][0]
top1 = np.argsort(out1)[::-1][:5]
print("\nTest 1 (Range [0, 1]):")
for idx in top1:
    print(f"  {labels[idx]}: {out1[idx]:.4f}")

# Test 2: Standard [-1, 1] i.e. (x - 0.5)/0.5
dummy_m1_1 = (dummy_0_1 - 0.5) / 0.5
out2 = session.run([output_name], {input_name: dummy_m1_1})[0][0]
top2 = np.argsort(out2)[::-1][:5]
print("\nTest 2 (Range [-1, 1]):")
for idx in top2:
    print(f"  {labels[idx]}: {out2[idx]:.4f}")

# Test 3: Raw uint8 values [0, 255]
dummy_0_255 = (dummy_0_1 * 255.0).astype(np.float32)
out3 = session.run([output_name], {input_name: dummy_0_255})[0][0]
top3 = np.argsort(out3)[::-1][:5]
print("\nTest 3 (Range [0, 255]):")
for idx in top3:
    print(f"  {labels[idx]}: {out3[idx]:.4f}")

# Test 4: ImageNet normalized (x - mean)/std
mean = np.array([0.485, 0.456, 0.406], dtype=np.float32).reshape(1, 3, 1, 1)
std = np.array([0.229, 0.224, 0.225], dtype=np.float32).reshape(1, 3, 1, 1)
dummy_imagenet = (dummy_0_1 - mean) / std
out4 = session.run([output_name], {input_name: dummy_imagenet})[0][0]
top4 = np.argsort(out4)[::-1][:5]
print("\nTest 4 (ImageNet normalized):")
for idx in top4:
    print(f"  {labels[idx]}: {out4[idx]:.4f}")
