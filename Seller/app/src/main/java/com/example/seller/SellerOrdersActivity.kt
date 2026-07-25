package com.example.seller

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.seller.databinding.ActivitySellerOrdersBinding

class SellerOrdersActivity : BaseActivity() {

    private lateinit var binding: ActivitySellerOrdersBinding
    private val db   = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val allOrders  = mutableListOf<SellerOrder>()
    private val showOrders = mutableListOf<SellerOrder>()
    private lateinit var orderAdapter: SellerOrderAdapter
    private var currentFilter = "all"   // all / pending / completed / rejected

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySellerOrdersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        // ✅ Read filter passed from Dashboard cards
        currentFilter = intent.getStringExtra("filter") ?: "all"

        orderAdapter = SellerOrderAdapter(showOrders) { order, action ->
            updateOrderStatus(order, action)
        }
        binding.rvOrders.layoutManager = LinearLayoutManager(this)
        binding.rvOrders.adapter = orderAdapter

        binding.swipeRefresh.setColorSchemeColors(getColor(R.color.shopix_primary))
        binding.swipeRefresh.setOnRefreshListener {
            loadMyOrders()
        }

        setupFilterTabs()
        setupSearch()
        loadMyOrders()
    }

    // ✅ Filter tabs — Pending / Completed / Rejected / All
    private fun setupFilterTabs() {
        fun selectTab(filter: String) {
            currentFilter = filter
            applyFilter()
            // Visual: highlight selected tab
            val activeColor  = getColor(R.color.shopix_primary)
            val inactiveColor = getColor(android.R.color.darker_gray)
            binding.tabAll.setTextColor(if (filter == "all") activeColor else inactiveColor)
            binding.tabPending.setTextColor(if (filter == "pending") activeColor else inactiveColor)
            binding.tabCompleted.setTextColor(if (filter == "completed") activeColor else inactiveColor)
            binding.tabRejected.setTextColor(if (filter == "rejected") activeColor else inactiveColor)
        }

        binding.tabAll.setOnClickListener       { selectTab("all") }
        binding.tabPending.setOnClickListener   { selectTab("pending") }
        binding.tabCompleted.setOnClickListener { selectTab("completed") }
        binding.tabRejected.setOnClickListener  { selectTab("rejected") }

        // Set initial tab from intent
        selectTab(currentFilter)
    }

    // ✅ Search orders by product name or order ID
    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                applyFilter(s.toString().trim())
            }
        })
    }

    private fun applyFilter(searchQuery: String = binding.etSearch.text.toString().trim()) {
        val query = searchQuery.lowercase()
        val filtered = allOrders.filter { order ->
            val matchesFilter = when (currentFilter) {
                "pending"   -> order.orderStatus == "pending"
                "completed" -> order.orderStatus == "completed"
                "rejected"  -> order.orderStatus == "rejected"
                else        -> true
            }
            val matchesSearch = query.isEmpty() ||
                    order.productName.lowercase().contains(query) ||
                    order.orderId.lowercase().contains(query)
            matchesFilter && matchesSearch
        }
        orderAdapter.updateList(filtered)
    }

    private fun loadMyOrders() {
        val uid = auth.currentUser?.uid ?: return
        binding.swipeRefresh.isRefreshing = true

        db.collection("orders")
            .whereArrayContains("sellerIds", uid)
            .get()
            .addOnSuccessListener { snapshots ->
                allOrders.clear()
                snapshots?.documents?.forEach { doc ->
                    val items = doc.get("items") as? List<*> ?: return@forEach
                    items.forEach { raw ->
                        val item = raw as? Map<*, *> ?: return@forEach
                        if (item["sellerId"] == uid) {
                            val timestamp = doc.getTimestamp("placedAt")
                            val dateStr = timestamp?.toDate()?.let {
                                java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a",
                                    java.util.Locale.getDefault()).format(it)
                            } ?: "—"
                            allOrders.add(SellerOrder(
                                orderId      = doc.id,
                                productName  = item["name"] as? String ?: "",
                                productPrice = (item["price"] as? Double) ?: 0.0,
                                qty          = ((item["quantity"] as? Long) ?: 1).toInt(),
                                skuId        = item["skuId"] as? String ?: "",
                                paymentStatus = "Paid",
                                orderStatus  = doc.getString("status") ?: "pending",
                                placedAt     = dateStr,
                                placedAtTimestamp = timestamp?.toDate()?.time ?: 0L,
                                buyerId      = doc.getString("userId") ?: ""
                            ))
                        }
                    }
                }
                // Sort locally newest first
                allOrders.sortByDescending { it.placedAtTimestamp }
                applyFilter()
                binding.swipeRefresh.isRefreshing = false
            }
            .addOnFailureListener { error ->
                Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                binding.swipeRefresh.isRefreshing = false
            }
    }

    private fun updateOrderStatus(order: SellerOrder, newStatus: String) {
        db.collection("orders").document(order.orderId)
            .update("status", newStatus)
            .addOnSuccessListener {
                Toast.makeText(this, "Order marked as $newStatus", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show()
            }
    }
}