package com.shopix.buyer

import android.content.Intent
import android.graphics.Paint
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.shopix.buyer.databinding.ActivityProductDetailBinding
import com.shopix.buyer.databinding.BottomSheetCategoryBinding

class ProductDetailActivity : BaseActivity() {

    private lateinit var binding: ActivityProductDetailBinding
    private val db   = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var quantity = 1
    private var isFavorite = false
    private var isExpanded = false
    private var recommendedListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var favoritesListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var productFavoriteListener: com.google.firebase.firestore.ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProductDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val product = if (android.os.Build.VERSION.SDK_INT >= 33) {
            intent.getSerializableExtra("product", Product::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra("product") as? Product
        }
        if (product == null) { finish(); return }

        bindProduct(product)
        fetchSellerInfo(product.sellerId) 
        setupQtyCounter(product)
        setupFavoriteButton(product)
        loadRecommended(product)
        setupBottomNav(binding.bottomNav, -1) // Corrected: use BaseActivity version

        binding.txtMore.setOnClickListener {
            if (isExpanded) {
                binding.txtDescription.maxLines = 3
                binding.txtMore.text = "more"
            } else {
                binding.txtDescription.maxLines = Int.MAX_VALUE
                binding.txtMore.text = "less"
            }
            isExpanded = !isExpanded
        }

        binding.btnAddToCart.setOnClickListener {
            addToCart(product, quantity)
        }

        binding.btnBuyNow.setOnClickListener {
            addToCart(product, quantity) {
                startActivity(Intent(this, CartActivity::class.java))
            }
        }
    }

