package com.territoryapp.ui.login

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.territoryapp.TerritoryApp
import com.territoryapp.data.repository.AuthRepository
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {
    private val repo = AuthRepository(TerritoryApp.apiService, TerritoryApp.securePrefs)

    val isLoading = MutableLiveData<Boolean>()
    val error = MutableLiveData<String?>()
    val loginSuccess = MutableLiveData<Boolean>()

    fun login(email: String, pass: String) {
        isLoading.value = true
        error.value = null
        viewModelScope.launch {
            val result = repo.login(email, pass)
            isLoading.value = false
            if (result.isSuccess) {
                loginSuccess.value = true
            } else {
                error.value = "Credenciales incorrectas o error de red"
            }
        }
    }

    fun register(email: String, pass: String) {
        isLoading.value = true
        error.value = null
        viewModelScope.launch {
            val result = repo.register(email, pass)
            if (result.isSuccess) {
                // If registration succeeds, log the user in automatically
                val loginResult = repo.login(email, pass)
                isLoading.value = false
                if (loginResult.isSuccess) {
                    loginSuccess.value = true
                } else {
                    error.value = "Registro exitoso, pero falló el inicio de sesión automático"
                }
            } else {
                isLoading.value = false
                error.value = "Error al registrarse. El usuario podría ya existir."
            }
        }
    }
}
