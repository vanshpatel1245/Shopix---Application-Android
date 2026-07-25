package com.shopix.buyer

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.shopix.buyer.databinding.ActivityLoginBinding

class LoginActivity : BaseActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        // ✅ If user is already logged in, skip to Home (ONLY if not blocked)
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            db.collection("users").document(currentUser.uid).get()
                .addOnSuccessListener { doc ->
                    val isBlocked = doc.getBoolean("isBlocked") ?: false
                    if (isBlocked) {
                        auth.signOut()
                        Toast.makeText(this, "Your account is blocked", Toast.LENGTH_LONG).show()
                    } else {
                        val intent = Intent(this, HomeActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        startActivity(intent)
                        finish()
                    }
                }
                .addOnFailureListener {
                    val intent = Intent(this, HomeActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                    finish()
                }
            return
        }

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            // Reset errors
            binding.tilEmail.error = null
            binding.tilPassword.error = null

            if (email.isEmpty()) {
                binding.tilEmail.error = "Enter email"
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                binding.tilPassword.error = "Enter password"
                return@setOnClickListener
            }

            // ✅ Show progress and disable button to prevent multiple clicks
            binding.btnLogin.isEnabled = false
            binding.progressLogin.visibility = View.VISIBLE

            // ✅ Firebase Login
            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener { result ->
                    // Check if newly logged in user is blocked
                    val uid = result.user?.uid ?: return@addOnSuccessListener
                    val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    
                    db.collection("users").document(uid).get()
                        .addOnSuccessListener { doc ->
                            val isBlocked = doc.getBoolean("isBlocked") ?: false
                            if (isBlocked) {
                                auth.signOut()
                                binding.btnLogin.isEnabled = true
                                binding.progressLogin.visibility = View.GONE
                                Toast.makeText(this, "This account has been blocked by Admin", Toast.LENGTH_LONG).show()
                            } else {
                                val intent = Intent(this, HomeActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                }
                                startActivity(intent)
                                finish()
                            }
                        }
                        .addOnFailureListener {
                            val intent = Intent(this, HomeActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            }
                            startActivity(intent)
                            finish()
                        }
                }
                .addOnFailureListener { e ->
                    // Re-enable on failure
                    binding.btnLogin.isEnabled = true
                    binding.progressLogin.visibility = View.GONE
                    Toast.makeText(this, "Login failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        binding.txtSignup.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }
    }
}