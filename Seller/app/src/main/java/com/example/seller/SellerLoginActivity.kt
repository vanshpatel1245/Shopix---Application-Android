package com.example.seller

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.example.seller.databinding.ActivitySellerLoginBinding

class SellerLoginActivity : BaseActivity() {

    private lateinit var binding: ActivitySellerLoginBinding
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySellerLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ✅ Skip login if already signed in (ONLY if not blocked)
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            db.collection("sellers").document(currentUser.uid).get()
                .addOnSuccessListener { doc ->
                    val isBlocked = doc.getBoolean("isBlocked") ?: false
                    if (isBlocked) {
                        auth.signOut()
                        Toast.makeText(this, "Your account is blocked", Toast.LENGTH_LONG).show()
                    } else {
                        startActivity(Intent(this, SellerDashboardActivity::class.java))
                        finish()
                    }
                }
                .addOnFailureListener {
                    // If check fails, go to Dashboard
                    startActivity(Intent(this, SellerDashboardActivity::class.java))
                    finish()
                }
            return
        }

        binding.btnLogin.setOnClickListener {
            val email    = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Enter email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // ✅ Disable button and show progress
            binding.btnLogin.isEnabled = false
            binding.progressLogin.visibility = View.VISIBLE

            // ✅ Firebase Auth login
            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener { result ->
                    // Check if newly logged in user is blocked
                    val uid = result.user?.uid ?: return@addOnSuccessListener
                    val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()

                    db.collection("sellers").document(uid).get()
                        .addOnSuccessListener { doc ->
                            val isBlocked = doc.getBoolean("isBlocked") ?: false
                            if (isBlocked) {
                                auth.signOut()
                                binding.btnLogin.isEnabled = true
                                binding.progressLogin.visibility = View.GONE
                                Toast.makeText(this, "This account has been blocked by Admin", Toast.LENGTH_LONG).show()
                            } else {
                                startActivity(Intent(this, SellerDashboardActivity::class.java))
                                finish()
                            }
                        }
                }
                .addOnFailureListener { e ->
                    // ✅ Re-enable button and hide progress
                    binding.btnLogin.isEnabled = true
                    binding.progressLogin.visibility = View.GONE
                    Toast.makeText(this, "Login failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        binding.txtBecomeSeller.setOnClickListener {
            startActivity(Intent(this, SellerRegisterActivity::class.java))
        }
    }
}
