package com.shopix.buyer

import com.google.firebase.firestore.DocumentSnapshot
import java.io.Serializable

data class Product(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val oldPrice: Double = 0.0,
    val salePrice: Double = 0.0,
    val category: String = "",
    val imageUrl: String = "",
    val stock: Int = 0,
    val sellerId: String = "",
    val gstNo: String = "",
    val skuId: String = "",
    var isFavorite: Boolean = false
) : Serializable {
    companion object {
        fun fromFirestore(doc: DocumentSnapshot): Product {
            return Product(
                id = doc.id,
                name = doc.getString("name") ?: "",
                price = doc.getDouble("price") ?: 0.0,
                oldPrice = doc.getDouble("oldPrice") ?: 0.0,
                salePrice = doc.getDouble("salePrice") ?: 0.0,
                category = doc.getString("category") ?: "",
                description = doc.getString("description") ?: "",
                imageUrl = doc.getString("imageUrl") ?: "",
                stock = (doc.getLong("stock") ?: 0).toInt(),
                sellerId = doc.getString("sellerId") ?: "",
                gstNo = doc.getString("gstNo") ?: "",
                skuId = doc.getString("skuId") ?: "",
                isFavorite = false
            )
        }
    }
}