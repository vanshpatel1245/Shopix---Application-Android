package com.example.shopix_admin

import java.io.Serializable

// ── User (Buyer) ─────────────────────────────────────────────────
data class AdminUser(
    val uid: String = "",
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val address: String = "",
    val city: String = "",
    val state: String = "",
    val isBlocked: Boolean = false,
    val createdAt: String = ""
) : Serializable

// ── Seller ───────────────────────────────────────────────────────
data class AdminSeller(
    val uid: String = "",
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val shopName: String = "",
    val gstin: String = "",
    val isVerified: Boolean = false,
    val isBlocked: Boolean = false,
    val totalIncome: Double = 0.0,
    val createdAt: String = ""
) : Serializable

// ── Product ──────────────────────────────────────────────────────
data class AdminProduct(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val salePrice: Double = 0.0,
    val category: String = "",
    val imageUrl: String = "",
    val stock: Int = 0,
    val skuId: String = "",
    val sellerId: String = "",
    val sellerName: String = "",
    val isApproved: Boolean = true
) : Serializable

// ── Order ────────────────────────────────────────────────────────
data class AdminOrder(
    val orderId: String = "",
    val buyerId: String = "",
    val buyerName: String = "",
    val sellerId: String = "",
    val sellerName: String = "",
    val productName: String = "",
    val productPrice: Double = 0.0,
    val qty: Int = 0,
    val totalAmount: Double = 0.0,
    val paymentId: String = "",
    val paymentStatus: String = "Paid",
    val orderStatus: String = "pending",
    val placedAt: String = ""
) : Serializable