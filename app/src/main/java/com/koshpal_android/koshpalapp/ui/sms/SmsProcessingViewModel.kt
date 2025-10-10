package com.koshpal_android.koshpalapp.ui.sms

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.koshpal_android.koshpalapp.data.local.UserPreferences
import com.koshpal_android.koshpalapp.utils.SMSManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SmsProcessingViewModel @Inject constructor(
    application: Application,
    private val userPreferences: UserPreferences
) : AndroidViewModel(application) {
    
    private val _processingState = MutableStateFlow<SmsProcessingState>(SmsProcessingState.Idle)
    val processingState: StateFlow<SmsProcessingState> = _processingState.asStateFlow()
    
    private val smsManager by lazy { 
        SMSManager(getApplication<Application>().applicationContext) 
    }
    
    fun startProcessing() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d("SmsProcessing", "🚀 Starting SMS processing...")
                
                _processingState.value = SmsProcessingState.Processing(
                    message = "🔍 Scanning your SMS inbox...",
                    details = "Reading messages from last 6 months",
                    smsFound = 0,
                    transactionSms = 0,
                    transactionsCreated = 0
                )
                
                delay(500) // Brief delay for UI
                
                _processingState.value = SmsProcessingState.Processing(
                    message = "📱 Reading SMS messages...",
                    details = "This may take a few moments",
                    smsFound = 0,
                    transactionSms = 0,
                    transactionsCreated = 0
                )
                
                delay(500)
                
                // Process all SMS
                val result = smsManager.processAllSMS()
                
                if (result.success) {
                    Log.d("SmsProcessing", "✅ SMS processing successful")
                    Log.d("SmsProcessing", "   SMS found: ${result.smsFound}")
                    Log.d("SmsProcessing", "   Transaction SMS: ${result.transactionSmsFound}")
                    Log.d("SmsProcessing", "   Transactions created: ${result.transactionsCreated}")
                    
                    _processingState.value = SmsProcessingState.Processing(
                        message = "✨ Creating transactions...",
                        details = "Categorizing and organizing your data",
                        smsFound = result.smsFound,
                        transactionSms = result.transactionSmsFound,
                        transactionsCreated = result.transactionsCreated
                    )
                    
                    delay(1000)
                    
                    _processingState.value = SmsProcessingState.Success(
                        summary = buildSuccessSummary(result),
                        smsFound = result.smsFound,
                        transactionSms = result.transactionSmsFound,
                        transactionsCreated = result.transactionsCreated
                    )
                    
                    // Mark as processed
                    userPreferences.setInitialSmsProcessed(true)
                    
                } else {
                    Log.e("SmsProcessing", "❌ SMS processing failed: ${result.error}")
                    _processingState.value = SmsProcessingState.Error(
                        message = result.error ?: "Failed to process SMS"
                    )
                }
                
            } catch (e: Exception) {
                Log.e("SmsProcessing", "❌ Exception during processing", e)
                _processingState.value = SmsProcessingState.Error(
                    message = "Error: ${e.message ?: "Unknown error occurred"}"
                )
            }
        }
    }
    
    fun retryProcessing() {
        _processingState.value = SmsProcessingState.Idle
        startProcessing()
    }
    
    private fun buildSuccessSummary(result: com.koshpal_android.koshpalapp.utils.ProcessResult): String {
        return buildString {
            if (result.transactionsCreated > 0) {
                append("Successfully created ${result.transactionsCreated} transactions ")
                append("from ${result.transactionSmsFound} payment SMS messages!")
            } else if (result.transactionSmsFound > 0) {
                append("Found ${result.transactionSmsFound} payment messages, ")
                append("but they may have been already processed.")
            } else if (result.smsFound > 0) {
                append("Scanned ${result.smsFound} messages, ")
                append("but no new payment transactions were found.")
            } else {
                append("No SMS messages found. You can add transactions manually.")
            }
        }
    }
}

sealed class SmsProcessingState {
    object Idle : SmsProcessingState()
    
    data class Processing(
        val message: String,
        val details: String,
        val smsFound: Int = 0,
        val transactionSms: Int = 0,
        val transactionsCreated: Int = 0
    ) : SmsProcessingState()
    
    data class Success(
        val summary: String,
        val smsFound: Int,
        val transactionSms: Int,
        val transactionsCreated: Int
    ) : SmsProcessingState()
    
    data class Error(
        val message: String
    ) : SmsProcessingState()
    
    object PermissionDenied : SmsProcessingState()
}

