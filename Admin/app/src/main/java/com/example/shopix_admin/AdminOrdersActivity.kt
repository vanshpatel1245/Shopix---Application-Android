package com.example.shopix_admin

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.shopix_admin.databinding.ActivityAdminOrdersBinding
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class AdminOrdersActivity : BaseActivity() {

    private lateinit var binding: ActivityAdminOrdersBinding
    private val db = FirebaseFirestore.getInstance()
    private val masterOrdersList = mutableListOf<AdminOrder>()
    private val displayOrdersList = mutableListOf<AdminOrder>()
    private lateinit var adapter: AdminOrdersAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminOrdersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        // Initialize adapter
        adapter = AdminOrdersAdapter(displayOrdersList)
        binding.rvOrders.layoutManager = LinearLayoutManager(this)
        binding.rvOrders.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener {
            loadAllOrders()
        }

        setupFilters()
        loadAllOrders()
    }

    private fun setupFilters() {
        binding.chipGroupStatus.setOnCheckedStateChangeListener { _, checkedIds ->
            filterOrders()
        }
    }

    private fun filterOrders() {
        val filtered = when (binding.chipGroupStatus.checkedChipId) {
            R.id.chipPending -> masterOrdersList.filter { it.orderStatus.lowercase() == "pending" }
            R.id.chipCompleted -> masterOrdersList.filter { it.orderStatus.lowercase() == "completed" }
            R.id.chipRejected -> masterOrdersList.filter { it.orderStatus.lowercase() == "rejected" }
            else -> masterOrdersList
        }

        displayOrdersList.clear()
        displayOrdersList.addAll(filtered)
        adapter.notifyDataSetChanged()

        binding.txtCount.text = "${displayOrdersList.size} order items"
        binding.layoutEmpty.visibility = if (displayOrdersList.isEmpty()) View.VISIBLE else View.GONE
        binding.rvOrders.visibility = if (displayOrdersList.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun loadAllOrders() {
        binding.swipeRefresh.isRefreshing = true
        db.collection("orders")
            .orderBy("placedAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshots ->
                binding.swipeRefresh.isRefreshing = false
                
                val rawOrders = mutableListOf<AdminOrder>()
                val buyerIds = mutableSetOf<String>()
                val sellerIds = mutableSetOf<String>()

                snapshots?.documents?.forEach { doc ->
                    val rawItems = doc.get("items") as? List<*> ?: return@forEach

                    val timestamp = doc.getTimestamp("placedAt")
                    val dateStr = timestamp?.toDate()?.let {
                        java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a",
                            java.util.Locale.getDefault()).format(it)
                    } ?: "—"

                    val bId = doc.getString("userId") ?: doc.getString("buyerId") ?: ""
                    if (bId.isNotEmpty()) buyerIds.add(bId)

                    rawItems.forEach { raw ->
                        val item = raw as? Map<*, *> ?: return@forEach
                        
                        val sId = item["sellerId"] as? String ?: ""
                        if (sId.isNotEmpty()) sellerIds.add(sId)

                        rawOrders.add(AdminOrder(
                            orderId       = doc.id,
                            buyerId       = bId,
                            buyerName     = "Loading...",
                            sellerId      = sId,
                            sellerName    = "Loading...",
                            productName   = item["name"] as? String ?: "—",
                            productPrice  = (item["price"] as? Double) ?: 0.0,
                            qty           = ((item["quantity"] as? Long) ?: 1).toInt(),
                            totalAmount   = doc.getDouble("totalAmount") ?: 0.0,
                            paymentId     = doc.getString("paymentId") ?: "—",
                            paymentStatus = "Paid",
                            orderStatus   = doc.getString("status") ?: "pending",
                            placedAt      = dateStr
                        ))
                    }
                }

                if (rawOrders.isEmpty()) {
                    masterOrdersList.clear()
                    filterOrders()
                } else {
                    masterOrdersList.clear()
                    masterOrdersList.addAll(rawOrders)
                    enrichData(buyerIds.toList(), sellerIds.toList())
                }
            }
            .addOnFailureListener {
                binding.swipeRefresh.isRefreshing = false
                Toast.makeText(this, "Error loading orders", Toast.LENGTH_SHORT).show()
            }
    }

    private fun enrichData(buyerIds: List<String>, sellerIds: List<String>) {
        val buyerNames = mutableMapOf<String, String>()
        val sellerNames = mutableMapOf<String, String>()
        
        var buyersDone = false
        var sellersDone = false

        fun checkFinished() {
            if (buyersDone && sellersDone) {
                val finalOrders = masterOrdersList.map { order ->
                    order.copy(
                        buyerName = buyerNames[order.buyerId] ?: "Unknown Buyer",
                        sellerName = sellerNames[order.sellerId] ?: "Unknown Seller"
                    )
                }
                masterOrdersList.clear()
                masterOrdersList.addAll(finalOrders)
                filterOrders()
            }
        }

        if (buyerIds.isEmpty()) {
            buyersDone = true
            checkFinished()
        } else {
            val chunks = buyerIds.chunked(10)
            var count = 0
            chunks.forEach { chunk ->
                db.collection("users").whereIn(FieldPath.documentId(), chunk).get()
                    .addOnSuccessListener { snap ->
                        snap.documents.forEach { buyerNames[it.id] = it.getString("name") ?: it.getString("fullName") ?: "—" }
                        count++
                        if (count == chunks.size) {
                            buyersDone = true
                            checkFinished()
                        }
                    }
                    .addOnFailureListener {
                        count++
                        if (count == chunks.size) {
                            buyersDone = true
                            checkFinished()
                        }
                    }
            }
        }

        if (sellerIds.isEmpty()) {
            sellersDone = true
            checkFinished()
        } else {
            val chunks = sellerIds.chunked(10)
            var count = 0
            chunks.forEach { chunk ->
                db.collection("sellers").whereIn(FieldPath.documentId(), chunk).get()
                    .addOnSuccessListener { snap ->
                        snap.documents.forEach { sellerNames[it.id] = it.getString("businessName") ?: it.getString("name") ?: "—" }
                        count++
                        if (count == chunks.size) {
                            sellersDone = true
                            checkFinished()
                        }
                    }
                    .addOnFailureListener {
                        count++
                        if (count == chunks.size) {
                            sellersDone = true
                            checkFinished()
                        }
                    }
            }
        }
    }
}