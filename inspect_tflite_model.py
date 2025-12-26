#!/usr/bin/env python3
"""
Inspect TFLite model to understand input/output specifications.
Run this to see what the model actually expects.
"""

import tensorflow as tf
import sys
import os

# Path to the TFLite model (try multiple locations)
MODEL_PATHS = [
    "app/src/main/assets/mobilebert_phase1_int8.tflite",  # Android project
    "../AndroidStudioProjects/Koshpal/app/src/main/assets/mobilebert_phase1_int8.tflite",  # From ML dir
    "assets/mobilebert_phase1_int8.tflite",  # ML assets dir
]

MODEL_PATH = None
for path in MODEL_PATHS:
    if os.path.exists(path):
        MODEL_PATH = path
        break

if MODEL_PATH is None:
    print(f"❌ Model not found. Tried:")
    for path in MODEL_PATHS:
        print(f"   - {path}")
    sys.exit(1)

if not os.path.exists(MODEL_PATH):
    print(f"❌ Model not found at: {MODEL_PATH}")
    sys.exit(1)

print("=" * 80)
print("TFLite Model Inspection")
print("=" * 80)
print(f"\nModel: {MODEL_PATH}")
print(f"Size: {os.path.getsize(MODEL_PATH) / (1024*1024):.2f} MB\n")

try:
    # Load the TFLite model
    interpreter = tf.lite.Interpreter(model_path=MODEL_PATH)
    interpreter.allocate_tensors()
    
    # Get input details
    input_details = interpreter.get_input_details()
    print("📥 INPUT DETAILS:")
    print("-" * 80)
    for i, input_detail in enumerate(input_details):
        print(f"Input {i}:")
        print(f"  Name: {input_detail.get('name', 'N/A')}")
        print(f"  Shape: {input_detail['shape']}")
        print(f"  Data Type: {input_detail['dtype']} ({tf.dtypes.as_dtype(input_detail['dtype']).name})")
        
        # Check quantization parameters
        if 'quantization_parameters' in input_detail:
            quant_params = input_detail['quantization_parameters']
            print(f"  Quantization:")
            print(f"    Scale: {quant_params['scales']}")
            print(f"    Zero Point: {quant_params['zero_points']}")
            print(f"    Quantized Dimension: {quant_params.get('quantized_dimension', 'N/A')}")
        else:
            print(f"  Quantization: None (not quantized)")
        print()
    
    # Get output details
    output_details = interpreter.get_output_details()
    print("📤 OUTPUT DETAILS:")
    print("-" * 80)
    for i, output_detail in enumerate(output_details):
        print(f"Output {i}:")
        print(f"  Name: {output_detail.get('name', 'N/A')}")
        print(f"  Shape: {output_detail['shape']}")
        print(f"  Data Type: {output_detail['dtype']} ({tf.dtypes.as_dtype(output_detail['dtype']).name})")
        
        # Check quantization parameters
        if 'quantization_parameters' in output_detail:
            quant_params = output_detail['quantization_parameters']
            print(f"  Quantization:")
            print(f"    Scale: {quant_params['scales']}")
            print(f"    Zero Point: {quant_params['zero_points']}")
            print(f"    Quantized Dimension: {quant_params.get('quantized_dimension', 'N/A')}")
        else:
            print(f"  Quantization: None (not quantized)")
        print()
    
    # Test with dummy input
    print("🧪 TESTING WITH DUMMY INPUT:")
    print("-" * 80)
    
    # Create dummy inputs based on model specs
    dummy_inputs = []
    for input_detail in input_details:
        shape = input_detail['shape']
        dtype = input_detail['dtype']
        
        # Create dummy data
        if dtype == tf.int32:
            dummy_data = tf.zeros(shape, dtype=tf.int32).numpy()
        elif dtype == tf.int8:
            dummy_data = tf.zeros(shape, dtype=tf.int8).numpy()
        else:
            dummy_data = tf.zeros(shape, dtype=dtype).numpy()
        
        dummy_inputs.append(dummy_data)
        dtype_name = str(dtype) if hasattr(dtype, '__name__') else str(tf.dtypes.as_dtype(dtype).name)
        print(f"Input {len(dummy_inputs)-1}: shape={shape}, dtype={dtype_name}, sample={dummy_data.flatten()[:5]}")
    
    # Run inference
    try:
        for i, dummy_input in enumerate(dummy_inputs):
            interpreter.set_tensor(input_details[i]['index'], dummy_input)
        
        interpreter.invoke()
        
        # Get outputs
        outputs = []
        for output_detail in output_details:
            output_data = interpreter.get_tensor(output_detail['index'])
            outputs.append(output_data)
            print(f"Output {len(outputs)-1}: shape={output_data.shape}, dtype={output_data.dtype}, sample={output_data.flatten()[:5]}")
        
        print("\n✅ Model inference test successful!")
        
    except Exception as e:
        print(f"\n❌ Inference test failed: {e}")
        import traceback
        traceback.print_exc()
    
    print("\n" + "=" * 80)
    print("SUMMARY:")
    print("=" * 80)
    print(f"Input 0: {input_details[0]['shape']} {input_details[0]['dtype'].name}")
    print(f"Input 1: {input_details[1]['shape']} {input_details[1]['dtype'].name}")
    print(f"Output 0: {output_details[0]['shape']} {output_details[0]['dtype'].name}")
    
    if 'quantization_parameters' in output_details[0]:
        quant = output_details[0]['quantization_parameters']
        print(f"Output Scale: {quant['scales'][0]}")
        print(f"Output Zero Point: {quant['zero_points'][0]}")
    
except Exception as e:
    print(f"❌ Error inspecting model: {e}")
    import traceback
    traceback.print_exc()
    sys.exit(1)

