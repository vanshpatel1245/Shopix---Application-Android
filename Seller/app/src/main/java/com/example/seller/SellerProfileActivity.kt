package com.example.seller

import android.content.Intent
import android.os.Bundle
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.seller.databinding.ActivitySellerProfileBinding

class SellerProfileActivity : BaseActivity() {

    private lateinit var binding: ActivitySellerProfileBinding
    private val auth = FirebaseAuth.getInstance()
    private val db   = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySellerProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        // ✅ Update Profile button → open Edit Profile screen
        binding.btnUpdateProfile.setOnClickListener {
            startActivity(Intent(this, SellerEditProfileActivity::class.java))
        }

        binding.btnWithdraw.setOnClickListener {
            android.widget.Toast.makeText(this, "Withdraw coming soon!", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        // ✅ Reload profile every time user comes back from edit screen
        loadProfile()
    }

    private fun loadProfile() {
        val uid = auth.currentUser?.uid ?: return

        // Load name, business, gst, photo
        db.collection("sellers").document(uid).get()
            .addOnSuccessListener { doc ->
                binding.txtName.text         = doc.getString("name") ?: "Seller"
                binding.txtBusinessName.text = doc.getString("businessName") ?: ""
                binding.txtGst.text          = doc.getString("gstNo") ?: ""

                val photoUrl = doc.getString("photoUrl") ?: ""
                if (photoUrl.isNotEmpty()) {
                    Glide.with(this)
                        .load(photoUrl)
                        .circleCrop()
                        .placeholder(R.drawable.ic_profile)
                        .into(binding.imgProfile)
                }
            }

        // ✅ Compute income from COMPLETED orders only
        // Formula: salePrice × qty × 0.95 (5% shipping deducted)
        db.collection("orders")
            .whereArrayContains("sellerIds", uid)
            .get()
            .addOnSuccessListener { snapshots ->
                var income = 0.0
                snapshots.documents
                    .filter {
                        it.getString("status") == "completed" ||
                                it.getString("status") == "delivered"
                    }
                    .forEach { doc ->
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
                binding.txtTotalIncome.text = "₹%.2f".format(income)

                // Also update sellers doc so dashboard shows same value
                db.collection("sellers").document(uid)
                    .update("totalIncome", income)
            }
    }
}
