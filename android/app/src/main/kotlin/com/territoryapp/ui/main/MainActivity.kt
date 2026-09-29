package com.territoryapp.ui.main

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.gson.Gson
import com.territoryapp.databinding.ActivityMainBinding
import com.territoryapp.ui.admin.AdminActivity
import com.territoryapp.ui.checklist.MapaChecklistActivity
import com.territoryapp.ui.login.LoginActivity

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            getLastLocation()
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setupWebView()
        setupObservers()
        setupListeners()
        viewModel.checkAuthStatus()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            getLastLocation()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkAuthStatus()
        getLastLocation()
    }

    private fun setupWebView() {
        binding.webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            cacheMode = WebSettings.LOAD_NO_CACHE
        }
        binding.webView.addJavascriptInterface(WebAppInterface(), "Android")
        binding.webView.loadUrl("file:///android_asset/globalmap.html")
    }

    private fun setupObservers() {
        viewModel.isLoggedIn.observe(this) { loggedIn ->
            binding.btnLogin.visibility = if (loggedIn) android.view.View.GONE else android.view.View.VISIBLE
            binding.userMenu.visibility = if (loggedIn) android.view.View.VISIBLE else android.view.View.GONE
        }
        viewModel.isAdmin.observe(this) { admin ->
            binding.btnAdmin.visibility = if (admin) android.view.View.VISIBLE else android.view.View.GONE
        }
        viewModel.territories.observe(this) { list ->
            val json = Gson().toJson(list)
            binding.webView.evaluateJavascript("addTerritories('$json')", null)
        }
    }

    private fun setupListeners() {
        binding.btnLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
        binding.btnLogout.setOnClickListener {
            viewModel.logout()
        }
        binding.btnAdmin.setOnClickListener {
            startActivity(Intent(this, AdminActivity::class.java))
        }
        binding.fabLocation.setOnClickListener {
            getLastLocation()
        }
    }

    @SuppressLint("MissingPermission")
    private fun getLastLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                loc?.let {
                    binding.webView.evaluateJavascript("initGlobalMap(${it.latitude}, ${it.longitude}, 14)", null)
                    viewModel.fetchTerritories(it.latitude, it.longitude)
                } ?: run {
                    // Si 'loc' es nulo, no podemos usar 'it.latitude'. 
                    // Debemos usar unas coordenadas por defecto u obtener la ubicación de otra forma.
                    // Aquí usamos coordenadas por defecto.
                    binding.webView.evaluateJavascript("initGlobalMap(-34.6037, -58.3816, 12)", null)
                }
            }
        }
    }

    inner class WebAppInterface {
        @JavascriptInterface
        fun openTerritory(id: Int) {
            val intent = Intent(this@MainActivity, MapaChecklistActivity::class.java)
            intent.putExtra("TERRITORY_ID", id)
            startActivity(intent)
        }
    }
}
