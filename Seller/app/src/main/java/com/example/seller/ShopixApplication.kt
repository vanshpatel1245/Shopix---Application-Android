package com.example.seller

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

class ShopixApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Force Light Mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
    }
}
