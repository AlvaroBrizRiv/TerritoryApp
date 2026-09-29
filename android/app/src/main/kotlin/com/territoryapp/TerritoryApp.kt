package com.territoryapp

import android.app.Application
import com.territoryapp.data.api.ApiService
import com.territoryapp.data.api.AuthInterceptor
import com.territoryapp.util.SecurePrefs
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class TerritoryApp : Application() {

    companion object {
        lateinit var instance: TerritoryApp
            private set
        lateinit var apiService: ApiService
            private set
        lateinit var securePrefs: SecurePrefs
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        securePrefs = SecurePrefs(this)

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        val authInterceptor = AuthInterceptor(securePrefs)

        val client = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(authInterceptor)
            .build()

        val retrofit = Retrofit.Builder()
            // Usa 10.0.2.2 si pruebas en el emulador de Android Studio. 
            // Si pruebas en un dispositivo físico, cambia esto a tu IP local (ej: http://192.168.1.X:3000/) o actualiza el túnel Cloudflare.
            .baseUrl("http://192.168.0.72:3000/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiService = retrofit.create(ApiService::class.java)
    }
}
