package com.territoryapp.models

data class UserPrincipal(val userId: Int, val email: String, val rol: String)
data class LoginRequest(val email: String, val password: String)
data class AuthResponse(val token: String, val refreshToken: String, val user: UserInfo)
data class UserInfo(val id: Int, val email: String, val rol: String)
data class RefreshRequest(val refreshToken: String)
data class LogoutRequest(val refreshToken: String?)
data class CreateUsuarioRequest(val email: String, val password: String, val rol: String)
data class CreateDireccionRequest(val ciudad: String, val direccion: String, val descripcion: String?, val lat: Double, val lng: Double)
data class ClusterRequest(val ciudad: String, val maxDireccionesPorTerritorio: Int)
data class EstadoRequest(val estado: String)
data class ActivoRequest(val activo: Boolean)
data class MessageResponse(val message: String)
