package com.koshpal_android.koshpalapp.ml

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.util.Log
import org.json.JSONObject
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import kotlin.math.exp
import kotlin.math.max

/**
 * MobileBERT INT8 TFLite Inference Module
 * 
 * Responsibilities:
 * - Load TFLite model once (lazy singleton)
 * - Load tokenizer files from assets
 * - Tokenize SMS text → input_ids + attention_mask
 * - Run INT8 inference
 * - Dequantize outputs
 * - Apply softmax
 * - Apply confidence threshold
 * - Return structured result
 */
class MobileBERTInference private constructor(private val context: Context) {
    
    companion object {
        private const val TAG = "MobileBERTInference"
        private const val MODEL_FILE = "mobilebert_phase1_int8.tflite"
        private const val LABEL_MAPPING_FILE = "label_mapping.json"
        private const val VOCAB_FILE = "tokenizer/vocab.txt"
        private const val MAX_SEQUENCE_LENGTH = 128
        private const val CONFIDENCE_THRESHOLD = 0.60f
        private const val NUM_THREADS = 2
        
        // Special tokens
        private const val TOKEN_PAD = "[PAD]"
        private const val TOKEN_UNK = "[UNK]"
        private const val TOKEN_CLS = "[CLS]"
        private const val TOKEN_SEP = "[SEP]"
        
        @Volatile
        private var INSTANCE: MobileBERTInference? = null
        
        fun getInstance(context: Context): MobileBERTInference {
            return INSTANCE ?: synchronized(this) {
                val instance = MobileBERTInference(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
    
    private var interpreter: Interpreter? = null
    private var vocabMap: Map<String, Int>? = null
    private var labelMapping: Map<Int, String>? = null
    private var isInitialized = false
    
    // Model output quantization parameters (INT8)
    private var outputScale: Float = 1.0f
    private var outputZeroPoint: Int = 0
    
    init {
        try {
            initialize()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to initialize MobileBERT: ${e.message}", e)
        }
    }
    
    /**
     * Initialize model, tokenizer, and label mapping
     */
    private fun initialize() {
        if (isInitialized) return
        
        try {
            // Load vocab
            vocabMap = loadVocab()
            Log.d(TAG, "✅ Loaded vocab: ${vocabMap?.size} tokens")
            
            // Load label mapping
            labelMapping = loadLabelMapping()
            Log.d(TAG, "✅ Loaded label mapping: ${labelMapping?.size} labels")
            
            // Load TFLite model
            interpreter = loadModel()
            Log.d(TAG, "✅ Loaded TFLite model")
            
            // Log input tensor shapes for debugging
            val inputTensor0 = interpreter?.getInputTensor(0)
            val inputTensor1 = interpreter?.getInputTensor(1)
            val outputTensor = interpreter?.getOutputTensor(0)
            
            Log.d(TAG, "📊 Model Input 0: shape=${inputTensor0?.shape()?.contentToString()}, type=${inputTensor0?.dataType()}")
            Log.d(TAG, "📊 Model Input 1: shape=${inputTensor1?.shape()?.contentToString()}, type=${inputTensor1?.dataType()}")
            Log.d(TAG, "📊 Model Output: shape=${outputTensor?.shape()?.contentToString()}, type=${outputTensor?.dataType()}")
            
            // Get output quantization parameters
            if (outputTensor != null) {
                val quantizationParams = outputTensor.quantizationParams()
                outputScale = quantizationParams.scale  // Property, not method
                outputZeroPoint = quantizationParams.zeroPoint.toInt()  // Property, not method
                Log.d(TAG, "📊 Output quantization: scale=$outputScale, zeroPoint=$outputZeroPoint")
            }
            
            isInitialized = true
            Log.d(TAG, "✅ MobileBERT initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Initialization failed: ${e.message}", e)
            isInitialized = false
        }
    }
    
    /**
     * Load vocabulary from assets
     */
    private fun loadVocab(): Map<String, Int> {
        val vocab = mutableMapOf<String, Int>()
        try {
            context.assets.open(VOCAB_FILE).bufferedReader().useLines { lines ->
                lines.forEachIndexed { index, token ->
                    vocab[token.trim()] = index
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to load vocab: ${e.message}", e)
            throw e
        }
        return vocab
    }
    
    /**
     * Load label mapping from assets
     */
    private fun loadLabelMapping(): Map<Int, String> {
        val mapping = mutableMapOf<Int, String>()
        try {
            val jsonString = context.assets.open(LABEL_MAPPING_FILE).bufferedReader().use { it.readText() }
            val jsonObject = JSONObject(jsonString)
            val id2label = jsonObject.getJSONObject("id2label")
            
            id2label.keys().forEach { key ->
                val id = key.toInt()
                val label = id2label.getString(key)
                mapping[id] = label
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to load label mapping: ${e.message}", e)
            throw e
        }
        return mapping
    }
    
    /**
     * Load TFLite model from assets
     */
    private fun loadModel(): Interpreter {
        var assetFileDescriptor: AssetFileDescriptor? = null
        try {
            assetFileDescriptor = context.assets.openFd(MODEL_FILE)
            val inputStream = assetFileDescriptor.createInputStream()
            val fileChannel = inputStream.channel
            val startOffset = assetFileDescriptor.startOffset
            val declaredLength = assetFileDescriptor.declaredLength
            val modelBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
            
            val options = Interpreter.Options().apply {
                setNumThreads(NUM_THREADS)
            }
            
            // Explicitly cast to ByteBuffer to resolve constructor ambiguity
            return Interpreter(modelBuffer as ByteBuffer, options)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to load model: ${e.message}", e)
            throw e
        } finally {
            assetFileDescriptor?.close()
        }
    }
    
    /**
     * Predict transaction type from SMS text
     * 
     * @param smsText Raw SMS message body
     * @return MobileBERTResult with prediction
     */
    fun predict(smsText: String): MobileBERTResult {
        if (!isInitialized || interpreter == null || vocabMap == null || labelMapping == null) {
            Log.w(TAG, "⚠️ MobileBERT not initialized, returning fallback result")
            Log.w(TAG, "   isInitialized: $isInitialized, interpreter: ${interpreter != null}, vocabMap: ${vocabMap != null}, labelMapping: ${labelMapping != null}")
            // Try to initialize if not already done
            if (!isInitialized) {
                try {
                    initialize()
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Failed to initialize on-demand: ${e.message}", e)
                }
            }
            // If still not initialized, return fallback
            if (!isInitialized) {
                return MobileBERTResult(
                    label = "other",
                    confidence = 0.0f,
                    isTransaction = false,
                    rawScores = emptyMap()
                )
            }
        }
        
        try {
            // Tokenize SMS text
            Log.d(TAG, "📝 Original SMS text: ${smsText.take(100)}")
            val tokenIds = tokenize(smsText)
            val attentionMask = createAttentionMask(tokenIds)
            
            // Log tokenization details
            Log.d(TAG, "🔤 Tokenized length: ${tokenIds.size}")
            Log.d(TAG, "🔤 First 20 token IDs: ${tokenIds.take(20).joinToString()}")
            Log.d(TAG, "🔤 Last 10 token IDs: ${tokenIds.takeLast(10).joinToString()}")
            
            // Get model input tensor shapes to verify expected format
            val inputTensor0 = interpreter?.getInputTensor(0)
            val inputTensor1 = interpreter?.getInputTensor(1)
            
            val inputShape0 = inputTensor0?.shape()
            val inputShape1 = inputTensor1?.shape()
            
            Log.d(TAG, "📊 Input tensor 0 shape: ${inputShape0?.contentToString()}, dtype: ${inputTensor0?.dataType()}")
            Log.d(TAG, "📊 Input tensor 1 shape: ${inputShape1?.contentToString()}, dtype: ${inputTensor1?.dataType()}")
            
            // Prepare input arrays - Model expects [batch_size=1, sequence_length=128]
            // TFLite expects Array<IntArray> for INT32 inputs with shape [1, 128]
            val inputIdsArray = Array(1) { IntArray(MAX_SEQUENCE_LENGTH) { i ->
                if (i < tokenIds.size) tokenIds[i] else 0
            } }
            val attentionMaskArray = Array(1) { IntArray(MAX_SEQUENCE_LENGTH) { i ->
                if (i < attentionMask.size) attentionMask[i] else 0
            } }
            
            Log.d(TAG, "📊 Prepared input_ids array: [${inputIdsArray.size}, ${inputIdsArray[0].size}]")
            Log.d(TAG, "📊 Prepared attention_mask array: [${attentionMaskArray.size}, ${attentionMaskArray[0].size}]")
            Log.d(TAG, "📊 Sample input_ids (first 20): ${inputIdsArray[0].take(20).joinToString()}")
            
            // Verify token IDs are in valid range (0 to vocab_size)
            val maxTokenId = inputIdsArray[0].maxOrNull() ?: 0
            val minTokenId = inputIdsArray[0].minOrNull() ?: 0
            val vocabSize = vocabMap?.size ?: 0
            Log.d(TAG, "📊 Token ID range: min=$minTokenId, max=$maxTokenId, vocab_size=$vocabSize")
            if (maxTokenId >= vocabSize) {
                Log.w(TAG, "⚠️ WARNING: Token ID $maxTokenId exceeds vocab size $vocabSize!")
            }
            
            // Prepare output buffer (INT8: shape [1, 5] = 5 bytes)
            // For INT8 quantized output, we need a ByteBuffer with exactly 5 bytes
            val outputBuffer = ByteBuffer.allocateDirect(5)
                .order(ByteOrder.nativeOrder())
            
            // Run inference with arrays for inputs, ByteBuffer for output
            val inputArray = arrayOf(inputIdsArray, attentionMaskArray)
            val outputMap = hashMapOf<Int, Any>()
            outputMap[0] = outputBuffer
            
            Log.d(TAG, "🚀 Running inference with arrays for inputs...")
            
            interpreter?.runForMultipleInputsOutputs(inputArray, outputMap)
            
            Log.d(TAG, "✅ Inference completed")
            
            // Try reading output from tensor directly as fallback
            val outputTensor = interpreter?.getOutputTensor(0)
            val outputShape = outputTensor?.shape()
            Log.d(TAG, "📊 Output tensor shape: ${outputShape?.contentToString()}")
            
            // Dequantize output (INT8 to Float)
            // INT8 values are stored as signed bytes (-128 to 127)
            // TFLite quantization uses: real_value = (quantized_value - zero_point) * scale
            val logits = FloatArray(5)
            outputBuffer.rewind() // Reset position to start
            
            // Read raw INT8 values and dequantize
            val rawValues = IntArray(5)
            for (i in 0 until 5) {
                // Read byte at position i (0-4) - use absolute position
                val quantizedByte = outputBuffer.get(i)
                // Convert signed byte to int (preserving sign: -128 to 127)
                // Java bytes are signed, so -128 to 127
                val quantizedValue = quantizedByte.toInt()
                rawValues[i] = quantizedValue
                // Apply dequantization formula: real_value = (quantized - zero_point) * scale
                logits[i] = (quantizedValue - outputZeroPoint) * outputScale
            }
            
            // If all values are zero point, the model may not be writing correctly
            // This could indicate an issue with the model or input format
            if (rawValues.all { it == outputZeroPoint }) {
                Log.w(TAG, "⚠️ WARNING: All output values are zero point ($outputZeroPoint)")
                Log.w(TAG, "⚠️ This suggests the model isn't writing to the output buffer")
                Log.w(TAG, "⚠️ Possible causes: incorrect input format, model issue, or buffer format")
            }
            
            Log.d(TAG, "📊 Raw INT8 values: ${rawValues.contentToString()}")
            Log.d(TAG, "📊 Dequantized logits: ${logits.contentToString()}")
            
            // Apply softmax
            val probabilities = softmax(logits)
            
            Log.d(TAG, "📊 Probabilities after softmax: ${probabilities.contentToString()}")
            
            // Find max probability and label
            var maxProb = 0.0f
            var maxIndex = 0
            for (i in probabilities.indices) {
                if (probabilities[i] > maxProb) {
                    maxProb = probabilities[i]
                    maxIndex = i
                }
            }
            
            val predictedLabel = labelMapping?.get(maxIndex) ?: "other"
            val isTransaction = (predictedLabel == "debit_transaction" || predictedLabel == "credit_transaction") 
                                && maxProb >= CONFIDENCE_THRESHOLD
            
            Log.d(TAG, "📊 Max probability: $maxProb, Index: $maxIndex, Label: $predictedLabel, Threshold: $CONFIDENCE_THRESHOLD, IsTransaction: $isTransaction")
            
            // Create raw scores map
            val rawScores = mutableMapOf<String, Float>()
            labelMapping?.forEach { (id, label) ->
                if (id < probabilities.size) {
                    rawScores[label] = probabilities[id]
                }
            }
            
            Log.d(TAG, "🤖 ML Prediction: label=$predictedLabel, confidence=$maxProb, isTransaction=$isTransaction")
            
            return MobileBERTResult(
                label = predictedLabel,
                confidence = maxProb,
                isTransaction = isTransaction,
                rawScores = rawScores
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Inference failed: ${e.message}", e)
            return MobileBERTResult(
                label = "other",
                confidence = 0.0f,
                isTransaction = false,
                rawScores = emptyMap()
            )
        }
    }
    
    /**
     * Tokenize SMS text using WordPiece tokenization
     * This is a simplified implementation - for production, consider using a proper HuggingFace tokenizer library
     */
    private fun tokenize(text: String): List<Int> {
        val vocab = vocabMap ?: return emptyList()
        
        // Normalize text: lowercase, trim, handle special characters
        var cleanText = text.lowercase().trim()
        
        // Basic normalization: remove extra whitespace
        cleanText = cleanText.replace(Regex("\\s+"), " ")
        
        // Add [CLS] token at the start
        val tokens = mutableListOf<Int>()
        val clsTokenId = vocab[TOKEN_CLS] ?: 101
        tokens.add(clsTokenId)
        
        // Split into words (simple whitespace split)
        val words = cleanText.split(Regex("\\s+")).filter { it.isNotEmpty() }
        
        for (word in words) {
            // Check if we need to truncate (reserve space for [SEP])
            if (tokens.size >= MAX_SEQUENCE_LENGTH - 1) {
                break
            }
            
            // Try exact word match first
            val wordTokenId = vocab[word]
            if (wordTokenId != null) {
                tokens.add(wordTokenId)
            } else {
                // WordPiece tokenization: split into subwords
                val subwords = wordPieceTokenize(word, vocab)
                tokens.addAll(subwords)
                
                // Truncate if we exceeded max length during subword tokenization
                if (tokens.size >= MAX_SEQUENCE_LENGTH - 1) {
                    // Remove excess tokens
                    while (tokens.size >= MAX_SEQUENCE_LENGTH - 1) {
                        tokens.removeAt(tokens.size - 1)
                    }
                    break
                }
            }
        }
        
        // Add [SEP] token
        val sepTokenId = vocab[TOKEN_SEP] ?: 102
        if (tokens.size < MAX_SEQUENCE_LENGTH) {
            tokens.add(sepTokenId)
        }
        
        // Pad to max length with [PAD] token
        val padTokenId = vocab[TOKEN_PAD] ?: 0
        while (tokens.size < MAX_SEQUENCE_LENGTH) {
            tokens.add(padTokenId)
        }
        
        // Ensure exactly MAX_SEQUENCE_LENGTH tokens
        return tokens.take(MAX_SEQUENCE_LENGTH).toMutableList().apply {
            while (size < MAX_SEQUENCE_LENGTH) {
                add(padTokenId)
            }
        }.take(MAX_SEQUENCE_LENGTH)
    }
    
    /**
     * WordPiece tokenization: split word into subwords
     * This implements a greedy longest-match-first algorithm
     */
    private fun wordPieceTokenize(word: String, vocab: Map<String, Int>): List<Int> {
        val tokens = mutableListOf<Int>()
        val unkTokenId = vocab[TOKEN_UNK] ?: 100
        
        if (word.isEmpty()) {
            return listOf(unkTokenId)
        }
        
        // First try the whole word (without ## prefix)
        val wholeWordId = vocab[word]
        if (wholeWordId != null) {
            return listOf(wholeWordId)
        }
        
        // Greedy longest-match-first algorithm
        var start = 0
        while (start < word.length) {
            var end = word.length
            var found = false
            
            // Try to find the longest matching subword starting from 'start'
            while (start < end) {
                val subword = if (start == 0) {
                    // First subword: no ## prefix
                    word.substring(start, end)
                } else {
                    // Subsequent subwords: add ## prefix
                    "##${word.substring(start, end)}"
                }
                
                val tokenId = vocab[subword]
                if (tokenId != null) {
                    tokens.add(tokenId)
                    start = end
                    found = true
                    break
                }
                end--
            }
            
            if (!found) {
                // If no subword found, use [UNK] for the remaining characters
                tokens.add(unkTokenId)
                break
            }
        }
        
        // If we didn't tokenize anything, return [UNK]
        return if (tokens.isEmpty()) listOf(unkTokenId) else tokens
    }
    
    /**
     * Create attention mask (1 for real tokens, 0 for padding)
     */
    private fun createAttentionMask(tokenIds: List<Int>): List<Int> {
        val padTokenId = vocabMap?.get(TOKEN_PAD) ?: 0
        return tokenIds.map { if (it == padTokenId) 0 else 1 }
    }
    
    /**
     * Apply softmax to logits
     */
    private fun softmax(logits: FloatArray): FloatArray {
        val maxLogit = logits.maxOrNull() ?: 0f
        val expValues = logits.map { exp(it - maxLogit) }
        val sumExp = expValues.sum()
        return expValues.map { (it / sumExp).toFloat() }.toFloatArray()
    }
    
    /**
     * Check if model is ready for inference
     */
    fun isReady(): Boolean {
        return isInitialized && interpreter != null
    }
}

/**
 * Result data class for MobileBERT inference
 */
data class MobileBERTResult(
    val label: String,                    // Predicted label: debit_transaction, credit_transaction, otp, promo, other
    val confidence: Float,                 // Confidence score (0.0-1.0)
    val isTransaction: Boolean,           // True if label is debit_transaction or credit_transaction AND confidence >= 0.60
    val rawScores: Map<String, Float>     // Raw probability scores for all classes
)

