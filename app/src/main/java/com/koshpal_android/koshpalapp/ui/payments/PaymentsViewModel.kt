package com.koshpal_android.koshpalapp.ui.payments

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.koshpal_android.koshpalapp.model.PaymentSms
import com.koshpal_android.koshpalapp.repository.SmsRepository
import kotlinx.coroutines.launch

class PaymentsViewModel(application: Application) : AndroidViewModel(application) {

    private val smsRepository = SmsRepository(application)

    private val _paymentSms = MutableLiveData<List<PaymentSms>>()
    val paymentSms: LiveData<List<PaymentSms>> = _paymentSms

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun loadPaymentSms() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                val smsMessages = smsRepository.getPaymentSms()
                _paymentSms.value = smsMessages
            } catch (e: Exception) {
                _error.value = "Failed to load SMS messages: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
