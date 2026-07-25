package com.example.seller

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.seller.R

open class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // FIX 5: Force light theme
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        
        super.onCreate(savedInstanceState)
        // This ensures the layout is drawn behind system bars if needed, 
        // but we'll set it to 'true' to avoid overlapping issues on OPPO devices.
        WindowCompat.setDecorFitsSystemWindows(window, true)
        applySystemUIFix()
        checkBlockedStatus()
    }

    private fun checkBlockedStatus() {
        val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser ?: return
        
        // Prevent checking on login screen to avoid infinite loops
        if (this is SellerLoginActivity) return

        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()

        // Real-time listener to kick seller out if blocked by admin
        db.collection("sellers").document(user.uid)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    val isBlocked = snapshot.getBoolean("isBlocked") ?: false
                    if (isBlocked) {
                        logoutSeller()
                    }
                }
            }
    }

    private fun logoutSeller() {
        com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
        val intent = android.content.Intent(this, SellerLoginActivity::class.java)
        intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        android.widget.Toast.makeText(this, "Your account has been blocked by Admin", android.widget.Toast.LENGTH_LONG).show()
        finish()
    }

    private fun applySystemUIFix() {
        // Set a default status bar color
        window.statusBarColor = getColor(R.color.shopix_primary)
        
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        // For shopix_primary (Blue), we want white icons
        controller.isAppearanceLightStatusBars = false
    }
}