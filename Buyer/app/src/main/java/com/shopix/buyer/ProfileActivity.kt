package com.shopix.buyer

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.shopix.buyer.databinding.ActivityProfileBinding

class ProfileActivity : BaseActivity() {

    private lateinit var binding: ActivityProfileBinding
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        setupBottomNav(binding.bottomNav, R.id.nav_profile)
        binding.bottomNav.menu.findItem(R.id.nav_profile)?.isChecked = true
        loadUserData()

        binding.btnUpdateProfile.setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }

        binding.btnLogout.setOnClickListener {
            showLogoutConfirmation()
        }

        binding.rowOrders.setOnClickListener {
            startActivity(Intent(this, OrdersActivity::class.java))
        }
        binding.rowFavorites.setOnClickListener {
            android.util.Log.d("ProfileActivity", "Favorites row clicked - opening FavoritesActivity")
            startActivity(Intent(this, FavoritesActivity::class.java))
        }
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ ->
                auth.signOut()
                val intent = Intent(this, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun loadUserData() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                if (isFinishing || isDestroyed) return@addOnSuccessListener
                if (doc.exists()) {
                    binding.txtName.text = doc.getString("name") ?: "User"
                    doc.getString("photoUrl")?.let { photoUrl ->
                        if (photoUrl.isNotEmpty()) {
                            Glide.with(this)
                                .load(photoUrl)
                                .circleCrop()
                                .placeholder(R.drawable.logo_shopix_light)
                                .into(binding.imgAvatar)
                        }
                    }
                }
            }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        loadUserData()
    }

    override fun onResume() {
        super.onResume()
        // Refresh data when returning from EditProfileActivity
        loadUserData()
    }
}