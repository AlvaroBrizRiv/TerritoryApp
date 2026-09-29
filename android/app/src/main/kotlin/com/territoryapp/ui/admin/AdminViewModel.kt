package com.territoryapp.ui.admin

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.territoryapp.TerritoryApp
import com.territoryapp.data.model.*
import com.territoryapp.data.repository.TerritoryRepository
import kotlinx.coroutines.launch

class AdminViewModel : ViewModel() {
    private val repo = TerritoryRepository(TerritoryApp.apiService)

    val usuarios = MutableLiveData<List<Usuario>>()
    val message = MutableLiveData<String>()

    fun loadUsuarios() {
        viewModelScope.launch {
            try {
                val res = repo.getUsuarios()
                if (res.isSuccessful) usuarios.value = res.body()
            } catch (e: Exception) {}
        }
    }

    fun deleteUsuario(id: Int) {
        viewModelScope.launch {
            try {
                if (repo.deleteUsuario(id).isSuccessful) loadUsuarios()
            } catch (e: Exception) {}
        }
    }

    fun createDireccion(ciudad: String, dir: String, desc: String, lat: Double, lng: Double) {
        viewModelScope.launch {
            try {
                val req = CreateDireccionRequest(ciudad, dir, desc, lat, lng)
                if (repo.createDireccion(req).isSuccessful) {
                    message.value = "Dirección guardada"
                }
            } catch (e: Exception) {
                message.value = "Error guardando dirección"
            }
        }
    }

    fun calcularCluster(ciudad: String, max: Int) {
        viewModelScope.launch {
            try {
                if (repo.calcularCluster(CalcularClusterRequest(ciudad, max)).isSuccessful) {
                    message.value = "Cálculo iniciado"
                }
            } catch (e: Exception) {
                message.value = "Error calculando"
            }
        }
    }
}
