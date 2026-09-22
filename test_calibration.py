import onnxruntime as ort
import json
import numpy as np

model_path = r"C:\Users\HP\projects\agriedge\models\agri_classifier_india_v1_int8.onnx"
labels_path = r"C:\Users\HP\projects\agriedge\models\india_v1_labels.json"

with open(labels_path, 'r') as f:
    labels = json.load(f)

session = ort.InferenceSession(model_path)
in_name = session.get_inputs()[0].name
out_name = session.get_outputs()[0].name

# Compute baseline bias across 50 random neutral samples
baseline_logits = np.zeros(103, dtype=np.float32)
for _ in range(50):
    dummy = np.random.rand(1, 3, 224, 224).astype(np.float32)
    l = session.run([out_name], {in_name: dummy})[0][0]
    baseline_logits += l
baseline_logits /= 50.0

print("Top 5 baseline logit bias values:")
top_b = np.argsort(baseline_logits)[::-1][:5]
for idx in top_b:
    print(f"  [{idx}] {labels[idx]}: baseline = {baseline_logits[idx]:.4f}")

# Function to run calibrated inference
def predict_calibrated(img_input):
    raw_logits = session.run([out_name], {in_name: img_input})[0][0]
    calibrated_logits = raw_logits - baseline_logits
    
    # Softmax
    exp_c = np.exp(calibrated_logits - np.max(calibrated_logits))
    probs = exp_c / exp_c.sum()
    
    top = np.argsort(probs)[::-1][:5]
    print("\nCalibrated Predictions:")
    for idx in top:
        print(f"  {labels[idx]}: prob={probs[idx]*100:.2f}%, raw_logit={raw_logits[idx]:.2f}, cal_logit={calibrated_logits[idx]:.2f}")

dummy_test = np.random.rand(1, 3, 224, 224).astype(np.float32)
predict_calibrated(dummy_test)
