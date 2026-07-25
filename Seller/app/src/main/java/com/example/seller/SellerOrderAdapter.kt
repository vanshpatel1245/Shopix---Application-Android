package com.example.seller

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.seller.databinding.ItemSellerOrderBinding

class SellerOrderAdapter(
    private val orders: MutableList<SellerOrder>,
    private val onAction: (SellerOrder, String) -> Unit
) : RecyclerView.Adapter<SellerOrderAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemSellerOrderBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSellerOrderBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val order = orders[position]
        with(holder.binding) {
            txtOrderId.text      = "Order #${order.orderId.take(8)}..."
            txtPlacedAt.text     = "Placed on: ${order.placedAt}"
            txtProductName.text  = order.productName
            txtPrice.text        = "₹%.2f".format(order.productPrice)
            txtQty.text          = "Qty: ${order.qty}"
            txtPayment.text      = "Payment: ${order.paymentStatus}"
            txtSku.text          = "SKU: ${order.skuId}"

            // ✅ Status badge — matches your image design
            when (order.orderStatus) {
                "completed" -> {
                    txtStatus.text = "Completed"
                    txtStatus.setTextColor(Color.parseColor("#22C55E"))
                    txtStatus.background = ContextCompat.getDrawable(root.context, R.drawable.bg_status_green)
                }
                "rejected" -> {
                    txtStatus.text = "Rejected"
                    txtStatus.setTextColor(Color.parseColor("#FF4144"))
                    txtStatus.background = ContextCompat.getDrawable(root.context, R.drawable.bg_status_red)
                }
                else -> {
                    txtStatus.text = "Pending"
                    txtStatus.setTextColor(Color.parseColor("#F59E0B"))
                    txtStatus.background = ContextCompat.getDrawable(root.context, R.drawable.bg_status_orange)
                }
            }

            // ✅ KEY FIX — buttons disabled if order already accepted or rejected
            val isPending = order.orderStatus == "pending"

            btnAccept.isEnabled = isPending
            btnReject.isEnabled = isPending

            // Visual: dim buttons when disabled
            btnAccept.alpha = if (isPending) 1.0f else 0.4f
            btnReject.alpha = if (isPending) 1.0f else 0.4f

            if (isPending) {
                btnAccept.setOnClickListener {
                    // ✅ Disable immediately before Firestore responds
                    btnAccept.isEnabled = false
                    btnReject.isEnabled = false
                    btnAccept.alpha = 0.4f
                    btnReject.alpha = 0.4f
                    onAction(order, "completed")
                }
                btnReject.setOnClickListener {
                    btnAccept.isEnabled = false
                    btnReject.isEnabled = false
                    btnAccept.alpha = 0.4f
                    btnReject.alpha = 0.4f
                    onAction(order, "rejected")
                }
            } else {
                btnAccept.setOnClickListener(null)
                btnReject.setOnClickListener(null)
            }
        }
    }

    override fun getItemCount() = orders.size

    fun updateList(newList: List<SellerOrder>) {
        val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = orders.size
            override fun getNewListSize() = newList.size
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return orders[oldItemPosition].orderId == newList[newItemPosition].orderId
            }
            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return orders[oldItemPosition] == newList[newItemPosition]
            }
        })
        orders.clear()
        orders.addAll(newList)
        diffResult.dispatchUpdatesTo(this)
    }
}