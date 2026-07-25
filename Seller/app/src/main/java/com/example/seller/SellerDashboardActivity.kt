package com.example.seller

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.seller.databinding.ActivitySellerDashboardBinding

class SellerDashboardActivity : BaseActivity() {

    private lateinit var binding: ActivitySellerDashboardBinding
    private val db   = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val myProducts    = mutableListOf<SellerProduct>()
    private val allProducts   = mutableListOf<SellerProduct>() // for search
    private lateinit var productAdapter: SellerProductAdapter

    private var loadCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySellerDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Status bar appearance
        window.statusBarColor = getColor(R.color.shopix_white)
        androidx.core.view.WindowInsetsControllerCompat(window, window.decorView)
            .isAppearanceLightStatusBars = true

        binding.progressBar.visibility = View.VISIBLE

        setupDrawer()
        setupProductRecyclerView()
        setupSearch()
        setupClickableCards()
        loadSellerProfile()
        loadMyProducts()
        loadDashboardStats()
    }

    private fun onLoadDone() {
        loadCount++
        if (loadCount >= 2) {
            binding.progressBar.visibility = View.GONE
        }
    }

    private fun setupClickableCards() {
        binding.cardProducts.setOnClickListener {
            binding.rvProducts.smoothScrollToPosition(0)
        }

        binding.cardTotalOrders.setOnClickListener {
            startActivity(Intent(this, SellerOrdersActivity::class.java))
        }

        binding.cardPending.setOnClickListener {
            val intent = Intent(this, SellerOrdersActivity::class.java)
            intent.putExtra("filter", "pending")
            startActivity(intent)
        }

        binding.cardDelivered.setOnClickListener {
            val intent = Intent(this, SellerOrdersActivity::class.java)
            intent.putExtra("filter", "completed")
            startActivity(intent)
        }
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim().lowercase()
                val filtered = if (query.isEmpty()) {
                    allProducts
                } else {
                    allProducts.filter {
                        it.name.lowercase().contains(query) ||
                                it.skuId.lowercase().contains(query) ||
                                it.category.lowercase().contains(query)
                    }
                }
                productAdapter.updateList(filtered)
                binding.txtProducts.text = filtered.size.toString()
            }
        })

        binding.btnSearch.setOnClickListener {
            val query = binding.etSearch.text.toString().trim().lowercase()
            val filtered = if (query.isEmpty()) allProducts
            else allProducts.filter {
                it.name.lowercase().contains(query) || it.skuId.lowercase().contains(query)
            }
            productAdapter.updateList(filtered)
            binding.txtProducts.text = filtered.size.toString()
        }
    }

    private fun loadSellerProfile() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("sellers").document(uid).get()
            .addOnSuccessListener { doc ->
                val name = doc.getString("name") ?: "Seller"
                val businessName = doc.getString("businessName") ?: name
                val email = doc.getString("email") ?: ""

                // Dashboard Header
                binding.txtSellerName.text = "Welcome, $name 👋"
                binding.txtSubtitle.text = "Here's your business overview"

                // Drawer Header
                binding.txtNavStoreName.text = businessName
                binding.txtNavEmail.text     = email
            }
    }

    private fun loadMyProducts() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("products")
            .whereEqualTo("sellerId", uid)
            .addSnapshotListener { snapshots, error ->
                onLoadDone()
                if (error != null) return@addSnapshotListener
                
                val newList = snapshots?.documents?.map { doc ->
                    SellerProduct(
                        id          = doc.id,
                        name        = doc.getString("name") ?: "",
                        description = doc.getString("description") ?: "",
                        price       = doc.getDouble("price") ?: 0.0,
                        salePrice   = doc.getDouble("salePrice") ?: 0.0,
                        category    = doc.getString("category") ?: "",
                        imageUrl    = doc.getString("imageUrl") ?: "",
                        stock       = (doc.getLong("stock") ?: 0).toInt(),
                        skuId       = doc.getString("skuId") ?: "",
                        sellerId    = uid
                    )
                } ?: emptyList()

                allProducts.clear()
                allProducts.addAll(newList)
                productAdapter.updateList(newList)
                binding.txtProducts.text = allProducts.size.toString()
            }
    }

    private fun loadDashboardStats() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("orders")
            .whereArrayContains("sellerIds", uid)
            .addSnapshotListener { snapshots, _ ->
                onLoadDone()
                val allOrders = snapshots?.documents ?: return@addSnapshotListener
                val total     = allOrders.size
                val pending   = allOrders.count { it.getString("status") == "pending" }
                val delivered = allOrders.count {
                    it.getString("status") == "completed" || it.getString("status") == "delivered"
                }

                var income = 0.0
                allOrders.filter {
                    it.getString("status") == "completed" || it.getString("status") == "delivered"
                }.forEach { doc ->
                    val items = doc.get("items") as? List<*> ?: return@forEach
                    items.forEach { raw ->
                        val item = raw as? Map<*, *> ?: return@forEach
                        if (item["sellerId"] == uid) {
                            val salePrice = (item["salePrice"] as? Double)
                                ?: (item["price"] as? Double) ?: 0.0
                            val qty = ((item["quantity"] as? Long) ?: 1).toInt()
                            income += salePrice * qty * 0.95
                        }
                    }
                }

                binding.txtTotalOrders.text = total.toString()
                binding.txtPending.text     = pending.toString()
                binding.txtDelivered.text   = delivered.toString()
                binding.txtTotalIncome.text = "₹%.2f".format(income)
            }
    }

    private fun setupProductRecyclerView() {
        productAdapter = SellerProductAdapter(
            myProducts,
            onEdit = { product ->
                val intent = Intent(this, EditProductActivity::class.java)
                intent.putExtra("product", product)
                startActivity(intent)
            },
            onDelete = { product -> deleteProduct(product) }
        )
        binding.rvProducts.layoutManager = GridLayoutManager(this, 2)
        binding.rvProducts.adapter = productAdapter
    }

    private fun deleteProduct(product: SellerProduct) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Delete Product")
            .setMessage("Are you sure you want to delete \"${product.name}\"?")
            .setPositiveButton("Delete") { _, _ ->
                db.collection("products").document(product.id).delete()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setupDrawer() {
        // Double-check GravityCompat.END
        binding.btnMenu.setOnClickListener { binding.drawerLayout.openDrawer(GravityCompat.END) }
        binding.btnCloseDrawer.setOnClickListener { binding.drawerLayout.closeDrawers() }
        binding.tvNavDashboard.setOnClickListener { binding.drawerLayout.closeDrawers() }

        binding.tvNavAddProduct.setOnClickListener {
            binding.drawerLayout.closeDrawers()
            startActivity(Intent(this, AddProductActivity::class.java))
        }
        binding.tvNavOrders.setOnClickListener {
            binding.drawerLayout.closeDrawers()
            startActivity(Intent(this, SellerOrdersActivity::class.java))
        }
        binding.tvNavProfile.setOnClickListener {
            binding.drawerLayout.closeDrawers()
            startActivity(Intent(this, SellerProfileActivity::class.java))
        }
        binding.tvNavLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(this, SellerLandingActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }
}