package com.koshpal_android.koshpalapp.ml

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.io.FileDescriptor

/**
 * SMS Classifier using TFLite INT8 model.
 * 
 * INTEGRATED ML MODULE: This class implements the machine learning module for SMS transaction classification.
 * It uses a TensorFlow Lite INT8 quantized model to classify SMS messages into transaction types.
 * 
 * Features:
 * - Model loading from assets (sms_classifier_int8.tflite)
 * - INT8 input/output quantization for optimized performance
 * - Inference execution using TensorFlow Lite Interpreter
 * - Result interpretation with confidence scores
 * - Fallback handling for model loading failures
 * 
 * The ML model classifies SMS into 5 categories:
 * - debit_transaction: Debit/expense transactions
 * - credit_transaction: Credit/income transactions
 * - otp: One-time passwords
 * - promo: Promotional messages
 * - other: Other non-transaction SMS
 */
class SmsClassifier(context: Context) {
    
    private val interpreter: Interpreter
    private val hashingVectorizer = HashingVectorizer()
    
    // Model input/output details
    private val inputShape: IntArray
    private val outputShape: IntArray
    private val isQuantized: Boolean
    
    // INT8 quantization parameters
    private val inputScale: Float
    private val inputZeroPoint: Int
    private val outputScale: Float
    private val outputZeroPoint: Int
    
    companion object {
        private const val TAG = "SmsClassifier"
        private const val MODEL_FILE = "sms_classifier_int8.tflite"
    }
    
    init {
        // Load model from assets
        val modelFile = loadModelFile(context)
        
        // Configure interpreter options
        val options = Interpreter.Options().apply {
            setNumThreads(2) // Use 2 threads for faster inference
            setUseXNNPACK(true) // Enable XNNPACK for optimized CPU inference
        }
        
        // Create interpreter
        interpreter = Interpreter(modelFile, options)
        
        // Get input/output details
        val inputDetails = interpreter.getInputTensor(0)
        val outputDetails = interpreter.getOutputTensor(0)
        
        inputShape = inputDetails.shape()
        outputShape = outputDetails.shape()
        
        // Check quantization
        val inputQuantization = inputDetails.quantizationParams()
        val outputQuantization = outputDetails.quantizationParams()
        
        isQuantized = inputQuantization.scale != 0f
        
        if (isQuantized) {
            inputScale = inputQuantization.scale
            inputZeroPoint = inputQuantization.zeroPoint.toInt()
            outputScale = outputQuantization.scale
            outputZeroPoint = outputQuantization.zeroPoint.toInt()
        } else {
            inputScale = 1.0f
            inputZeroPoint = 0
            outputScale = 1.0f
            outputZeroPoint = 0
        }
        
        // Validate model
        validateModel()
        
        Log.d(TAG, "✅ SMS Classifier initialized successfully")
    }
    
    /**
     * Load model file from assets.
     */
    private fun loadModelFile(context: Context): ByteBuffer {
        var assetFileDescriptor: AssetFileDescriptor? = null
        try {
            assetFileDescriptor = context.assets.openFd(MODEL_FILE)
            val inputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
            val fileChannel = inputStream.channel
            val startOffset = assetFileDescriptor.startOffset
            val declaredLength = assetFileDescriptor.declaredLength
            val modelBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
            
            return modelBuffer
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to load model: ${e.message}", e)
            throw e
        } finally {
            assetFileDescriptor?.close()
        }
    }
    
    /**
     * Validate model input/output shapes.
     */
    private fun validateModel() {
        require(inputShape.contentEquals(intArrayOf(1, 65536))) {
            "Invalid input shape: expected [1, 65536], got ${inputShape.contentToString()}"
        }
        require(outputShape.contentEquals(intArrayOf(1, 5))) {
            "Invalid output shape: expected [1, 5], got ${outputShape.contentToString()}"
        }
    }
    
    /**
     * Classify SMS text.
     * 
     * @param rawSms Raw SMS text
     * @return SmsInferenceResult with prediction and confidence
     */
    fun classify(rawSms: String): SmsInferenceResult {
        // Step 1: Clean SMS
        val cleaned = SmsTextCleaner.clean(rawSms)
        
        // Step 2: Hash to feature vector
        val features = hashingVectorizer.transform(cleaned)
        
        // Step 3: Prepare input buffer
        val inputBuffer = prepareInputBuffer(features)
        
        // Step 4: Prepare output buffer
        val outputBuffer = prepareOutputBuffer()
        
        // Step 5: Run inference
        interpreter.run(inputBuffer, outputBuffer)
        
        // Step 6: Dequantize and interpret results
        val probabilities = dequantizeOutput(outputBuffer)
        
        // Step 7: Get prediction
        val predictedIndex = probabilities.indices.maxByOrNull { probabilities[it] } ?: 0
        val confidence = probabilities[predictedIndex]
        val label = LabelMapper.getLabel(predictedIndex)
        
        return SmsInferenceResult(
            label = label,
            confidence = confidence,
            probabilities = probabilities.toList(),
            cleanedText = cleaned
        )
    }
    
    /**
     * Prepare input buffer for INT8 model.
     */
    private fun prepareInputBuffer(features: FloatArray): ByteBuffer {
        val inputBuffer = ByteBuffer.allocateDirect(65536 * 1) // 1 byte per feature (INT8)
        inputBuffer.order(ByteOrder.nativeOrder())
        
        // Quantize float features to INT8
        for (i in features.indices) {
            val quantized = (features[i] / inputScale + inputZeroPoint).toInt()
            val clamped = quantized.coerceIn(-128, 127) // INT8 range
            inputBuffer.put(clamped.toByte())
        }
        
        inputBuffer.rewind()
        return inputBuffer
    }
    
    /**
     * Prepare output buffer for INT8 model.
     */
    private fun prepareOutputBuffer(): ByteBuffer {
        val outputBuffer = ByteBuffer.allocateDirect(5 * 1) // 5 classes, 1 byte each (INT8)
        outputBuffer.order(ByteOrder.nativeOrder())
        return outputBuffer
    }
    
    /**
     * Dequantize INT8 output to float probabilities.
     */
    private fun dequantizeOutput(outputBuffer: ByteBuffer): FloatArray {
        outputBuffer.rewind()
        val probabilities = FloatArray(5)
        
        for (i in 0 until 5) {
            val quantized = outputBuffer.get().toInt()
            val dequantized = (quantized - outputZeroPoint) * outputScale
            probabilities[i] = dequantized
        }
        
        // Apply softmax
        return softmax(probabilities)
    }
    
    /**
     * Apply softmax to logits.
     */
    private fun softmax(logits: FloatArray): FloatArray {
        val maxLogit = logits.maxOrNull() ?: 0f
        var sum = 0f
        
        // Subtract max for numerical stability
        val expValues = FloatArray(logits.size)
        for (i in logits.indices) {
            expValues[i] = kotlin.math.exp(logits[i] - maxLogit)
            sum += expValues[i]
        }
        
        // Normalize
        for (i in expValues.indices) {
            expValues[i] /= sum
        }
        
        return expValues
    }
    
    /**
     * Close interpreter and release resources.
     */
    fun close() {
        interpreter.close()
    }
}