    private fun fetchSellerInfo(sellerId: String) {
        if (sellerId.isBlank()) {
            binding.txtGst.text = "Not Available"
            binding.txtSellerId.text = "Not Available"
            binding.imgVerified.visibility = View.GONE
            return
        }

        db.collection("sellers").document(sellerId).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val gst = doc.getString("gstNo") ?: doc.getString("gst_no") ?: ""
                    binding.txtGst.text = if (gst.isNotBlank()) gst else "Not Available"

                    val seqId = doc.getString("sellerId") ?: ""
                    binding.txtSellerId.text = if (seqId.isNotBlank()) seqId else "Not Available"
                    
                    val isVerified = doc.getBoolean("isVerified") ?: false
                    binding.imgVerified.visibility = if (isVerified) View.VISIBLE else View.GONE
                } else {
                    binding.txtGst.text = "Not Available"
                    binding.txtSellerId.text = "Not Available"
                    binding.imgVerified.visibility = View.GONE
                }
            }
            .addOnFailureListener { e ->
                Log.e("ProductDetail", "Error fetching Seller Info: ${e.message}")
                binding.txtGst.text = "Not Available"
                binding.txtSellerId.text = "Not Available"
                binding.imgVerified.visibility = View.GONE
            }
    }

    private fun loadRecommended(product: Product) {
        recommendedListener?.remove()
        recommendedListener = db.collection("products")
            .whereEqualTo("category", product.category)
            .whereEqualTo("isApproved", true)
            .limit(6)
            .addSnapshotListener { snap, error ->
                if (error != null || snap == null) return@addSnapshotListener
                
                val list = snap.documents
                    .filter { it.id != product.id }
                    .map { doc -> Product.fromFirestore(doc) }

                if (binding.rvRecommended.adapter == null) {
                    val recAdapter = ProductAdapter(
                        items = list.toMutableList(),
                        onProductClick = { p ->
                            val intent = Intent(this, ProductDetailActivity::class.java)
                            intent.putExtra("product", p)
                            startActivity(intent)
                        },
                        onFavoriteClick = { p -> toggleFavoriteRecommended(p) }
                    )
                    binding.rvRecommended.layoutManager =
                        androidx.recyclerview.widget.GridLayoutManager(this, 2)
                    binding.rvRecommended.adapter = recAdapter
                }
                
                // Immediately update the list reference and apply current favorites
                recommendedProducts = list
                applyRecommendedFavorites()
                
                // Also start listening for real-time favorite changes
                syncRecommendedFavorites()
            }
    }

    private var recommendedProducts = listOf<Product>()

    private fun applyRecommendedFavorites() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).collection("favorites").get().addOnSuccessListener { snap ->
            val favIds = snap?.documents?.map { it.id }?.toSet() ?: emptySet()
            recommendedProducts.forEach { it.isFavorite = it.id in favIds }
            (binding.rvRecommended.adapter as? ProductAdapter)?.updateList(recommendedProducts)
        }
    }

    private fun syncRecommendedFavorites() {
        val uid = auth.currentUser?.uid ?: return
        favoritesListener?.remove()
        favoritesListener = db.collection("users").document(uid).collection("favorites")
            .addSnapshotListener { snap, _ ->
                val favIds = snap?.documents?.map { it.id }?.toSet() ?: emptySet()
                // ✅ Update existing products instead of creating new ones
                recommendedProducts.forEach { it.isFavorite = it.id in favIds }
                (binding.rvRecommended.adapter as? ProductAdapter)?.updateList(recommendedProducts)
            }
    }

    private fun toggleFavoriteRecommended(product: Product) {
        val uid = auth.currentUser?.uid ?: return
        val favRef = db.collection("users").document(uid).collection("favorites").document(product.id)
        
        // ✅ Toggle local state immediately for visual feedback
        product.isFavorite = !product.isFavorite
        
        // ✅ Update the adapter immediately to show the change
        (binding.rvRecommended.adapter as? ProductAdapter)?.updateList(recommendedProducts)
        
        // ✅ Perform Firebase operation on background thread
        Thread {
            if (product.isFavorite) {
                val favItem = hashMapOf(
                    "name"        to product.name,
                    "price"       to product.price,
                    "oldPrice"    to product.oldPrice,
                    "salePrice"   to product.salePrice,
                    "imageUrl"    to product.imageUrl,
                    "category"    to product.category,
                    "description" to product.description,
                    "stock"       to product.stock,
                    "sellerId"    to product.sellerId,
                    "gstNo"       to product.gstNo,
                    "skuId"       to product.skuId
                )
                favRef.set(favItem)
            } else {
                favRef.delete()
            }
        }.start()
    }

    private fun setupQtyCounter(product: Product) {
        binding.txtQty.text = quantity.toString()

        binding.btnMinus.setOnClickListener {
            if (quantity > 1) {
                quantity--
                binding.txtQty.text = quantity.toString()
            }
        }
        binding.btnPlus.setOnClickListener {
            if (quantity < product.stock) {
                quantity++
                binding.txtQty.text = quantity.toString()
            } else {
                Toast.makeText(this, "Only ${product.stock} in stock", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupFavoriteButton(product: Product) {
        val uid = auth.currentUser?.uid ?: return
        val favRef = db.collection("users").document(uid)
            .collection("favorites").document(product.id)

        productFavoriteListener?.remove()
        productFavoriteListener = favRef.addSnapshotListener { doc, _ ->
            isFavorite = doc?.exists() == true
            product.isFavorite = isFavorite // Sync back to product object
            updateHeartIcon()
        }

        binding.btnFavorite.setOnClickListener {
            it.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
            
            // ✅ Toggle local state and animate
            isFavorite = !isFavorite
            product.isFavorite = isFavorite
            
            binding.btnFavorite.animate()
                .scaleX(1.2f).scaleY(1.2f).setDuration(100)
                .withEndAction {
                    binding.btnFavorite.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()
                }.start()
            
            updateHeartIcon()

            // ✅ Perform Firebase operation on background thread
            Thread {
                if (isFavorite) {
                    val favItem = hashMapOf(
                        "name"        to product.name,
                        "price"       to product.price,
                        "oldPrice"    to product.oldPrice,
                        "salePrice"   to product.salePrice,
                        "imageUrl"    to product.imageUrl,
                        "category"    to product.category,
                        "description" to product.description,
                        "stock"       to product.stock,
                        "sellerId"    to product.sellerId,
                        "gstNo"       to product.gstNo,
                        "skuId"       to product.skuId
                    )
                    favRef.set(favItem)
                } else {
                    favRef.delete()
                }
            }.start()
        }
    }

    private fun updateHeartIcon() {
        binding.btnFavorite.setImageResource(
            if (isFavorite) R.drawable.ic_heart_filled else R.drawable.ic_heart
        )
    }

    private fun addToCart(product: Product, qty: Int, onDone: (() -> Unit)? = null) {
        val uid = auth.currentUser?.uid ?: return
        val cartRef = db.collection("users").document(uid)
            .collection("cart").document(product.id)

        cartRef.get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                val currentQty = (doc.getLong("quantity") ?: 1).toInt()
                cartRef.update("quantity", currentQty + qty)
                    .addOnSuccessListener { onDone?.invoke() }
            } else {
                val cartItem = hashMapOf(
                    "productId" to product.id,
                    "name"      to product.name,
                    "price"     to product.price,
                    "salePrice"  to product.salePrice,
                    "imageUrl"  to product.imageUrl,
                    "quantity"  to qty,
                    "sellerId"  to product.sellerId,
                    "skuId"     to product.skuId,
                    "gstNo"     to product.gstNo
                )
                cartRef.set(cartItem).addOnSuccessListener { onDone?.invoke() }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // No default navigation selection for Detail screen
        val product = if (android.os.Build.VERSION.SDK_INT >= 33) {
            intent.getSerializableExtra("product", Product::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra("product") as? Product
        } ?: return
        loadRecommended(product)
        setupFavoriteButton(product)
    }

    override fun onPause() {
        super.onPause()
        recommendedListener?.remove()
        favoritesListener?.remove()
        productFavoriteListener?.remove()
    }

    private fun bindProduct(product: Product) {
        with(binding) {
            txtProductName.text = product.name
            txtPrice.text       = "₹%.2f".format(product.salePrice)
            txtOldPrice.text    = "₹%.2f".format(product.oldPrice)
            txtOldPrice.paintFlags = txtOldPrice.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG

            txtDescription.text = product.description
            txtCategory.text    = product.category
            
            // Initial placeholder while fetching from seller doc
            txtGst.text = "Loading..."
            
            txtSellerId.text    = "Loading..."
            txtStock.text       = if (product.stock > 0) "${product.stock} pcs" else "Out of Stock"

            Glide.with(this@ProductDetailActivity)
                .load(product.imageUrl)
                .placeholder(R.drawable.ic_launcher_background)
                .centerCrop()
                .into(imgProduct)
            
            // More/Less logic
            isExpanded = false
            txtDescription.maxLines = 3
            txtMore.text = "more"
            
            txtDescription.post {
                // If text is long enough (by chars or lines), show the more button
                if (txtDescription.lineCount >= 3 || (product.description.length > 100)) {
                    txtMore.visibility = View.VISIBLE
                } else {
                    txtMore.visibility = View.GONE
                }
            }
        }
    }
}