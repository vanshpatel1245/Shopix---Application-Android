package com.shopix.buyer

import android.app.Application
import android.webkit.WebView
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.gms.security.ProviderInstaller
import com.razorpay.Checkout

class ShopixApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Patch Security Provider for older devices to avoid SSL/TLS & gRPC failures
        try {
            ProviderInstaller.installIfNeeded(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Set night mode once
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        
        // Global Razorpay preload
        Checkout.preload(applicationContext)

        // Warm up WebView to avoid renderer crashes on buggy vendor systems
        try {
            android.webkit.WebView(applicationContext).destroy()
        } catch (e: Exception) {}
    }
}
