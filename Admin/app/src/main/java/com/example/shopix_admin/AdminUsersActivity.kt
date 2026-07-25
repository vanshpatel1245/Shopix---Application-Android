package com.example.shopix_admin

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.shopix_admin.databinding.ActivityAdminUsersBinding
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale

class AdminUsersActivity : BaseActivity() {

    private lateinit var binding: ActivityAdminUsersBinding
    private val db = FirebaseFirestore.getInstance()
    private val usersList = mutableListOf<AdminUser>()
    private lateinit var adapter: AdminUsersAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminUsersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        // Initialize adapter with a persistent mutable list
        adapter = AdminUsersAdapter(usersList) { user, action ->
            when (action) {
                "block"   -> setUserBlocked(user, true)
                "unblock" -> setUserBlocked(user, false)
            }
        }
        binding.rvUsers.layoutManager = LinearLayoutManager(this)
        binding.rvUsers.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener {
            loadUsers()
        }

        loadUsers()
    }

    private fun loadUsers() {
        binding.swipeRefresh.isRefreshing = true
        db.collection("users")
            .get()
            .addOnSuccessListener { snapshots ->
                binding.swipeRefresh.isRefreshing = false
                
                val newUsers = snapshots?.documents?.mapNotNull { doc ->
                    if (doc.getString("email").isNullOrEmpty()) return@mapNotNull null
                    
                    val timestamp = doc.getTimestamp("createdAt")
                    val dateStr = timestamp?.toDate()?.let {
                        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(it)
                    } ?: "—"

                    AdminUser(
                        uid       = doc.id,
                        fullName  = doc.getString("name") ?: "—",
                        email     = doc.getString("email") ?: "—",
                        phone     = doc.getString("phone") ?: "—",
                        address   = doc.getString("address") ?: "—",
                        city      = doc.getString("city") ?: "—",
                        state     = doc.getString("state") ?: "—",
                        isBlocked = doc.getBoolean("isBlocked") ?: false,
                        createdAt = dateStr
                    )
                } ?: emptyList<AdminUser>()

                binding.txtCount.text = "${newUsers.size} users"
                
                if (newUsers.isEmpty()) {
                    binding.layoutEmpty.visibility = View.VISIBLE
                    binding.rvUsers.visibility = View.GONE
                } else {
                    binding.layoutEmpty.visibility = View.GONE
                    binding.rvUsers.visibility = View.VISIBLE
                }

                adapter.updateList(newUsers)
            }
            .addOnFailureListener {
                binding.swipeRefresh.isRefreshing = false
                Toast.makeText(this, "Error loading users", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setUserBlocked(user: AdminUser, block: Boolean) {
        val title = if (block) "Block User" else "Unblock User"
        val msg   = if (block) "Block ${user.fullName}? They won't be able to use the app." else "Unblock ${user.fullName}?"
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(msg)
            .setPositiveButton(if (block) "Block" else "Unblock") { _, _ ->
                db.collection("users").document(user.uid).update("isBlocked", block)
                    .addOnSuccessListener {
                        Toast.makeText(this, if (block) "${user.fullName} blocked" else "${user.fullName} unblocked", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}