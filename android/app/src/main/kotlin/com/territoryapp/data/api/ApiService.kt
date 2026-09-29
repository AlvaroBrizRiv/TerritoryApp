package com.territoryapp.data.api

import com.territoryapp.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    @POST("api/auth/register")
    suspend fun register(@Body request: CreateUsuarioRequest): Response<Unit>

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("api/auth/refresh")
    suspend fun refresh(@Body request: RefreshRequest): Response<RefreshResponse>

    @POST("api/auth/logout")
    suspend fun logout(@Body request: LogoutRequest): Response<Unit>

    @GET("api/territorios")
    suspend fun getTerritorios(@Query("ciudad") ciudad: String?): Response<List<Territorio>>

    @GET("api/territorios/cercanos")
    suspend fun getTerritoriosCercanos(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
        @Query("radius") radius: Double
    ): Response<List<Territorio>>

    @GET("api/territorios/{id}/direcciones")
    suspend fun getDireccionesTerritorio(@Path("id") id: Int): Response<List<Direccion>>

    @PATCH("api/territorios/{id}/completar")
    suspend fun completarTerritorio(@Path("id") id: Int): Response<Unit>

    @PATCH("api/territorios/{id}/estado")
    suspend fun updateEstadoTerritorio(@Path("id") id: Int, @Body request: EstadoRequest): Response<Unit>

    @DELETE("api/territorios/{id}")
    suspend fun deleteTerritorio(@Path("id") id: Int): Response<Unit>

    @POST("api/territorios/calcular-cluster")
    suspend fun calcularCluster(@Body request: CalcularClusterRequest): Response<Unit>

    @GET("api/direcciones")
    suspend fun getDirecciones(@Query("ciudad") ciudad: String?, @Query("sinAsignar") sinAsignar: Boolean?): Response<List<Direccion>>

    @POST("api/direcciones")
    suspend fun createDireccion(@Body request: CreateDireccionRequest): Response<Direccion>

    @DELETE("api/direcciones/{id}")
    suspend fun deleteDireccion(@Path("id") id: Int): Response<Unit>

    @GET("api/usuarios")
    suspend fun getUsuarios(): Response<List<Usuario>>

    @POST("api/usuarios")
    suspend fun createUsuario(@Body request: CreateUsuarioRequest): Response<Usuario>

    @DELETE("api/usuarios/{id}")
    suspend fun deleteUsuario(@Path("id") id: Int): Response<Unit>
}
