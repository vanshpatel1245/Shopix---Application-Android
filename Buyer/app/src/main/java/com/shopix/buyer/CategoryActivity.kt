package com.shopix.buyer

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.shopix.buyer.databinding.ActivityCategoryBinding
import com.shopix.buyer.databinding.DialogSortBinding

class CategoryActivity : BaseActivity() {

    companion object {
        const val DIALOG_DISMISSAL_DELAY = 100L
    }

    private lateinit var binding: ActivityCategoryBinding
    private lateinit var adapter: ProductAdapter
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val favoritesList = mutableListOf<Product>()
    private var fullProductsList = mutableListOf<Product>()
    private var productsList = mutableListOf<Product>()
    private var productsListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var favoritesListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var currentSortType = 0 // 0: Default, 1: Price Low to High, 2: Price High to Low

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCategoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val category = intent.getStringExtra("category") ?: "All"
        android.util.Log.d("CategoryActivity", "onCreate: Initial category = '$category'")
        
        binding.txtCategoryTitle.text = category
        binding.btnBack.setOnClickListener { finish() }
        binding.btnSort.setOnClickListener { showSortBottomSheet() }

        setupSearch()
        setupRecyclerView()
        setupCategoryBottomNav()
        binding.bottomNav.menu.findItem(R.id.nav_categories)?.isChecked = true  // highlight without triggering listener
        setupSwipeRefresh()
        
