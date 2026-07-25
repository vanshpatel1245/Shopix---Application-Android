package com.shopix.buyer

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.shopix.buyer.databinding.ActivityOrdersBinding

class OrdersActivity : BaseActivity() {

    private lateinit var binding: ActivityOrdersBinding
    private lateinit var ordersAdapter: OrdersAdapter
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var ordersListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOrdersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
        binding.rvOrders.layoutManager = LinearLayoutManager(this)

        ordersAdapter = OrdersAdapter(emptyList())
        binding.rvOrders.adapter = ordersAdapter

        setupBottomNav(binding.bottomNav, R.id.nav_profile)
        setupSwipeRefresh()
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setColorSchemeResources(R.color.shopix_primary)
        binding.swipeRefresh.setOnRefreshListener {
            loadOrders()
        }
    }

    override fun onResume() {
        super.onResume()
        loadOrders()
    }

    override fun onPause() {
        super.onPause()
        ordersListener?.remove()
    }

    private fun loadOrders() {
        val uid = auth.currentUser?.uid ?: return

        binding.swipeRefresh.isRefreshing = true
        db.collection("orders")
            .whereEqualTo("userId", uid)
            .get()
            .addOnSuccessListener { snapshots ->
                binding.swipeRefresh.isRefreshing = false
                val ordersList = snapshots?.documents?.map { doc ->
                    Order.fromFirestore(doc)
                } ?: emptyList()

                val sorted = ordersList.sortedByDescending { it.placedAtTimestamp }
                ordersAdapter.updateList(sorted)

                val isEmpty = sorted.isEmpty()
                binding.layoutEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
                binding.rvOrders.visibility = if (isEmpty) View.GONE else View.VISIBLE
            }
            .addOnFailureListener { e ->
                binding.swipeRefresh.isRefreshing = false
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}