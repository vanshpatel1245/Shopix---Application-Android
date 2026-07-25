package com.example.shopix_admin

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.shopix_admin.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Animate logo in
        binding.imgLogo.alpha = 0f
        binding.txtAdmin.alpha = 0f
        binding.txtVersion.alpha = 0f

        binding.imgLogo.animate().alpha(1f).setDuration(700).start()
        binding.txtAdmin.animate().alpha(1f).setStartDelay(300).setDuration(700).start()
        binding.txtVersion.animate().alpha(1f).setStartDelay(600).setDuration(700).start()

        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, AdminLoginActivity::class.java))
            finish()
        }, 2200)
    }
}