        android.util.Log.d("CategoryActivity", "onCreate: Setup completed, loading initial products")
        loadProductsByCategory(category)
    }

    private fun setupCategoryBottomNav() {
        setupCartBadge(binding.bottomNav)
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, HomeActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    })
                    true
                }
                R.id.nav_cart -> {
                    startActivity(Intent(this, CartActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    })
                    true
                }
                R.id.nav_categories -> {
                    showCategoryBottomSheet()
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    })
                    true
                }
                else -> false
            }
        }
        binding.bottomNav.setOnItemReselectedListener { item ->
            if (item.itemId == R.id.nav_categories) {
                showCategoryBottomSheet()
            }
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setColorSchemeResources(R.color.shopix_primary)
        binding.swipeRefresh.setOnRefreshListener {
            val category = intent.getStringExtra("category") ?: "All"
            loadProductsByCategory(category)
        }
    }

    private fun setupSearch() {
        binding.etSearchProducts.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterAndSortProducts(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun filterAndSortProducts(query: String = binding.etSearchProducts.text.toString()) {
        val filteredList = if (query.isEmpty()) {
            fullProductsList.toList()
        } else {
            fullProductsList.filter {
                it.name.contains(query, ignoreCase = true) || 
                it.description.contains(query, ignoreCase = true)
            }
        }

        val sortedList = when (currentSortType) {
            1 -> filteredList.sortedBy { it.salePrice }
            2 -> filteredList.sortedByDescending { it.salePrice }
            else -> filteredList
        }

        productsList.clear()
        productsList.addAll(sortedList)
        
        if (::adapter.isInitialized) {
            adapter.updateList(sortedList)
        }

        binding.layoutEmpty.visibility = if (productsList.isEmpty()) View.VISIBLE else View.GONE
        binding.rvCategoryProducts.visibility = if (productsList.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun showSortBottomSheet() {
        val dialog = BottomSheetDialog(this)
        val sortBinding = DialogSortBinding.inflate(layoutInflater)
        dialog.setContentView(sortBinding.root)

        when (currentSortType) {
            0 -> sortBinding.rbDefault.isChecked = true
            1 -> sortBinding.rbPriceLowHigh.isChecked = true
            2 -> sortBinding.rbPriceHighLow.isChecked = true
        }

        sortBinding.rgSort.setOnCheckedChangeListener { _, checkedId ->
            currentSortType = when (checkedId) {
                R.id.rbPriceLowHigh -> 1
                R.id.rbPriceHighLow -> 2
                else -> 0
            }
            filterAndSortProducts()
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun setupRecyclerView() {
        adapter = ProductAdapter(
            items = mutableListOf(), // Fix: Use a fresh list to avoid shared reference bugs with DiffUtil
            onProductClick = { product ->
                startActivity(Intent(this, ProductDetailActivity::class.java).putExtra("product", product))
            },
            onFavoriteClick = { toggleFavorite(it) }
        )
        binding.rvCategoryProducts.layoutManager = GridLayoutManager(this, 2)
        binding.rvCategoryProducts.adapter = adapter
    }

    private fun loadProductsByCategory(category: String) {
        val trimmedCategory = category.trim()
        android.util.Log.d("CategoryActivity", "Loading for category: '$trimmedCategory'")
        binding.swipeRefresh.isRefreshing = true
        
        // Try with isApproved filter first
        db.collection("products")
            .whereEqualTo("category", trimmedCategory)
            .whereEqualTo("isApproved", true)
            .get()
            .addOnSuccessListener { snapshots ->
                android.util.Log.d("CategoryActivity", "With isApproved filter: ${snapshots.documents.size} docs")
                
                if (snapshots.documents.isNotEmpty()) {
                    processProductResults(snapshots.documents)
                } else {
                    // Fallback: query without isApproved filter
                    // This handles: missing index, or products where isApproved field doesn't exist
                    android.util.Log.d("CategoryActivity", "Trying fallback query without isApproved filter")
                    db.collection("products")
                        .whereEqualTo("category", trimmedCategory)
                        .get()
                        .addOnSuccessListener { snap2 ->
                            android.util.Log.d("CategoryActivity", "Without isApproved filter: ${snap2.documents.size} docs")
                            processProductResults(snap2.documents)
                        }
                        .addOnFailureListener { e ->
                            binding.swipeRefresh.isRefreshing = false
                            android.util.Log.e("CategoryActivity", "Fallback query failed: ${e.message}")
                            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener { e ->
                android.util.Log.e("CategoryActivity", "Primary query failed: ${e.message}")
                // Fallback without isApproved
                db.collection("products")
                    .whereEqualTo("category", trimmedCategory)
                    .get()
                    .addOnSuccessListener { snap2 ->
                        android.util.Log.d("CategoryActivity", "Fallback after failure: ${snap2.documents.size} docs")
                        processProductResults(snap2.documents)
                    }
                    .addOnFailureListener { e2 ->
                        binding.swipeRefresh.isRefreshing = false
                        Toast.makeText(this, "Error loading products", Toast.LENGTH_SHORT).show()
                    }
            }
    }

    private fun processProductResults(documents: List<com.google.firebase.firestore.DocumentSnapshot>) {
        binding.swipeRefresh.isRefreshing = false
        val list = documents.mapNotNull { doc ->
            try {
                Product.fromFirestore(doc)
            } catch (e: Exception) {
                android.util.Log.e("CategoryActivity", "Parse error: ${doc.id}", e)
                null
            }
        }
        android.util.Log.d("CategoryActivity", "Processed ${list.size} products")
        fullProductsList.clear()
        fullProductsList.addAll(list)
        filterAndSortProducts()
        applyFavorites()
    }

    private fun applyFavorites() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            if (::adapter.isInitialized) adapter.updateList(productsList.toList())
            return
        }
        
        db.collection("users").document(uid).collection("favorites").get().addOnSuccessListener { snap ->
            val favIds = snap?.documents?.map { it.id }?.toSet() ?: emptySet()
            fullProductsList.forEach { it.isFavorite = it.id in favIds }
            productsList.forEach { it.isFavorite = it.id in favIds }
            if (::adapter.isInitialized) adapter.updateList(productsList.toList())
        }.addOnFailureListener { e ->
            if (::adapter.isInitialized) adapter.updateList(productsList.toList())
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        android.util.Log.d("CategoryActivity", "onNewIntent: CALLED - New intent received")
        
        // Update the intent when a new category is selected
        this.intent = intent
        val category = intent.getStringExtra("category") ?: "All"
        android.util.Log.d("CategoryActivity", "onNewIntent: Updated category to: '$category'")
        
        // Load products for the new category immediately
        android.util.Log.d("CategoryActivity", "onNewIntent: Loading products for new category: $category")
        loadProductsByCategory(category)
    }

    override fun onResume() {
        super.onResume()
        syncFavorites()
    }

    override fun onPause() {
        super.onPause()
        productsListener?.remove()
        favoritesListener?.remove()
    }

    private fun syncFavorites() {
        val uid = auth.currentUser?.uid ?: return
        favoritesListener?.remove()
        favoritesListener = db.collection("users").document(uid).collection("favorites")
            .addSnapshotListener { snap, _ ->
                val favIds = snap?.documents?.map { it.id }?.toSet() ?: emptySet()
                android.util.Log.d("CategoryActivity", "Sync favorites: ${favIds.size} favorites found")
                
                fullProductsList.forEach { it.isFavorite = it.id in favIds }
                productsList.forEach { it.isFavorite = it.id in favIds }
                
                if (::adapter.isInitialized) {
                    adapter.updateList(productsList.toList())
                }
            }
    }
}