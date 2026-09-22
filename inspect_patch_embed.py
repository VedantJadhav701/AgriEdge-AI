import onnx
from onnx import numpy_helper
import numpy as np

model_path = r"C:\Users\HP\projects\agriedge\models\agri_classifier_india_v1_int8.onnx"
model = onnx.load(model_path)
graph = model.graph

print("Nodes in graph:")
for i, node in enumerate(graph.node[:15]):
    print(f"Node {i}: {node.op_type} ({node.name})")
    print(f"   Inputs: {node.input}")
    print(f"   Outputs: {node.output}")

print("\nInitializers in graph top 15:")
for init in graph.initializer[:15]:
    arr = numpy_helper.to_array(init)
    print(f"Init {init.name}: shape={arr.shape}, dtype={arr.dtype}, min={arr.min()}, max={arr.max()}")
