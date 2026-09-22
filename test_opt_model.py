import onnxruntime as ort
import json
import os
import numpy as np

m_path = r"C:\Users\HP\projects\agriedge\models\agri_classifier_india_v1_int8_opt.onnx"
labels_path = r"C:\Users\HP\projects\agriedge\models\india_v1_labels.json"

with open(labels_path, 'r') as f:
    labels = json.load(f)

session = ort.InferenceSession(m_path)
in_name = session.get_inputs()[0].name
out_name = session.get_outputs()[0].name

filename = os.path.basename(m_path)
print(f"Loaded {filename}")
print("Input shape:", session.get_inputs()[0].shape)
print("Output shape:", session.get_outputs()[0].shape)

# Test input normalization [0, 1]
dummy = np.random.rand(1, 3, 224, 224).astype(np.float32)
logits = session.run([out_name], {in_name: dummy})[0][0]

top = np.argsort(logits)[::-1][:10]
print("\nTop 10 logits for random dummy input:")
for idx in top:
    print(f"  [{idx}] {labels[idx]}: logit={logits[idx]:.4f}")
