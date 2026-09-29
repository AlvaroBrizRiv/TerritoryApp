package com.territoryapp.ui.checklist

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.webkit.WebSettings
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.territoryapp.databinding.ActivityMapaChecklistBinding

class MapaChecklistActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMapaChecklistBinding
    private val viewModel: MapaChecklistViewModel by viewModels()
    private lateinit var adapter: DireccionAdapter
    private var isMapMode = true

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapaChecklistBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val territoryId = intent.getIntExtra("TERRITORY_ID", -1)
        if (territoryId == -1) {
            finish()
            return
        }

        binding.tvTitle.text = "Territorio $territoryId"
        
        setupWebView()
        setupRecyclerView()
        setupListeners()
        setupObservers()

        viewModel.loadDirecciones(territoryId)
    }

    private fun setupWebView() {
        binding.webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            cacheMode = WebSettings.LOAD_NO_CACHE
        }
        binding.webView.loadUrl("file:///android_asset/mapview.html")
    }

    private fun setupRecyclerView() {
        adapter = DireccionAdapter { dir, isChecked ->
            dir.isVisited = isChecked
        }
        binding.rvDirecciones.layoutManager = LinearLayoutManager(this)
        binding.rvDirecciones.adapter = adapter
    }

    private fun setupListeners() {
        binding.btnMapMode.setOnClickListener {
            isMapMode = true
            updateModeUI()
        }
        binding.btnChecklistMode.setOnClickListener {
            isMapMode = false
            updateModeUI()
        }
        binding.btnCompletar.setOnClickListener {
            viewModel.completarTerritorio(intent.getIntExtra("TERRITORY_ID", -1))
        }
        binding.toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun updateModeUI() {
        if (isMapMode) {
            binding.webView.visibility = View.VISIBLE
            binding.rvDirecciones.visibility = View.GONE
        } else {
            binding.webView.visibility = View.GONE
            binding.rvDirecciones.visibility = View.VISIBLE
        }
    }

    private fun setupObservers() {
        viewModel.direcciones.observe(this) { list ->
            adapter.submitList(list)
            val json = Gson().toJson(list)
            binding.webView.evaluateJavascript("addMarkers('$json')", null)
            if (list.isNotEmpty()) {
                val center = list.first()
                binding.webView.evaluateJavascript("initMap(${center.lat}, ${center.lng}, 15)", null)
            } else {
                binding.webView.evaluateJavascript("initMap(-34.6037, -58.3816, 12)", null)
            }
        }
        viewModel.completarResult.observe(this) { success ->
            if (success) {
                Toast.makeText(this, "Territorio completado", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Error al completar", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
