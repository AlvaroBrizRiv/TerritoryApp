package com.territoryapp.ui.register

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.territoryapp.TerritoryApp
import com.territoryapp.data.repository.AuthRepository
import kotlinx.coroutines.launch

class RegisterViewModel : ViewModel() {
    private val repo = AuthRepository(TerritoryApp.apiService, TerritoryApp.securePrefs)

    val isLoading = MutableLiveData<Boolean>()
    val error = MutableLiveData<String?>()
    val registerSuccess = MutableLiveData<Boolean>()

    fun register(email: String, pass: String) {
        isLoading.value = true
        error.value = null
        viewModelScope.launch {
            val result = repo.register(email, pass)
            isLoading.value = false
            if (result.isSuccess) {
                registerSuccess.value = true
            } else {
                error.value = "Error al registrarse. El usuario podría ya existir."
            }
        }
    }
}
