package com.example.shopix_admin

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.example.shopix_admin.databinding.ActivityAdminLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AdminLoginActivity : BaseActivity() {

    private lateinit var binding: ActivityAdminLoginBinding
    private val auth = FirebaseAuth.getInstance()
    private val db   = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ✅ Skip login if already signed in as admin
        val current = auth.currentUser
        if (current != null) {
            verifyAdminRole(current.uid)
            return
        }

        binding.btnLogin.setOnClickListener {
            val email    = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isEmpty()) { binding.etEmail.error = "Required"; return@setOnClickListener }
            if (password.isEmpty()) { binding.etPassword.error = "Required"; return@setOnClickListener }

            // 1. Loading state
            binding.btnLogin.isEnabled = false
            binding.progressLogin.visibility = View.VISIBLE

            // ✅ Firebase Auth login
            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener { result ->
                    // Verify this account actually has admin role
                    verifyAdminRole(result.user!!.uid)
                }
                .addOnFailureListener {
                    binding.btnLogin.isEnabled = true
                    binding.progressLogin.visibility = View.GONE
                    Toast.makeText(this, "Login failed: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    // ✅ Check Firestore admins collection to confirm role
    private fun verifyAdminRole(uid: String) {
        db.collection("admins").document(uid).get()
            .addOnSuccessListener { doc ->
                if (doc.exists() && doc.getString("role") == "admin") {
                    startActivity(Intent(this, AdminDashboardActivity::class.java))
                    finish()
                } else {
                    // Signed in but not an admin — sign out immediately
                    auth.signOut()
                    binding.btnLogin.isEnabled = true
                    binding.progressLogin.visibility = View.GONE
                    Toast.makeText(this, "Access denied. Not an admin account.", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener {
                auth.signOut()
                binding.btnLogin.isEnabled = true
                binding.progressLogin.visibility = View.GONE
                Toast.makeText(this, "Verification failed: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}