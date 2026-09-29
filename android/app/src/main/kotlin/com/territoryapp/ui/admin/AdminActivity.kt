package com.territoryapp.ui.admin

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.territoryapp.databinding.ActivityAdminBinding

class AdminActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminBinding
    private val viewModel: AdminViewModel by viewModels()
    private lateinit var adapter: UsuarioAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupListeners()
        setupObservers()
        
        viewModel.loadUsuarios()
    }

    private fun setupRecyclerView() {
        adapter = UsuarioAdapter { user ->
            viewModel.deleteUsuario(user.id)
        }
        binding.rvUsuarios.layoutManager = LinearLayoutManager(this)
        binding.rvUsuarios.adapter = adapter
    }

    private fun setupListeners() {
        binding.btnSaveDir.setOnClickListener {
            viewModel.createDireccion(
                binding.etDirCiudad.text.toString(),
                binding.etDirAddress.text.toString(),
                binding.etDirDesc.text.toString(),
                binding.etDirLat.text.toString().toDoubleOrNull() ?: 0.0,
                binding.etDirLng.text.toString().toDoubleOrNull() ?: 0.0
            )
        }

        binding.btnCalcular.setOnClickListener {
            viewModel.calcularCluster(
                binding.etCalcCiudad.text.toString(),
                binding.etCalcMax.text.toString().toIntOrNull() ?: 50
            )
        }
    }

    private fun setupObservers() {
        viewModel.usuarios.observe(this) {
            adapter.submitList(it)
        }
        viewModel.message.observe(this) { msg ->
            if (msg.isNotEmpty()) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
