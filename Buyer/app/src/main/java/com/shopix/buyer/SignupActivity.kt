package com.shopix.buyer

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.shopix.buyer.databinding.ActivitySignupBinding

class SignupActivity : BaseActivity() {

    private lateinit var binding: ActivitySignupBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        binding.btnSignup.setOnClickListener {
            val name = binding.etFullName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            // Reset errors
            binding.tilFullName.error = null
            binding.tilEmail.error = null
            binding.tilPassword.error = null

            if (name.isEmpty()) {
                binding.tilFullName.error = "Name required"
                return@setOnClickListener
            }
            if (email.isEmpty()) {
                binding.tilEmail.error = "Email required"
                return@setOnClickListener
            }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.tilEmail.error = "Enter a valid email"
                return@setOnClickListener
            }
            if (password.length < 6) {
                binding.tilPassword.error = "Min 6 characters"
                return@setOnClickListener
            }

            // FIX 1: Loading state
            binding.btnSignup.isEnabled = false
            binding.progressSignup.visibility = View.VISIBLE

            // ✅ Firebase Register
            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { result ->
                    val uid = result.user!!.uid
                    // Save user profile to Firestore with default fields
                    val userMap = hashMapOf(
                        "name" to name,
                        "email" to email,
                        "uid" to uid,
                        "isBlocked" to false,
                        "createdAt" to com.google.firebase.Timestamp.now(),
                        "phone" to "",
                        "address" to ""
                    )
                    
                    db.collection("users").document(uid).set(userMap)
                        .addOnSuccessListener {
                            binding.progressSignup.visibility = View.GONE
                            Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show()
                            
                            val intent = Intent(this, HomeActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            }
                            startActivity(intent)
                            finish()
                        }
                        .addOnFailureListener { e ->
                            binding.btnSignup.isEnabled = true
                            binding.progressSignup.visibility = View.GONE
                            Toast.makeText(this, "Profile creation failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener { e ->
                    binding.btnSignup.isEnabled = true
                    binding.progressSignup.visibility = View.GONE
                    Toast.makeText(this, "Signup failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        binding.txtLogin.setOnClickListener { finish() }
    }
}