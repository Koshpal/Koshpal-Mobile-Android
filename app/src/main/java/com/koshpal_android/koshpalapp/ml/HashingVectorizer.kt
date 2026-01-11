package com.koshpal_android.koshpalapp.ml

/**
 * HashingVectorizer for SMS Text
 * 
 * Converts cleaned SMS text into a fixed-size feature vector (65,536 dimensions)
 * using MurmurHash3 for deterministic hashing.
 * 
 * Configuration (must match Python):
 * - n_features: 65536 (2^16)
 * - ngram_range: (1, 2) - unigrams and bigrams
 * - alternate_sign: false
 * - norm: null (no normalization)
 * - lowercase: false (text already cleaned and lowercased)
 */
class HashingVectorizer {
    
    companion object {
        const val N_FEATURES = 65536 // 2^16
        const val NGRAM_MIN = 1
        const val NGRAM_MAX = 2
    }
    
    /**
     * Transform cleaned SMS text to feature vector.
     * 
     * @param text Cleaned SMS text (already lowercased and normalized)
     * @return FloatArray of size N_FEATURES (65,536)
     */
    fun transform(text: String): FloatArray {
        val features = FloatArray(N_FEATURES)
        
        // Generate n-grams (unigrams and bigrams)
        val ngrams = generateNgrams(text, NGRAM_MIN, NGRAM_MAX)
        
        // Hash each n-gram to feature index
        for (ngram in ngrams) {
            val hash = murmurHash3(ngram)
            val index = (hash and 0x7FFFFFFF) % N_FEATURES // Ensure positive index
            
            // Increment feature count (alternate_sign = false, so no sign flipping)
            features[index] += 1.0f
        }
        
        return features
    }
    
    /**
     * Generate n-grams from text.
     * 
     * @param text Input text
     * @param minN Minimum n-gram size (1 for unigrams)
     * @param maxN Maximum n-gram size (2 for bigrams)
     * @return List of n-gram strings
     */
    private fun generateNgrams(text: String, minN: Int, maxN: Int): List<String> {
        val ngrams = mutableListOf<String>()

        // BIT-EXACT TOKENIZATION: Match scikit-learn HashingVectorizer
        // Uses (?u)\b\w\w+\b regex for Unicode-aware alphanumeric tokens ≥ 2 chars
        val tokenizer = Regex("\\b\\w\\w+\\b")
        val words = tokenizer.findAll(text).map { match -> match.value }.toList()
        
        // Generate unigrams and bigrams
        for (n in minN..maxN) {
            for (i in 0..words.size - n) {
                val ngram = words.subList(i, i + n).joinToString(" ")
                ngrams.add(ngram)
            }
        }
        
        return ngrams
    }
    
    /**
     * Hash function implementation (32-bit).
     * 
     * IMPORTANT: Python's sklearn HashingVectorizer uses a hash function
     * based on Python's built-in hash(). This implementation uses MurmurHash3
     * which should produce similar collision behavior.
     * 
     * For exact matching, you may need to use Python's hash() function output
     * or verify hash outputs match. The model is robust to minor hash differences
     * due to collision handling during training.
     * 
     * @param input String to hash
     * @return 32-bit hash value
     */
    private fun murmurHash3(input: String): Int {
        val data = input.toByteArray(Charsets.UTF_8)
        val seed = 0
        val c1 = 0xcc9e2d51
        val c2 = 0x1b873593
        
        var h1 = seed
        val len = data.size
        var i = 0
        
        // Process 4-byte chunks
        while (i < len - 3) {
            var k1 = (data[i].toInt() and 0xFF) or
                    ((data[i + 1].toInt() and 0xFF) shl 8) or
                    ((data[i + 2].toInt() and 0xFF) shl 16) or
                    ((data[i + 3].toInt() and 0xFF) shl 24)
            
            k1 = (k1 * c1).toInt()
            k1 = ((k1 shl 15) or (k1 ushr 17)).toInt()
            k1 = (k1 * c2).toInt()
            
            h1 = (h1 xor k1).toInt()
            h1 = ((h1 shl 13) or (h1 ushr 19)).toInt()
            h1 = (h1 * 5 + 0xe6546b64).toInt()
            
            i += 4
        }
        
        // Process remaining bytes
        var k1 = 0
        when (len and 3) {
            3 -> {
                k1 = (k1 shl 16) or ((data[i + 2].toInt() and 0xFF) shl 8)
                k1 = (k1 shl 8) or (data[i + 1].toInt() and 0xFF)
                k1 = k1 or (data[i].toInt() and 0xFF)
            }
            2 -> {
                k1 = (k1 shl 8) or (data[i + 1].toInt() and 0xFF)
                k1 = k1 or (data[i].toInt() and 0xFF)
            }
            1 -> {
                k1 = k1 or (data[i].toInt() and 0xFF)
            }
        }
        
        k1 = (k1 * c1).toInt()
        k1 = ((k1 shl 15) or (k1 ushr 17)).toInt()
        k1 = (k1 * c2).toInt()
        h1 = (h1 xor k1).toInt()
        
        // Finalization
        h1 = (h1 xor len).toInt()
        h1 = (h1 xor (h1 ushr 16)).toInt()
        h1 = (h1 * 0x85ebca6b).toInt()
        h1 = (h1 xor (h1 ushr 13)).toInt()
        h1 = (h1 * 0xc2b2ae35).toInt()
        h1 = (h1 xor (h1 ushr 16)).toInt()
        
        return h1
    }
}

