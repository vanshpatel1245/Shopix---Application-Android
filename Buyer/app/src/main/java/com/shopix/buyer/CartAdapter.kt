package com.shopix.buyer

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.shopix.buyer.databinding.ItemCartBinding

class CartAdapter(
    private var items: List<CartItem>,
    private val onRemove: (CartItem) -> Unit,
    private val onQtyChange: (CartItem, Int) -> Unit
) : RecyclerView.Adapter<CartAdapter.CartViewHolder>() {

    inner class CartViewHolder(val binding: ItemCartBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val binding = ItemCartBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return CartViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        val item = items[position]
        with(holder.binding) {
            txtName.text = item.name
            txtPrice.text = "₹%.2f".format(item.salePrice)
            txtQty.text = item.quantity.toString()

            Glide.with(root.context)
                .load(item.imageUrl)
                .placeholder(R.drawable.ic_launcher_background)
                .centerCrop()
                .into(imgProduct)

            btnRemove.setOnClickListener {
                val pos = holder.bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION && pos < items.size) {
                    onRemove(items[pos])
                }
            }

            btnMinus.setOnClickListener {
                val pos = holder.bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION && pos < items.size && items[pos].quantity > 1) {
                    val newQty = items[pos].quantity - 1
                    onQtyChange(items[pos], newQty)
                }
            }

            btnPlus.setOnClickListener {
                val pos = holder.bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION && pos < items.size) {
                    val newQty = items[pos].quantity + 1
                    onQtyChange(items[pos], newQty)
                }
            }
        }
    }

    override fun getItemCount() = items.size

    fun updateList(newList: List<CartItem>) {
        val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = items.size
            override fun getNewListSize() = newList.size
            override fun areItemsTheSame(oldPos: Int, newPos: Int): Boolean {
                return items[oldPos].id == newList[newPos].id
            }
            override fun areContentsTheSame(oldPos: Int, newPos: Int): Boolean {
                return items[oldPos] == newList[newPos]
            }
        })
        items = newList
        diffResult.dispatchUpdatesTo(this)
    }
}
