package com.example.seller

import java.io.Serializable

data class SellerOrder(
    val orderId: String = "",
    val productName: String = "",
    val productPrice: Double = 0.0,
    val qty: Int = 0,
    val skuId: String = "",
    val paymentStatus: String = "Paid",
    val orderStatus: String = "pending", // pending, completed, rejected
    val placedAt: String = "",
    val placedAtTimestamp: Long = 0L,
    val buyerId: String = ""
) : Serializable