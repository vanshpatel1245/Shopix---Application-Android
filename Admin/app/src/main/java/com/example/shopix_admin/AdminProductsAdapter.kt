package com.example.shopix_admin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.shopix_admin.databinding.ItemAdminProductBinding

class AdminProductsAdapter(
    private val items: MutableList<AdminProduct>,
    private val onAction: (AdminProduct, String) -> Unit
) : RecyclerView.Adapter<AdminProductsAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemAdminProductBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemAdminProductBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val product = items[position]
        with(holder.binding) {
            txtName.text       = product.name
            txtCategory.text   = product.category
            txtSeller.text     = "Seller: ${product.sellerName}"
            txtSku.text        = "SKU: ${product.skuId}"
            txtPrice.text      = "₹%.0f".format(product.salePrice)
            txtOldPrice.text   = "₹%.0f".format(product.price)
            txtOldPrice.visibility = if (product.price > product.salePrice) View.VISIBLE else View.GONE
            txtStock.text      = "Stock: ${product.stock}"

            Glide.with(root.context)
                .load(product.imageUrl)
                .placeholder(R.drawable.ic_launcher_foreground) // Use a better placeholder if available
                .into(imgProduct)

            if (product.isApproved) {
                txtStatus.text = "✅ Approved"
                txtStatus.setTextColor(root.context.getColor(R.color.admin_success))
                btnAction.text = "Remove"
                btnAction.backgroundTintList = root.context.getColorStateList(R.color.admin_error)
                btnAction.setOnClickListener { onAction(product, "remove") }
            } else {
                txtStatus.text = "⏳ Pending"
                txtStatus.setTextColor(root.context.getColor(R.color.admin_warning))
                btnAction.text = "Approve"
                btnAction.backgroundTintList = root.context.getColorStateList(R.color.admin_success)
                btnAction.setOnClickListener { onAction(product, "approve") }
            }
        }
    }

    override fun getItemCount() = items.size

    fun updateList(newList: List<AdminProduct>) {
        items.clear()
        items.addAll(newList)
        notifyDataSetChanged()
    }
}