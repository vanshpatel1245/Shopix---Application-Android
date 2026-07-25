package com.shopix.buyer

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.shopix.buyer.databinding.ItemProductBinding

class ProductAdapter(
    val items: MutableList<Product>,
    private val onProductClick: (Product) -> Unit,
    private val onFavoriteClick: (Product) -> Unit
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    inner class ProductViewHolder(val binding: ItemProductBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ItemProductBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = items[position]
        with(holder.binding) {
            txtProductName.text = product.name
            txtPrice.text = "₹%.2f".format(product.salePrice)
            txtOldPrice.text = "₹%.2f".format(product.oldPrice)
            txtOldPrice.paintFlags = txtOldPrice.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            txtCategory.text = product.category

            Glide.with(root.context)
                .load(product.imageUrl)
                .placeholder(R.drawable.ic_launcher_background)
                .centerCrop()
                .into(imgProduct)

            // FIX 4: Out of Stock overlay
            val isOOS = product.stock == 0
            overlayOutOfStock.visibility = if (isOOS) View.VISIBLE else View.GONE
            txtOutOfStock.visibility = if (isOOS) View.VISIBLE else View.GONE
            root.isClickable = !isOOS
            root.alpha = if (isOOS) 0.7f else 1.0f

            // FIX 6: Discount % badge
            if (product.oldPrice > product.salePrice && product.oldPrice > 0) {
                val discount = (((product.oldPrice - product.salePrice) / product.oldPrice) * 100).toInt()
                if (discount > 0) {
                    txtDiscount.text = "-$discount%"
                    txtDiscount.visibility = View.VISIBLE
                } else {
                    txtDiscount.visibility = View.GONE
                }
            } else {
                txtDiscount.visibility = View.GONE
            }

            // Debug logging to track heart icon state
            android.util.Log.d("ProductAdapter", "Setting heart icon for ${product.name}: isFavorite=${product.isFavorite}")
            imgFavorite.setImageResource(
                if (product.isFavorite) R.drawable.ic_heart_filled else R.drawable.ic_heart
            )

            imgFavorite.setOnClickListener { 
                it.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                
                // ✅ Animate heart icon
                imgFavorite.animate()
                    .scaleX(1.2f).scaleY(1.2f).setDuration(100)
                    .withEndAction {
                        imgFavorite.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()
                    }.start()

                onFavoriteClick(product) 
            }
            root.setOnClickListener { onProductClick(product) }
        }
    }

    override fun getItemCount(): Int = items.size

    // Called when search filters products
    fun updateList(newList: List<Product>) {
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