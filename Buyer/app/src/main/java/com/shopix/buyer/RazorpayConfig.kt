package com.shopix.buyer

/**
 * Razorpay Payment Configuration
 *
 * ⚠️ IMPORTANT — Before going LIVE:
 * 1. Replace rzp_test_ key with rzp_live_ key from Razorpay Dashboard
 * 2. NEVER commit key_secret to GitHub — move it to a backend server
 * 3. In production, create orders from your backend (not client side)
 *
 * Dashboard: https://dashboard.razorpay.com
 */
object RazorpayConfig {

    // ✅ Your Razorpay Test Keys
    const val KEY_ID     = "rzp_test_RJYHP1TteC3Y5A"

    // App info shown on Razorpay payment sheet
    const val APP_NAME   = "Shopix"
    const val CURRENCY   = "INR"
    const val THEME_COLOR = "#0F6EFD"

    // Payment descriptions
    const val DESCRIPTION = "Payment for Shopix Order"
}