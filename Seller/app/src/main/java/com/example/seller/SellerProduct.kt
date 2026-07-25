package com.example.seller

import java.io.Serializable

data class SellerProduct(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val salePrice: Double = 0.0,
    val category: String = "",
    val imageUrl: String = "",
    val stock: Int = 0,
    val skuId: String = "",
    val sellerId: String = ""
) : Serializable