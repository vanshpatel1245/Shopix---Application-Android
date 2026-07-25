package com.shopix.buyer

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.shopix.buyer.databinding.BottomSheetCategoryBinding

/**
 * BaseActivity — extend this in every Activity instead of AppCompatActivity.
 */
open class BaseActivity : AppCompatActivity() {

    private var blockedStatusListener: ListenerRegistration? = null
    private var cartBadgeListener: ListenerRegistration? = null
    private var categoryBottomSheet: BottomSheetDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applySystemUIFix()
    }

    override fun onResume() {
        super.onResume()
        checkBlockedStatus()
    }

    override fun onPause() {
        super.onPause()
        blockedStatusListener?.remove()
        cartBadgeListener?.remove()
        // Dismiss any open bottom sheet when activity is paused
        categoryBottomSheet?.dismiss()
        categoryBottomSheet = null
    }

    private fun checkBlockedStatus() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        if (this is LoginActivity || this is SignupActivity || this is MainActivity) return

        blockedStatusListener?.remove()
        blockedStatusListener = FirebaseFirestore.getInstance().collection("users").document(user.uid)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    val isBlocked = snapshot.getBoolean("isBlocked") ?: false
                    if (isBlocked) {
                        logoutUser()
                    }
                }
            }
    }

    private fun logoutUser() {
        FirebaseAuth.getInstance().signOut()
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        Toast.makeText(this, "Your account has been blocked", Toast.LENGTH_LONG).show()
        finish()
    }

    private fun applySystemUIFix() {
        WindowCompat.setDecorFitsSystemWindows(window, true)
        window.statusBarColor = android.graphics.Color.WHITE
        window.navigationBarColor = android.graphics.Color.WHITE

        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
    }

    /**
     * ✅ Shared Bottom Navigation Logic to reduce duplication
     */
    protected fun setupBottomNav(bottomNav: BottomNavigationView, currentItemId: Int) {
        setupCartBadge(bottomNav)
        bottomNav.selectedItemId = currentItemId
        bottomNav.setOnItemSelectedListener { item ->

            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, HomeActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT })
                    true
                }
                R.id.nav_cart -> {
                    startActivity(Intent(this, CartActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT })
                    true
                }
                R.id.nav_categories -> {
                    showCategoryBottomSheet()
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT })
                    true
                }
                else -> false
            }
        }
        bottomNav.setOnItemReselectedListener { item ->
            if (item.itemId == R.id.nav_categories) {
                showCategoryBottomSheet()
            }
        }
    }

    protected fun setupCartBadge(bottomNav: BottomNavigationView) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        cartBadgeListener?.remove()
        cartBadgeListener = FirebaseFirestore.getInstance().collection("users").document(uid).collection("cart")
            .addSnapshotListener { snapshots, _ ->
                val count = snapshots?.size() ?: 0
                val badge = bottomNav.getOrCreateBadge(R.id.nav_cart)
                badge.isVisible = count > 0
                if (count > 0) badge.number = count
            }
    }

    protected fun toggleFavorite(product: Product) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()
        val favRef = db.collection("users").document(uid).collection("favorites").document(product.id)

        product.isFavorite = !product.isFavorite

        if (product.isFavorite) {
            val favItem = hashMapOf(
                "name"        to product.name,
                "price"       to product.price,
                "oldPrice"    to product.oldPrice,
                "salePrice"   to product.salePrice,
                "imageUrl"    to product.imageUrl,
                "category"    to product.category,
                "description" to product.description,
                "stock"       to product.stock,
                "sellerId"    to product.sellerId,
                "gstNo"       to product.gstNo,
                "skuId"       to product.skuId
            )
            favRef.set(favItem)
                .addOnSuccessListener {
                    Toast.makeText(this, "Added to favorites", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    product.isFavorite = false
                    Toast.makeText(this, "Failed to add favorite: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            favRef.delete()
                .addOnSuccessListener {
                    Toast.makeText(this, "Removed from favorites", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    product.isFavorite = true
                    Toast.makeText(this, "Failed to remove favorite: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
    protected fun showCategoryBottomSheet() {
        android.util.Log.d("BaseActivity", "showCategoryBottomSheet: CALLED")
        android.util.Log.d("BaseActivity", "showCategoryBottomSheet: Current activity: ${this::class.java.simpleName}")
        
        // Dismiss any existing dialog to prevent multiple instances
        categoryBottomSheet?.dismiss()
        
        // Create new dialog
        categoryBottomSheet = BottomSheetDialog(this)
        val sheetBinding = BottomSheetCategoryBinding.inflate(layoutInflater)
        categoryBottomSheet?.setContentView(sheetBinding.root)

        // Test if close button exists and is clickable
        android.util.Log.d("BaseActivity", "Close button exists: ${sheetBinding.btnClose != null}")
        android.util.Log.d("BaseActivity", "Close button is clickable: ${sheetBinding.btnClose.isClickable}")
        
        sheetBinding.btnClose.setOnClickListener { 
            android.util.Log.d("BaseActivity", "BottomSheet close button clicked")
            android.util.Log.d("BaseActivity", "BottomSheet dialog before dismiss: ${categoryBottomSheet?.isShowing}")
            categoryBottomSheet?.dismiss()
            categoryBottomSheet = null
            android.util.Log.d("BaseActivity", "BottomSheet dialog after dismiss: null")
        }

        val categories = listOf("Mobiles & Tablets", "Fashion", "Electronics", "Home & Furniture", "TVs & Appliances", "Beauty & Food")
        val categoryViews = listOf(sheetBinding.tvCat1, sheetBinding.tvCat2, sheetBinding.tvCat3, sheetBinding.tvCat4, sheetBinding.tvCat5, sheetBinding.tvCat6)

        categories.forEachIndexed { index, category ->
            categoryViews[index].text = category
            categoryViews[index].setOnClickListener {
                android.util.Log.d("BaseActivity", "Category selected: '$category'")
                categoryBottomSheet?.dismiss()
                categoryBottomSheet = null
                
                val intent = Intent(this, CategoryActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    putExtra("category", category)
                }
                android.util.Log.d("BaseActivity", "Starting CategoryActivity with category: '$category'")
                startActivity(intent)
            }
        }
        
        // Set up dismiss listener to clean up reference
        categoryBottomSheet?.setOnDismissListener {
            android.util.Log.d("BaseActivity", "BottomSheet dismissed")
            categoryBottomSheet = null
        }
        
        categoryBottomSheet?.show()
    }
}