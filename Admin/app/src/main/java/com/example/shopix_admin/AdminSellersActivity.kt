package com.example.shopix_admin

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.shopix_admin.databinding.ActivityAdminSellersBinding
import com.google.firebase.firestore.FirebaseFirestore

class AdminSellersActivity : BaseActivity() {

    private lateinit var binding: ActivityAdminSellersBinding
    private val db = FirebaseFirestore.getInstance()
    private val sellersList = mutableListOf<AdminSeller>()
    private lateinit var adapter: AdminSellersAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminSellersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        // Initialize adapter with a persistent mutable list
        adapter = AdminSellersAdapter(sellersList) { seller, action ->
            when (action) {
                "verify"   -> toggleSellerVerification(seller)
                "block"    -> setSellerBlocked(seller, true)
                "unblock"  -> setSellerBlocked(seller, false)
                "delete"   -> deleteSeller(seller)
            }
        }
        binding.rvSellers.layoutManager = LinearLayoutManager(this)
        binding.rvSellers.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener {
            loadSellers()
        }

        loadSellers()
    }

    private fun loadSellers() {
        binding.swipeRefresh.isRefreshing = true
        db.collection("sellers")
            .get()
            .addOnSuccessListener { snapshots ->
                binding.swipeRefresh.isRefreshing = false
                
                val newSellers = snapshots?.documents?.mapNotNull { doc ->
                    if (doc.getString("email").isNullOrEmpty()) return@mapNotNull null

                    AdminSeller(
                        uid          = doc.id,
                        fullName     = doc.getString("name") ?: "—",
                        email        = doc.getString("email") ?: "—",
                        phone        = doc.getString("phone") ?: "—",
                        shopName     = doc.getString("businessName") ?: "—",
                        gstin        = doc.getString("gstNo") ?: "—",
                        isVerified   = doc.getBoolean("isVerified") ?: false,
                        isBlocked    = doc.getBoolean("isBlocked") ?: false,
                        totalIncome  = doc.getDouble("totalIncome") ?: 0.0,
                        createdAt    = doc.getString("createdAt") ?: "—"
                    )
                } ?: emptyList<AdminSeller>()

                binding.txtCount.text = "${newSellers.size} sellers"

                if (newSellers.isEmpty()) {
                    binding.layoutEmpty.visibility = View.VISIBLE
                    binding.rvSellers.visibility = View.GONE
                } else {
                    binding.layoutEmpty.visibility = View.GONE
                    binding.rvSellers.visibility = View.VISIBLE
                }

                adapter.updateList(newSellers)
            }
            .addOnFailureListener {
                binding.swipeRefresh.isRefreshing = false
                Toast.makeText(this, "Error loading sellers", Toast.LENGTH_SHORT).show()
            }
    }

    private fun toggleSellerVerification(seller: AdminSeller) {
        val nextStatus = !seller.isVerified
        val title = if (nextStatus) "Verify Seller" else "Unverify Seller"
        val msg = if (nextStatus) "Mark ${seller.fullName} as a verified seller?" else "Remove verification from ${seller.fullName}?"
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(msg)
            .setPositiveButton(if (nextStatus) "Verify" else "Unverify") { _, _ ->
                db.collection("sellers").document(seller.uid).update("isVerified", nextStatus)
                    .addOnSuccessListener {
                        Toast.makeText(this, if (nextStatus) "Seller verified" else "Seller unverified", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setSellerBlocked(seller: AdminSeller, block: Boolean) {
        val title = if (block) "Block Seller" else "Unblock Seller"
        val msg   = if (block) "Block ${seller.fullName}? All their products will be hidden from buyers." 
                    else "Unblock ${seller.fullName}? Their products will be visible again."
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(msg)
            .setPositiveButton(if (block) "Block" else "Unblock") { _, _ ->
                // 1. Update Seller Document
                db.collection("sellers").document(seller.uid).update("isBlocked", block)
                    .addOnSuccessListener {
                        // 2. Update all products of this seller (isApproved = !block)
                        db.collection("products").whereEqualTo("sellerId", seller.uid).get()
                            .addOnSuccessListener { snap ->
                                if (snap.isEmpty) {
                                    Toast.makeText(this, if (block) "${seller.fullName} blocked" else "${seller.fullName} unblocked", Toast.LENGTH_SHORT).show()
                                    return@addOnSuccessListener
                                }
                                
                                val batch = db.batch()
                                snap.documents.forEach { doc ->
                                    batch.update(doc.reference, "isApproved", !block)
                                }
                                batch.commit().addOnSuccessListener {
                                    Toast.makeText(this, if (block) "${seller.fullName} and their products blocked" else "${seller.fullName} unblocked", Toast.LENGTH_SHORT).show()
                                }
                            }
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteSeller(seller: AdminSeller) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Delete Seller")
            .setMessage("Delete \"${seller.fullName}\" and ALL their products? This cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                db.collection("sellers").document(seller.uid).delete()
                    .addOnSuccessListener {
                        db.collection("products").whereEqualTo("sellerId", seller.uid).get()
                            .addOnSuccessListener { snap ->
                                val batch = db.batch()
                                snap.documents.forEach { batch.delete(it.reference) }
                                batch.commit().addOnSuccessListener {
                                    Toast.makeText(this, "${seller.fullName} and their products deleted", Toast.LENGTH_SHORT).show()
                                    // Note: Firebase Auth account for this seller is not deleted because deleting auth accounts requires Firebase Admin SDK which can only run on a backend server, not on a client app. The seller's Firestore data and products are fully deleted. For full deletion, a Cloud Function would be needed in production.
                                }
                            }
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}