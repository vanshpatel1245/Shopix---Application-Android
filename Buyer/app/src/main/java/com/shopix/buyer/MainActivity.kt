package com.shopix.buyer

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import com.google.firebase.auth.FirebaseAuth

class MainActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        val logo = findViewById<ImageView>(R.id.imgLogo)
        logo.alpha = 0f
        logo.animate().alpha(1f).setDuration(500).start()

        // Check if user is already logged in
        val currentUser = FirebaseAuth.getInstance().currentUser
        
        Handler(Looper.getMainLooper()).postDelayed({
            if (currentUser != null) {
                // User is logged in, go directly to Home
                startActivity(Intent(this, HomeActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
            } else {
                // User not logged in, go to Login
                startActivity(Intent(this, LoginActivity::class.java))
            }
            finish()
        }, 1200)
    }
}