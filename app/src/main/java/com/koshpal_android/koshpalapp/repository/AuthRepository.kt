package com.koshpal_android.koshpalapp.repository

import android.app.Activity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.koshpal_android.koshpalapp.data.local.UserPreferences
import com.koshpal_android.koshpalapp.model.User
import com.koshpal_android.koshpalapp.network.NetworkResult
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

class AuthRepository(
    private val userRepository: UserRepository,
    private val userPreferences: UserPreferences
) {
    private val firebaseAuth = FirebaseAuth.getInstance()

    suspend fun sendOTP(
        phoneNumber: String,
        activity: Activity,
        callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks
    ) {
        firebaseAuth.firebaseAuthSettings.setAppVerificationDisabledForTesting(false)

        val options = PhoneAuthOptions.newBuilder(firebaseAuth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    suspend fun verifyOTPAndCreateUser(
        verificationId: String,
        otp: String,
        phoneNumber: String
    ): Result<User> {
        return try {
            // Step 1: Verify OTP with Firebase
            val credential = PhoneAuthProvider.getCredential(verificationId, otp)
            val result = firebaseAuth.signInWithCredential(credential).await()
            val firebaseUser = result.user

            if (firebaseUser != null) {
                // Step 2: Create user in your backend
                when (val apiResult = userRepository.createUser(phoneNumber)) {
                    is NetworkResult.Success -> {
                        // Step 3: Return success with user data
                        val user = User(
                            uid = firebaseUser.uid,
                            phoneNumber = phoneNumber,
                            isVerified = true,
                            apiToken = apiResult.data
                        )
                        Result.success(user)
                    }
                    is NetworkResult.Error -> {
                        // Surface backend error instead of masking it as success
                        Result.failure(Exception("Backend user creation failed: ${apiResult.message}${apiResult.code?.let { " (code: $it)" } ?: ""}"))
                    }
                    is NetworkResult.Loading -> {
                        Result.failure(Exception("Unexpected loading state"))
                    }
                }
            } else {
                Result.failure(Exception("Firebase authentication failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getCurrentUser(): User? {
        val firebaseUser = firebaseAuth.currentUser
        val savedToken = userPreferences.getUserToken()
        val isLoggedIn = userPreferences.isLoggedIn()

        return if (firebaseUser != null && isLoggedIn) {
            User(
                uid = firebaseUser.uid,
                phoneNumber = firebaseUser.phoneNumber ?: userPreferences.getPhoneNumber() ?: "",
                isVerified = true,
                apiToken = savedToken
            )
        } else null
    }

    fun signOut() {
        firebaseAuth.signOut()
        userPreferences.clearUserData()
    }
}