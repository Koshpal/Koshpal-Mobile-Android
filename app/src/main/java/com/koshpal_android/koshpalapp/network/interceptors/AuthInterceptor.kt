package com.koshpal_android.koshpalapp.network.interceptors

import android.content.Context
import com.koshpal_android.koshpalapp.data.local.UserPreferences
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Get token from SharedPreferences
        // Note: In real app, inject context properly through DI
        val token = getUserToken()

        val newRequest = if (token != null) {
            originalRequest.newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Content-Type", "application/json")
                .build()
        } else {
            originalRequest.newBuilder()
                .addHeader("Content-Type", "application/json")
                .build()
        }

        return chain.proceed(newRequest)
    }

    private fun getUserToken(): String? {
        // This should be injected properly in real app
        // For now, return null - token will be handled in repository
        return null
    }
}