import onnxruntime as ort
import json
import numpy as np

models = [
    r"C:\Users\HP\projects\agriedge\models\agri_classifier_india_v1_int8.onnx",
    r"C:\Users\HP\projects\agriedge\models\agri_classifier_india_v1_int8_mobile.onnx",
    r"C:\Users\HP\projects\agriedge\models\agri_classifier_india_v1_int8_opt.onnx",
]

labels_path = r"C:\Users\HP\projects\agriedge\models\india_v1_labels.json"
with open(labels_path, 'r') as f:
    labels = json.load(f)

dummy = np.random.rand(1, 3, 224, 224).astype(np.float32)

for m_path in models:
    print(f"\n--- Testing {m_path.split('\\')[-1]} ---")
    try:
        session = ort.InferenceSession(m_path)
        in_name = session.get_inputs()[0].name
        out_name = session.get_outputs()[0].name
        logits = session.run([out_name], {in_name: dummy})[0][0]
        top = np.argsort(logits)[::-1][:5]
        for idx in top:
            print(f"  {labels[idx]} (idx {idx}): logit={logits[idx]:.4f}")
    except Exception as e:
        print(f"  Error: {e}")
