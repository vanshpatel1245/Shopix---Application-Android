package com.shopix.buyer

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import com.shopix.buyer.databinding.ActivityHomeBinding

class HomeActivity : BaseActivity() {

    private lateinit var binding: ActivityHomeBinding
    private var allProducts = mutableListOf<Product>()
    private lateinit var productAdapter: ProductAdapter
    private var productsListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var favoritesListener: com.google.firebase.firestore.ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupSystemUI()
        initRecyclerView()
        setupBottomNav(binding.bottomNav, R.id.nav_home)
        setupSearch()
        setupSwipeRefresh()
        
        if (intent.getBooleanExtra("show_categories", false)) {
            showCategoryBottomSheet()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra("show_categories", false)) {
            showCategoryBottomSheet()
        }
    }

    private fun setupSystemUI() {
        @Suppress("DEPRECATION")
        window.statusBarColor = getColor(R.color.shopix_bg_default)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
        }
    }

    private fun initRecyclerView() {
        productAdapter = ProductAdapter(
            items = mutableListOf(),
            onProductClick = { product ->
                startActivity(Intent(this, ProductDetailActivity::class.java).putExtra("product", product))
            },
            onFavoriteClick = { product ->
                android.util.Log.d("HomeActivity", "Favorite clicked for ${product.name}: isFavorite before=${product.isFavorite}")
                
                // Find the position of the clicked product in the adapter
                val position = productAdapter.items.indexOfFirst { it.id == product.id }
                android.util.Log.d("HomeActivity", "Found product at position: $position")
                
                // Toggle favorite immediately for visual feedback
                toggleFavorite(product)
                
                android.util.Log.d("HomeActivity", "Favorite clicked for ${product.name}: isFavorite after=${product.isFavorite}")
                
                // Update the specific item immediately for instant visual feedback
                if (position >= 0) {
                    productAdapter.items[position] = product
                    productAdapter.notifyItemChanged(position)
                    android.util.Log.d("HomeActivity", "Notified item changed at position: $position")
                }
                
                // Also update the master list for consistency
                val masterIndex = allProducts.indexOfFirst { it.id == product.id }
                if (masterIndex >= 0) {
                    allProducts[masterIndex] = product
                }
            }
        )
        binding.rvProducts.layoutManager = GridLayoutManager(this, 2)
        binding.rvProducts.adapter = productAdapter
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setColorSchemeResources(R.color.shopix_primary)
        binding.swipeRefresh.setOnRefreshListener {
            loadProducts()
        }
    }

    private fun loadProducts() {
        if (!binding.swipeRefresh.isRefreshing) {
            binding.progressBar.visibility = View.VISIBLE
        }
        
        FirebaseFirestore.getInstance().collection("products")
            .whereEqualTo("isApproved", true)
            .get()
            .addOnSuccessListener { snapshots ->
                binding.swipeRefresh.isRefreshing = false
                binding.progressBar.visibility = View.GONE

                val productList = snapshots?.documents?.mapNotNull { doc ->
                    try {
                        Product.fromFirestore(doc)
                    } catch (e: Exception) {
                        android.util.Log.e("HomeActivity", "Error parsing product: ${doc.id}", e)
                        null
                    }
                } ?: emptyList()

                allProducts.clear()
                allProducts.addAll(productList)
                applyFavorites()
            }
            .addOnFailureListener { e ->
                binding.swipeRefresh.isRefreshing = false
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun syncFavorites() {
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return
        favoritesListener?.remove()
        favoritesListener = FirebaseFirestore.getInstance().collection("users").document(uid).collection("favorites")
            .addSnapshotListener { snap, _ ->
                val favIds = snap?.documents?.map { it.id }?.toSet() ?: emptySet()
                android.util.Log.d("HomeActivity", "Sync favorites: ${favIds.size} favorites found")
                
                // Update the favorite status in the master list
                allProducts.forEach { it.isFavorite = it.id in favIds }
                
                // Update adapter items directly to force visual refresh
                productAdapter.items.forEachIndexed { index, product ->
                    val updatedProduct = allProducts.find { it.id == product.id }
                    if (updatedProduct != null && product.isFavorite != updatedProduct.isFavorite) {
                        product.isFavorite = updatedProduct.isFavorite
                        productAdapter.notifyItemChanged(index)
                        android.util.Log.d("HomeActivity", "Updated favorite status for ${product.name}: ${product.isFavorite}")
                    }
                }
            }
    }

    private fun applyFavorites() {
        if (allProducts.isEmpty()) return
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            productAdapter.updateList(allProducts)
            return
        }
        
        FirebaseFirestore.getInstance().collection("users").document(uid)
            .collection("favorites").get().addOnSuccessListener { favSnap ->
                val favIds = favSnap.documents.map { it.id }.toSet()
                
                allProducts.forEach { it.isFavorite = it.id in favIds }
                productAdapter.updateList(allProducts)
            }
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { filterProducts(s.toString()) }
            override fun afterTextChanged(s: Editable?) {}
        })
        binding.btnSearch.setOnClickListener { filterProducts(binding.etSearch.text.toString()) }
    }

    private fun filterProducts(query: String) {
        val filtered = if (query.isBlank()) allProducts
        else allProducts.filter { it.name.contains(query, ignoreCase = true) || it.category.contains(query, ignoreCase = true) }
        productAdapter.updateList(filtered)
    }

    override fun onResume() {
        super.onResume()
        binding.bottomNav.selectedItemId = R.id.nav_home
        
        // Only load products if the list is empty to save Firestore reads
        if (allProducts.isEmpty()) {
            loadProducts()
        }
        syncFavorites()
    }

    override fun onPause() {
        super.onPause()
        productsListener?.remove()
        favoritesListener?.remove()
    }
}