package com.territoryapp.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.territoryapp.data.model.Usuario
import com.territoryapp.databinding.ItemUsuarioBinding

class UsuarioAdapter(private val onDelete: (Usuario) -> Unit) : 
    RecyclerView.Adapter<UsuarioAdapter.ViewHolder>() {

    private var items: List<Usuario> = emptyList()

    fun submitList(list: List<Usuario>) {
        items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemUsuarioBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class ViewHolder(private val binding: ItemUsuarioBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(user: Usuario) {
            binding.tvEmail.text = user.email
            binding.tvRol.text = user.rol
            binding.btnDelete.setOnClickListener { onDelete(user) }
        }
    }
}
