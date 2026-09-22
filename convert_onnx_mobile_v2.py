import onnx
from onnx import helper, numpy_helper, TensorProto
import numpy as np

model_path = r"C:\Users\HP\projects\agriedge\models\agri_classifier_india_v1_int8.onnx"
output_path = r"C:\Users\HP\projects\agriedge\models\agri_classifier_india_v1_int8_mobile.onnx"

print(f"Loading original ONNX model from {model_path}...")
# Reload original model
import shutil
shutil.copyfile(r"C:\Users\HP\projects\agriedge\models\agri_classifier_india_v1_int8_mobile.onnx", model_path)
model = onnx.load(model_path)
graph = model.graph

initializer_map = {init.name: init for init in graph.initializer}

target_node = None
for node in graph.node:
    if node.op_type == "ConvInteger":
        target_node = node
        break

if target_node:
    print(f"Target node: {target_node.name}")
    input_x = target_node.input[0]
    input_w = target_node.input[1]
    input_x_zero_point = target_node.input[2] if len(target_node.input) > 2 else None
    input_w_zero_point = target_node.input[3] if len(target_node.input) > 3 else None
    output_y = target_node.output[0]

    # Convert uint8 input X to float32 via Cast node
    float_x_name = input_x + "_float32"
    cast_x_node = helper.make_node(
        "Cast",
        inputs=[input_x],
        outputs=[float_x_name],
        to=TensorProto.FLOAT
    )

    # Convert weights W to float32
    w_init = initializer_map.get(input_w)
    if w_init:
        w_data = numpy_helper.to_array(w_init).astype(np.float32)
        w_zero_point = 0
        if input_w_zero_point and input_w_zero_point in initializer_map:
            w_zero_point = numpy_helper.to_array(initializer_map[input_w_zero_point])
        w_data = w_data - w_zero_point
        
        float_w_name = input_w + "_float32"
        float_w_init = numpy_helper.from_array(w_data, name=float_w_name)
        graph.initializer.append(float_w_init)

        # Create standard Conv node taking float32 X and float32 W
        conv_node = helper.make_node(
            "Conv",
            inputs=[float_x_name, float_w_name],
            outputs=[output_y],
            name=target_node.name + "_fp32_conv"
        )
        
        for attr in target_node.attribute:
            conv_node.attribute.append(attr)

        idx = list(graph.node).index(target_node)
        graph.node.remove(target_node)
        graph.node.insert(idx, cast_x_node)
        graph.node.insert(idx + 1, conv_node)
        print("Successfully converted ConvInteger to Cast + Conv float32!")

onnx.save(model, output_path)
onnx.save(model, model_path)
print("Saved converted model successfully!")
