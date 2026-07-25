package com.shopix.buyer

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.shopix.buyer.databinding.ItemOrderProductBinding

class OrderItemsAdapter(private val items: List<CartItem>) :
    RecyclerView.Adapter<OrderItemsAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemOrderProductBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemOrderProductBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        with(holder.binding) {
            txtName.text     = item.name
            // Show salePrice if available, else price
            val displayPrice = if (item.salePrice > 0) item.salePrice else item.price
            txtPrice.text    = "₹%.2f".format(displayPrice)
            txtQty.text      = "Qty: ${item.quantity}"
            txtSellerId.text = "Seller ID: ${item.sellerId}"
            
            // Load product image using Glide
            Glide.with(root.context)
                .load(item.imageUrl)
                .placeholder(R.drawable.ic_launcher_background)
                .centerCrop()
                .into(imgProduct)
        }
    }

    override fun getItemCount() = items.size
}