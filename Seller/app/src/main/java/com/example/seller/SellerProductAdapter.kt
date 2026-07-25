package com.example.seller

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.seller.R
import com.example.seller.databinding.ItemSellerProductBinding

class SellerProductAdapter(
    private val items: MutableList<SellerProduct>,
    private val onEdit: (SellerProduct) -> Unit,
    private val onDelete: (SellerProduct) -> Unit
) : RecyclerView.Adapter<SellerProductAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemSellerProductBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSellerProductBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val product = items[position]
        with(holder.binding) {
            txtName.text = product.name
            // FIX 3: Show SKU if available, else category
            txtSkuCategory.text = if (product.skuId.isNotBlank()) product.skuId else product.category
            
            txtPrice.text = "₹%.2f".format(product.salePrice)
            txtOldPrice.text = "₹%.2f".format(product.price)
            
            // Fix: Only show old price if it's greater than sale price
            if (product.price > product.salePrice) {
                txtOldPrice.visibility = View.VISIBLE
                txtOldPrice.paintFlags = txtOldPrice.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                txtOldPrice.visibility = View.GONE
            }

            Glide.with(root.context)
                .load(product.imageUrl)
                .placeholder(R.drawable.logo_shopix)
                .centerCrop()
                .into(imgProduct)

            btnEdit.setOnClickListener { onEdit(product) }
            btnDelete.setOnClickListener { onDelete(product) }
        }
    }

    override fun getItemCount() = items.size

    fun updateList(newList: List<SellerProduct>) {
        val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = items.size
            override fun getNewListSize() = newList.size
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return items[oldItemPosition].id == newList[newItemPosition].id
            }
            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return items[oldItemPosition] == newList[newItemPosition]
            }
        })
        items.clear()
        items.addAll(newList)
        diffResult.dispatchUpdatesTo(this)
    }
}