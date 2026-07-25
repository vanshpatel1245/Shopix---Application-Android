package com.example.shopix_admin

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.shopix_admin.R

/**
 * BaseActivity — extend this in every Activity instead of AppCompatActivity.
 *
 * Fixes status bar / navigation bar overlap across the entire Shopix Admin app.
 *
 * Usage: change `AppCompatActivity` → `BaseActivity` in each Activity file.
 */
open class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applySystemUIFix()
    }

    private fun applySystemUIFix() {
        // Admin uses dark theme — dark background, white icons
        WindowCompat.setDecorFitsSystemWindows(window, true)
        window.statusBarColor = getColor(R.color.admin_bg)       // #0D1117 dark
        window.navigationBarColor = getColor(R.color.admin_bg)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false   // white icons on dark bg
            isAppearanceLightNavigationBars = false
        }
    }
}