package com.example.seller

import android.content.Intent
import android.os.Bundle
import androidx.core.view.WindowInsetsControllerCompat
import com.example.seller.databinding.ActivitySellerLandingBinding

class SellerLandingActivity : BaseActivity() {

    private lateinit var binding: ActivitySellerLandingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ Landing has blue top section — white status bar icons
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
        }

        binding = ActivitySellerLandingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnLoginAsSeller.setOnClickListener {
            startActivity(Intent(this, SellerLoginActivity::class.java))
        }

        binding.btnBecomeSeller.setOnClickListener {
            startActivity(Intent(this, SellerRegisterActivity::class.java))
        }
    }
}
