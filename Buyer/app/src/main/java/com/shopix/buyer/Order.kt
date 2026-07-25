package com.shopix.buyer

import com.google.firebase.firestore.DocumentSnapshot
import java.text.SimpleDateFormat
import java.util.Locale

data class Order(
    val orderId: String = "",
    val userId: String = "",
    val items: List<CartItem> = emptyList(),
    val totalAmount: Double = 0.0,
    val status: String = "paid",
    val paymentId: String = "",
    val placedAt: String = "",
    val placedAtTimestamp: Long = 0L
) {
    companion object {
        fun fromFirestore(doc: DocumentSnapshot): Order {
            @Suppress("UNCHECKED_CAST")
            val itemsList = (doc.get("items") as? List<*>)?.mapNotNull { raw ->
                (raw as? Map<String, Any?>)?.let { CartItem.fromMap(it) }
            } ?: emptyList()

            val timestamp = doc.getTimestamp("placedAt")
            val dateStr = timestamp?.toDate()?.let {
                SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(it)
            } ?: "—"

            return Order(
                orderId = doc.id,
                userId = doc.getString("userId") ?: "",
                items = itemsList,
                totalAmount = doc.getDouble("totalAmount") ?: 0.0,
                status = doc.getString("status") ?: "pending",
                paymentId = doc.getString("paymentId") ?: "",
                placedAt = dateStr,
                placedAtTimestamp = timestamp?.toDate()?.time ?: 0L
            )
        }
    }
}