package com.territoryapp.ui.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.territoryapp.TerritoryApp
import com.territoryapp.data.model.Territorio
import com.territoryapp.data.repository.AuthRepository
import com.territoryapp.data.repository.TerritoryRepository
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
    private val authRepo = AuthRepository(TerritoryApp.apiService, TerritoryApp.securePrefs)
    private val terrRepo = TerritoryRepository(TerritoryApp.apiService)

    val isLoggedIn = MutableLiveData<Boolean>()
    val isAdmin = MutableLiveData<Boolean>()
    val territories = MutableLiveData<List<Territorio>>()

    fun checkAuthStatus() {
        isLoggedIn.value = authRepo.isLoggedIn()
        isAdmin.value = authRepo.getUserRol() == "admin"
    }

    fun logout() {
        authRepo.logout()
        checkAuthStatus()
    }

    fun fetchTerritories(lat: Double, lng: Double) {
        viewModelScope.launch {
            try {
                val res = terrRepo.getTerritoriosCercanos(lat, lng, 5000.0)
                if (res.isSuccessful) {
                    territories.value = res.body() ?: emptyList()
                }
            } catch (e: Exception) {
                // Ignore for now
            }
        }
    }
}
