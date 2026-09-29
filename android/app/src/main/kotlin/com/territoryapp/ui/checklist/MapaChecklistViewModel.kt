package com.territoryapp.ui.checklist

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.territoryapp.TerritoryApp
import com.territoryapp.data.model.Direccion
import com.territoryapp.data.repository.TerritoryRepository
import kotlinx.coroutines.launch

class MapaChecklistViewModel : ViewModel() {
    private val repo = TerritoryRepository(TerritoryApp.apiService)

    val direcciones = MutableLiveData<List<Direccion>>()
    val completarResult = MutableLiveData<Boolean>()

    fun loadDirecciones(id: Int) {
        viewModelScope.launch {
            try {
                val res = repo.getDireccionesTerritorio(id)
                if (res.isSuccessful) {
                    direcciones.value = res.body() ?: emptyList()
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun completarTerritorio(id: Int) {
        viewModelScope.launch {
            try {
                val res = repo.completarTerritorio(id)
                completarResult.value = res.isSuccessful
            } catch (e: Exception) {
                completarResult.value = false
            }
        }
    }
}
