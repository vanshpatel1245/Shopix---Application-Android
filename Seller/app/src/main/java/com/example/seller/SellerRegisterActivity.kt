package com.example.seller

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.seller.databinding.ActivitySellerRegisterBinding

class SellerRegisterActivity : BaseActivity() {

    private lateinit var binding: ActivitySellerRegisterBinding
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySellerRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSignUp.setOnClickListener {
            val name         = binding.etFullName.text.toString().trim()
            val email        = binding.etEmail.text.toString().trim()
            val password     = binding.etPassword.text.toString().trim()
            val businessName = binding.etShopName.text.toString().trim()
            val gstNo        = binding.etGstin.text.toString().trim()
            val phone        = binding.etPhone.text.toString().trim()

            if (name.isEmpty() || email.isEmpty() || password.isEmpty() ||
                businessName.isEmpty() || gstNo.isEmpty() || phone.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.etEmail.error = "Invalid email format"
                return@setOnClickListener
            }

            // ✅ Password length validation
            if (password.length < 6) {
                binding.etPassword.error = "Password must be at least 6 characters"
                return@setOnClickListener
            }

            // ✅ Disable button and show loading state
            binding.btnSignUp.isEnabled = false
            binding.btnSignUp.text = "Creating account..."

            // ✅ Create Firebase Auth account
            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { result ->
                    val uid = result.user!!.uid

                    val counterRef = db.collection("counters").document("sellers")
                    db.runTransaction { transaction ->
                        val snap = transaction.get(counterRef)
                        val next = (snap.getLong("count") ?: 0L) + 1
                        transaction.set(counterRef, mapOf("count" to next))
                        val sellerId = "ss%04d".format(next)
                        val sellerData = hashMapOf(
                            "uid"          to uid,
                            "sellerId"     to sellerId,
                            "name"         to name,
                            "email"        to email,
                            "phone"        to phone,
                            "businessName" to businessName,
                            "gstNo"        to gstNo,
                            "totalIncome"  to 0.0,
                            "role"         to "seller"
                        )
                        transaction.set(db.collection("sellers").document(uid), sellerData)
                        sellerId
                    }.addOnSuccessListener {
                        binding.btnSignUp.isEnabled = true
                        binding.btnSignUp.text = "Sign Up"
                        Toast.makeText(this, "Account created!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, SellerDashboardActivity::class.java))
                        finish()
                    }.addOnFailureListener { e ->
                        binding.btnSignUp.isEnabled = true
                        binding.btnSignUp.text = "Sign Up"
                        Toast.makeText(this, "Registration failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    // ✅ Re-enable button and reset text on failure
                    binding.btnSignUp.isEnabled = true
                    binding.btnSignUp.text = "Sign Up"
                    Toast.makeText(this, "Registration failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        binding.txtLogin.setOnClickListener { finish() }
    }
}
