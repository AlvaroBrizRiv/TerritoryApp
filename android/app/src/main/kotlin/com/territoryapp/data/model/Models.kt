package com.territoryapp.data.model

import com.google.gson.annotations.SerializedName

data class LoginRequest(val email: String, val password: String)
data class RefreshRequest(val refreshToken: String)
data class LogoutRequest(val refreshToken: String)

data class AuthResponse(
    val token: String,
    val refreshToken: String,
    val user: UserInfo
)

data class RefreshResponse(
    val token: String,
    val refreshToken: String
)

data class UserInfo(
    val id: Int,
    val email: String,
    val rol: String
)

data class Territorio(
    val id: Int,
    val numero: Int,
    val ciudad: String,
    val estado: String,
    @SerializedName("fecha_creacion") val fechaCreacion: String?,
    val geom: String?,
    @SerializedName("total_direcciones") val totalDirecciones: Int?
)

data class Direccion(
    val id: Int,
    val direccion: String,
    val descripcion: String?,
    @SerializedName("fecha_ingreso") val fechaIngreso: String?,
    val lat: Double,
    val lng: Double,
    var isVisited: Boolean = false
)

data class EstadoRequest(val estado: String)

data class CalcularClusterRequest(
    val ciudad: String,
    val maxDireccionesPorTerritorio: Int
)

data class CreateDireccionRequest(
    val ciudad: String,
    val direccion: String,
    val descripcion: String,
    val lat: Double,
    val lng: Double
)

data class Usuario(
    val id: Int,
    val email: String,
    val rol: String,
    val activo: Boolean
)

data class CreateUsuarioRequest(
    val email: String,
    val password: String,
    val rol: String
)
