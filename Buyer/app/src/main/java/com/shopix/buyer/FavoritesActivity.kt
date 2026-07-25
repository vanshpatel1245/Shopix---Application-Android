package com.shopix.buyer

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.shopix.buyer.databinding.ActivityFavoritesBinding

class FavoritesActivity : BaseActivity() {

    private lateinit var binding: ActivityFavoritesBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val favoritesList = mutableListOf<Product>()
    private lateinit var adapter: ProductAdapter
    private var favoritesListener: com.google.firebase.firestore.ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFavoritesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        setupRecyclerView()
        setupBottomNav(binding.bottomNav, R.id.nav_profile)
        setupSwipeRefresh()
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setColorSchemeResources(R.color.shopix_primary)
        binding.swipeRefresh.setOnRefreshListener {
            syncFavorites()
        }
    }

    private fun setupRecyclerView() {
        adapter = ProductAdapter(
            mutableListOf(),  // IMPORTANT: pass empty new list, NOT favoritesList reference
            onProductClick = { product ->
                startActivity(Intent(this, ProductDetailActivity::class.java).putExtra("product", product))
            },
            onFavoriteClick = { product ->
                removeFromFavorites(product)
            }
        )
        binding.rvFavorites.layoutManager = GridLayoutManager(this, 2)
        binding.rvFavorites.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        binding.bottomNav.selectedItemId = R.id.nav_profile
        syncFavorites()  // Only this — real-time listener handles everything
    }

    override fun onPause() {
        super.onPause()
        favoritesListener?.remove()
    }

    private fun syncFavorites() {
        val uid = auth.currentUser?.uid ?: run {
            binding.layoutEmpty.visibility = View.VISIBLE
            binding.rvFavorites.visibility = View.GONE
            return
        }
        favoritesListener?.remove()
        favoritesListener = db.collection("users").document(uid).collection("favorites")
            .addSnapshotListener { snap, error ->
                if (binding.swipeRefresh.isRefreshing) {
                    binding.swipeRefresh.isRefreshing = false
                }

                if (error != null) {
                    android.util.Log.e("FavoritesActivity", "Listener error: ${error.message}")
                    return@addSnapshotListener
                }
                
                val favProducts = snap?.documents?.mapNotNull { doc ->
                    try {
                        // Read all fields explicitly — do NOT use Product.fromFirestore() 
                        // because the saved document may have different field names
                        Product(
                            id          = doc.id,  // Always use doc.id, not doc.getString("id")
                            name        = doc.getString("name") ?: "",
                            price       = doc.getDouble("price") ?: 0.0,
                            oldPrice    = doc.getDouble("oldPrice") ?: 0.0,
                            salePrice   = doc.getDouble("salePrice") ?: 0.0,
                            category    = doc.getString("category") ?: "",
                            description = doc.getString("description") ?: "",
                            imageUrl    = doc.getString("imageUrl") ?: "",
                            stock       = (doc.getLong("stock") ?: 0).toInt(),
                            sellerId    = doc.getString("sellerId") ?: "",
                            gstNo       = doc.getString("gstNo") ?: "",
                            skuId       = doc.getString("skuId") ?: "",
                            isFavorite  = true
                        )
                    } catch (e: Exception) {
                        android.util.Log.e("FavoritesActivity", "Error parsing: ${doc.id}", e)
                        null
                    }
                } ?: emptyList()

                android.util.Log.d("FavoritesActivity", "syncFavorites: got ${favProducts.size} products")

                // Update UI on main thread with a completely new list
                favoritesList.clear()
                favoritesList.addAll(favProducts)
                
                // Create a fresh copy to avoid reference sharing issues
                val displayList = ArrayList(favProducts)
                
                if (::adapter.isInitialized) {
                    adapter.updateList(displayList)
                    android.util.Log.d("FavoritesActivity", "Adapter updated with ${displayList.size} items")
                }
                
                if (favProducts.isEmpty()) {
                    binding.layoutEmpty.visibility = View.VISIBLE
                    binding.rvFavorites.visibility = View.GONE
                } else {
                    binding.layoutEmpty.visibility = View.GONE
                    binding.rvFavorites.visibility = View.VISIBLE
                }
            }
    }

    private fun removeFromFavorites(product: Product) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid)
            .collection("favorites").document(product.id)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "${product.name} removed from favorites", Toast.LENGTH_SHORT).show()
            }
    }
}