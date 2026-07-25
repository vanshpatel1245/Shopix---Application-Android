package com.shopix.buyer

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class CartItem(
    val id: String = "",
    val productId: String = "",
    val name: String = "",
    val price: Double = 0.0,
    val salePrice: Double = 0.0,
    val imageUrl: String = "",
    var quantity: Int = 1,
    val sellerId: String = "",
    val skuId: String = "",
    val gstNo: String = ""
) : Parcelable {
    companion object {
        fun fromMap(map: Map<String, Any?>): CartItem {
            return CartItem(
                id = map["id"] as? String ?: "",
                productId = map["productId"] as? String ?: "",
                name = map["name"] as? String ?: "",
                price = (map["price"] as? Number)?.toDouble() ?: 0.0,
                salePrice = (map["salePrice"] as? Number)?.toDouble() ?: 0.0,
                imageUrl = map["imageUrl"] as? String ?: "",
                quantity = (map["quantity"] as? Number)?.toInt() ?: 1,
                sellerId = map["sellerId"] as? String ?: "",
                skuId = map["skuId"] as? String ?: "",
                gstNo = map["gstNo"] as? String ?: ""
            )
        }
    }
}