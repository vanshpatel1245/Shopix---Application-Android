package com.example.shopix_admin

import android.content.Intent
import android.os.Bundle
import androidx.core.view.GravityCompat
import com.example.shopix_admin.databinding.ActivityAdminDashboardBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AdminDashboardActivity : BaseActivity() {

    private lateinit var binding: ActivityAdminDashboardBinding
    private val db   = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadLiveStats()
        setupDrawer()
        setupCardClicks()

        binding.swipeRefresh.setOnRefreshListener {
            loadLiveStats()
        }
    }

    // ✅ Count real documents from every collection
    private fun loadLiveStats() {
        binding.swipeRefresh.isRefreshing = true
        var tasksCompleted = 0
        val totalTasks = 5

        fun checkAllDone() {
            tasksCompleted++
            if (tasksCompleted >= totalTasks) {
                binding.swipeRefresh.isRefreshing = false
            }
        }

        // Total users
        db.collection("users").get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                binding.txtTotalUsers.text = (task.result?.size() ?: 0).toString()
            }
            checkAllDone()
        }

        // Total sellers
        db.collection("sellers").get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                binding.txtTotalSellers.text = (task.result?.size() ?: 0).toString()
            }
            checkAllDone()
        }

        // Total products
        db.collection("products").get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                binding.txtTotalProducts.text = (task.result?.size() ?: 0).toString()
            }
            checkAllDone()
        }

        // Orders: total, pending, delivered, revenue
        db.collection("orders").get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val docs = task.result?.documents ?: emptyList()
                val total = docs.size
                val pending = docs.count { it.getString("status") == "pending" }
                val delivered = docs.count {
                    val s = it.getString("status")?.lowercase() ?: ""
                    s == "delivered" || s == "completed"
                }
                var revenue = 0.0
                docs.forEach { doc ->
                    val s = doc.getString("status")?.lowercase() ?: ""
                    if (s == "delivered" || s == "completed") {
                        val amount = when (val raw = doc.get("totalAmount")) {
                            is Number -> raw.toDouble()
                            is String -> raw.toDoubleOrNull() ?: 0.0
                            else -> 0.0
                        }
                        revenue += amount
                    }
                }

                binding.txtTotalOrders.text = total.toString()
                binding.txtPendingOrders.text = pending.toString()
                binding.txtDelivered.text = delivered.toString()
                binding.txtTotalRevenue.text = "₹%.0f".format(revenue)
            }
            checkAllDone()
        }

        // Blocked users
        db.collection("users")
            .whereEqualTo("isBlocked", true)
            .get().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    binding.txtBlockedUsers.text = (task.result?.size() ?: 0).toString()
                }
                checkAllDone()
            }
    }

    private fun setupCardClicks() {
        binding.cardUsers.setOnClickListener    { go(AdminUsersActivity::class.java) }
        binding.cardSellers.setOnClickListener  { go(AdminSellersActivity::class.java) }
        binding.cardProducts.setOnClickListener { go(AdminProductsActivity::class.java) }
        binding.cardOrders.setOnClickListener   { go(AdminOrdersActivity::class.java) }
        
        // Quick Actions
        binding.btnQuickUsers.setOnClickListener    { go(AdminUsersActivity::class.java) }
        binding.btnQuickSellers.setOnClickListener  { go(AdminSellersActivity::class.java) }
        binding.btnQuickOrders.setOnClickListener   { go(AdminOrdersActivity::class.java) }
    }

    private fun setupDrawer() {
        binding.btnMenu.setOnClickListener { binding.drawerLayout.openDrawer(GravityCompat.END) }
        binding.tvNavDashboard.setOnClickListener  { binding.drawerLayout.closeDrawers() }
        binding.tvNavUsers.setOnClickListener      { go(AdminUsersActivity::class.java) }
        binding.tvNavSellers.setOnClickListener    { go(AdminSellersActivity::class.java) }
        binding.tvNavProducts.setOnClickListener   { go(AdminProductsActivity::class.java) }
        binding.tvNavOrders.setOnClickListener     { go(AdminOrdersActivity::class.java) }
        binding.tvNavLogout.setOnClickListener {
            auth.signOut()
            val i = Intent(this, AdminLoginActivity::class.java)
            i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(i)
        }
    }

    private fun go(cls: Class<*>) {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.END)) {
            binding.drawerLayout.closeDrawer(GravityCompat.END)
        }
        startActivity(Intent(this, cls))
    }
}