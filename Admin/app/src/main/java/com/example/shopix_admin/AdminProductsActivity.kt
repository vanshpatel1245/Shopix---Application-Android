package com.example.shopix_admin

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.shopix_admin.databinding.ActivityAdminProductsBinding
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore

class AdminProductsActivity : BaseActivity() {

    private lateinit var binding: ActivityAdminProductsBinding
    private val db = FirebaseFirestore.getInstance()
    private val products = mutableListOf<AdminProduct>()
    private lateinit var adapter: AdminProductsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminProductsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        adapter = AdminProductsAdapter(products) { product, action ->
            when (action) {
                "approve" -> setProductApproved(product, true)
                "remove"  -> setProductApproved(product, false)
                "delete"  -> deleteProduct(product)
            }
        }
        binding.rvProducts.layoutManager = LinearLayoutManager(this)
        binding.rvProducts.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener {
            loadAllProducts()
        }

        loadAllProducts()
    }

    // ✅ Load ALL products from ALL sellers
    private fun loadAllProducts() {
        binding.swipeRefresh.isRefreshing = true
        db.collection("products")
            .get()
            .addOnSuccessListener { snapshots ->
                binding.swipeRefresh.isRefreshing = false
                
                val newProducts = mutableListOf<AdminProduct>()
                val docs = snapshots?.documents ?: emptyList()
                val sellerIds = docs.mapNotNull { it.getString("sellerId") }.distinct()

                docs.forEach { doc ->
                    newProducts.add(AdminProduct(
                        id          = doc.id,
                        name        = doc.getString("name") ?: "—",
                        description = doc.getString("description") ?: "—",
                        price       = doc.getDouble("price") ?: 0.0,
                        salePrice   = doc.getDouble("salePrice") ?: 0.0,
                        category    = doc.getString("category") ?: "—",
                        imageUrl    = doc.getString("imageUrl") ?: "",
                        stock       = (doc.getLong("stock") ?: 0).toInt(),
                        skuId       = doc.getString("skuId") ?: "—",
                        sellerId    = doc.getString("sellerId") ?: "—",
                        sellerName  = "Seller",   // enriched below
                        isApproved  = doc.getBoolean("isApproved") ?: true
                    ))
                }
                
                products.clear()
                products.addAll(newProducts)
                
                updateUI()

                // ✅ Enrich each product with the seller's business name
                if (sellerIds.isNotEmpty()) {
                    enrichWithSellerNames(sellerIds)
                }
            }
            .addOnFailureListener {
                binding.swipeRefresh.isRefreshing = false
                Toast.makeText(this, "Error loading products", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateUI() {
        binding.txtCount.text = "${products.size} products"
        
        if (products.isEmpty()) {
            binding.layoutEmpty.visibility = View.VISIBLE
            binding.rvProducts.visibility = View.GONE
        } else {
            binding.layoutEmpty.visibility = View.GONE
            binding.rvProducts.visibility = View.VISIBLE
        }
        
        adapter.updateList(products.toList())
    }

    // Fetch seller names and attach to each product row
    private fun enrichWithSellerNames(sellerIds: List<String>) {
        val validIds = sellerIds.filter { it.isNotBlank() }
        if (validIds.isEmpty()) return
        
        val nameMap = mutableMapOf<String, String>()
        val chunks = validIds.chunked(10)
        var completed = 0
        
        chunks.forEach { chunk ->
            // Using FieldPath.documentId() to fetch sellers by their document ID
            db.collection("sellers").whereIn(FieldPath.documentId(), chunk).get()
                .addOnSuccessListener { snap ->
                    snap.documents.forEach { doc ->
                        nameMap[doc.id] = doc.getString("businessName") ?: doc.getString("name") ?: "—"
                    }
                    completed++
                    if (completed == chunks.size) {
                        applySellerNames(nameMap)
                    }
                }
                .addOnFailureListener {
                    completed++
                    if (completed == chunks.size) {
                        applySellerNames(nameMap)
                    }
                }
        }
    }

    private fun applySellerNames(nameMap: Map<String, String>) {
        val enriched = products.map { p ->
            p.copy(sellerName = nameMap[p.sellerId] ?: p.sellerName)
        }
        products.clear()
        products.addAll(enriched)
        updateUI()
    }

    // ✅ Approve or hide a product (isApproved controls visibility in Buyer app)
    private fun setProductApproved(product: AdminProduct, approved: Boolean) {
        db.collection("products").document(product.id)
            .update("isApproved", approved)
            .addOnSuccessListener {
                val msg = if (approved) "${product.name} approved" else "${product.name} removed from listing"
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
    }

    // ✅ Permanently delete product
    private fun deleteProduct(product: AdminProduct) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Delete Product")
            .setMessage("Permanently delete \"${product.name}\"? This cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                db.collection("products").document(product.id).delete()
                    .addOnSuccessListener {
                        Toast.makeText(this, "${product.name} permanently deleted", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
