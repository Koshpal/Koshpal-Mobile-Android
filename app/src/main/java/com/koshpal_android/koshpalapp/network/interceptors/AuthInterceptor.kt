package com.koshpal_android.koshpalapp.network.interceptors

import com.koshpal_android.koshpalapp.auth.SessionManager
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val sessionManager: SessionManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        val newRequestBuilder = originalRequest.newBuilder()
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "*/*")

        // Add Authorization header if user is logged in and has a valid token
        sessionManager.getAccessToken()?.let { token ->
            newRequestBuilder.addHeader("Authorization", "Bearer $token")
        }

        val newRequest = newRequestBuilder.build()

        return chain.proceed(newRequest)
    }
}