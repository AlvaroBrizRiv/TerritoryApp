package com.territoryapp.data.repository

import com.territoryapp.data.api.ApiService
import com.territoryapp.data.model.*

class TerritoryRepository(private val apiService: ApiService) {
    suspend fun getTerritoriosCercanos(lat: Double, lng: Double, radius: Double) =
        apiService.getTerritoriosCercanos(lat, lng, radius)

    suspend fun getDireccionesTerritorio(id: Int) = apiService.getDireccionesTerritorio(id)
    
    suspend fun completarTerritorio(id: Int) = apiService.completarTerritorio(id)
    
    suspend fun getUsuarios() = apiService.getUsuarios()
    suspend fun createUsuario(req: CreateUsuarioRequest) = apiService.createUsuario(req)
    suspend fun deleteUsuario(id: Int) = apiService.deleteUsuario(id)
    
    suspend fun createDireccion(req: CreateDireccionRequest) = apiService.createDireccion(req)
    suspend fun calcularCluster(req: CalcularClusterRequest) = apiService.calcularCluster(req)
}
