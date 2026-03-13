package com.pixelmarket.app.presentation.marketplace

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.pixelmarket.app.R
import com.pixelmarket.app.databinding.ItemCategoryBinding

class CategoryAdapter(
    private val categories: List<String>,
    private var selectedCategory: String = "All",
    private val onCategoryClick: (String) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemCategoryBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(category: String) {
            binding.tvCategoryName.text = category
            
            val isSelected = category == selectedCategory
            val context = binding.root.context
            
            binding.cardCategory.setCardBackgroundColor(
                if (isSelected) ContextCompat.getColor(context, R.color.dark_teal)
                else ContextCompat.getColor(context, R.color.surface_card)
            )
            
            binding.tvCategoryName.setTextColor(
                if (isSelected) ContextCompat.getColor(context, R.color.white)
                else ContextCompat.getColor(context, R.color.text_primary)
            )
            
            binding.root.setOnClickListener {
                if (selectedCategory != category) {
                    val oldSelected = selectedCategory
                    selectedCategory = category
                    notifyItemChanged(categories.indexOf(oldSelected))
                    notifyItemChanged(categories.indexOf(selectedCategory))
                    onCategoryClick(category)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(categories[position])
    }

    override fun getItemCount() = categories.size
    
    fun updateSelected(category: String) {
        val oldSelected = selectedCategory
        selectedCategory = category
        notifyItemChanged(categories.indexOf(oldSelected))
        notifyItemChanged(categories.indexOf(selectedCategory))
    }
}
