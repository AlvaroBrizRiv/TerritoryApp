package com.territoryapp.data.api

import com.territoryapp.util.SecurePrefs
import okhttp3.Interceptor
import okhttp3.Response
import org.json.JSONObject
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

class AuthInterceptor(private val securePrefs: SecurePrefs) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val requestBuilder = originalRequest.newBuilder()
            .header("Bypass-Tunnel-Reminder", "true")

        val token = securePrefs.getToken()
        if (!token.isNullOrEmpty()) {
            requestBuilder.header("Authorization", "Bearer $token")
        }

        var request = requestBuilder.build()
        var response = chain.proceed(request)

        if (response.code == 401 && !securePrefs.getRefreshToken().isNullOrEmpty()) {
            synchronized(this) {
                // Check if another thread already refreshed the token
                val currentToken = securePrefs.getToken()
                if (currentToken != null && currentToken != token) {
                    val newRequest = originalRequest.newBuilder()
                        .header("Bypass-Tunnel-Reminder", "true")
                        .header("Authorization", "Bearer $currentToken")
                        .build()
                    response.close()
                    return chain.proceed(newRequest)
                }

                // Try to refresh token
                val refreshToken = securePrefs.getRefreshToken()
                if (refreshToken != null) {
                    val refreshRequest = okhttp3.Request.Builder()
                        .url(originalRequest.url.newBuilder().encodedPath("/api/auth/refresh").build())
                        .header("Bypass-Tunnel-Reminder", "true")
                        .post("{\"refreshToken\":\"$refreshToken\"}".toRequestBody("application/json".toMediaTypeOrNull()))
                        .build()
                        
                    val client = okhttp3.OkHttpClient()
                    val refreshResponse = client.newCall(refreshRequest).execute()
                    
                    if (refreshResponse.isSuccessful) {
                        refreshResponse.body?.string()?.let { bodyString ->
                            val json = JSONObject(bodyString)
                            val newToken = json.optString("token")
                            val newRefresh = json.optString("refreshToken")
                            if (newToken.isNotEmpty()) {
                                securePrefs.saveTokens(newToken, newRefresh)
                                val newRequest = originalRequest.newBuilder()
                                    .header("Bypass-Tunnel-Reminder", "true")
                                    .header("Authorization", "Bearer $newToken")
                                    .build()
                                response.close()
                                return chain.proceed(newRequest)
                            }
                        }
                    } else {
                        securePrefs.clear()
                    }
                    refreshResponse.close()
                }
            }
        }
        return response
    }
}
