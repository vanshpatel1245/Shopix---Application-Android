package com.example.seller

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.core.view.WindowInsetsControllerCompat
import com.google.firebase.auth.FirebaseAuth
import com.example.seller.databinding.ActivityMainBinding

class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = getColor(android.R.color.white)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.imgLogo.alpha = 0f
        binding.txtTagline.alpha = 0f
        binding.imgLogo.animate().alpha(1f).setDuration(600).start()
        binding.txtTagline.animate().alpha(1f).setStartDelay(300).setDuration(600).start()

        Handler(Looper.getMainLooper()).postDelayed({
            // ✅ If already logged in → go to Dashboard directly
            // ✅ If not logged in → go to Landing (onboarding) page
            if (auth.currentUser != null) {
                startActivity(Intent(this, SellerDashboardActivity::class.java))
            } else {
                startActivity(Intent(this, SellerLandingActivity::class.java))
            }
            finish()
        }, 2000)
    }
}