package com.territoryapp.ui.checklist

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.territoryapp.data.model.Direccion
import com.territoryapp.databinding.ItemDireccionBinding

class DireccionAdapter(private val onCheckedChange: (Direccion, Boolean) -> Unit) : 
    RecyclerView.Adapter<DireccionAdapter.ViewHolder>() {

    private var items: List<Direccion> = emptyList()

    fun submitList(list: List<Direccion>) {
        items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDireccionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class ViewHolder(private val binding: ItemDireccionBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(dir: Direccion) {
            binding.tvDireccion.text = dir.direccion
            binding.tvDescripcion.text = dir.descripcion ?: ""
            binding.cbVisited.isChecked = dir.isVisited
            
            binding.cbVisited.setOnCheckedChangeListener { _, isChecked ->
                onCheckedChange(dir, isChecked)
            }
        }
    }
}
