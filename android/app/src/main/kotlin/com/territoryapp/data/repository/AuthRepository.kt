package com.territoryapp.data.repository

import com.territoryapp.data.api.ApiService
import com.territoryapp.data.model.CreateUsuarioRequest
import com.territoryapp.data.model.LoginRequest
import com.territoryapp.util.SecurePrefs

class AuthRepository(private val apiService: ApiService, private val securePrefs: SecurePrefs) {

    suspend fun login(email: String, pass: String): Result<Unit> {
        return try {
            val res = apiService.login(LoginRequest(email, pass))
            if (res.isSuccessful && res.body() != null) {
                val body = res.body()!!
                securePrefs.saveTokens(body.token, body.refreshToken)
                securePrefs.saveUser(body.user.email, body.user.rol)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Error de login"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun register(email: String, pass: String): Result<Unit> {
        return try {
            val request = CreateUsuarioRequest(email, pass, "publicador")
            val res = apiService.register(request)
            if (res.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Error al registrar"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
        securePrefs.clear()
    }

    fun isLoggedIn(): Boolean = securePrefs.getToken() != null
    fun getUserRol(): String? = securePrefs.getUserRol()
    fun getUserEmail(): String? = securePrefs.getUserEmail()
}
