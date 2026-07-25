package com.shopix.buyer

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.shopix.buyer.databinding.ItemOrderBinding

class OrdersAdapter(private var orders: List<Order>) :
    RecyclerView.Adapter<OrdersAdapter.OrderViewHolder>() {

    fun updateList(newList: List<Order>) {
        orders = newList
        notifyDataSetChanged()
    }

    inner class OrderViewHolder(val binding: ItemOrderBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val binding = ItemOrderBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return OrderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val order = orders[position]
        with(holder.binding) {
            txtOrderId.text = "Order ${order.orderId}"
            txtPlacedAt.text = "Placed on: ${order.placedAt}"
            txtPaymentId.text = "Payment ID: ${order.paymentId}"

            // Status badge color
            val statusColor = when (order.status.lowercase()) {
                "paid", "completed" -> Color.parseColor("#22C55E")
                "pending" -> Color.parseColor("#F59E0B")
                "rejected" -> Color.parseColor("#FF4144")
                else -> Color.GRAY
            }
            txtStatus.text = order.status.replaceFirstChar { it.uppercase() }
            txtStatus.setTextColor(statusColor)

            // Nested items RecyclerView
            rvOrderItems.layoutManager = LinearLayoutManager(root.context)
            rvOrderItems.adapter = OrderItemsAdapter(order.items)
            rvOrderItems.isNestedScrollingEnabled = false
        }
    }

    override fun getItemCount() = orders.size
}