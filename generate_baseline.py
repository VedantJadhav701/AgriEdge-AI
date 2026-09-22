import onnxruntime as ort
import numpy as np

model_path = r"C:\Users\HP\projects\agriedge\models\agri_classifier_india_v1_int8.onnx"
session = ort.InferenceSession(model_path)
in_name = session.get_inputs()[0].name
out_name = session.get_outputs()[0].name

baseline_logits = np.zeros(103, dtype=np.float32)
for _ in range(50):
    dummy = np.random.rand(1, 3, 224, 224).astype(np.float32)
    l = session.run([out_name], {in_name: dummy})[0][0]
    baseline_logits += l
baseline_logits /= 50.0

kotlin_array = "floatArrayOf(\n    " + ", ".join([f"{v:.4f}f" for v in baseline_logits]) + "\n)"
print(kotlin_array)

with open(r"C:\Users\HP\projects\agriedge\baseline_array.txt", "w") as f:
    f.write(kotlin_array)
