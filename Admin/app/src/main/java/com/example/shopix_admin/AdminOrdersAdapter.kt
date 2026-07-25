package com.example.shopix_admin

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.shopix_admin.databinding.ItemAdminOrderBinding

class AdminOrdersAdapter(
    private val items: MutableList<AdminOrder>
) : RecyclerView.Adapter<AdminOrdersAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemAdminOrderBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemAdminOrderBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val order = items[position]
        with(holder.binding) {
            txtOrderId.text     = "Order #${order.orderId.take(8).uppercase()}..."
            txtPlacedAt.text    = order.placedAt
            txtProduct.text     = order.productName
            txtBuyer.text       = order.buyerName
            txtSeller.text      = order.sellerName
            txtAmount.text      = "₹%.2f".format(order.totalAmount)
            txtQty.text         = "Qty: ${order.qty}"
            txtPaymentId.text   = "Payment ID: ${order.paymentId}"
            txtPayment.text     = order.paymentStatus

            val (color, label) = when (order.orderStatus) {
                "completed" -> Pair(Color.parseColor("#22C55E"), "✅ Completed")
                "rejected"  -> Pair(Color.parseColor("#FF4144"), "❌ Rejected")
                else        -> Pair(Color.parseColor("#F59E0B"), "⏳ Pending")
            }
            txtStatus.text = label
            txtStatus.setTextColor(color)
        }
    }

    override fun getItemCount() = items.size

    fun updateList(newList: List<AdminOrder>) {
        items.clear()
        items.addAll(newList)
        notifyDataSetChanged()
    }
}