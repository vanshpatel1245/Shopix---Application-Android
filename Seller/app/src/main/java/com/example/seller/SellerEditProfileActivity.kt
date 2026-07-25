package com.example.seller

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.seller.databinding.ActivitySellerEditProfileBinding
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

class SellerEditProfileActivity : BaseActivity() {

    private lateinit var binding: ActivitySellerEditProfileBinding
    private val auth = FirebaseAuth.getInstance()
    private val db   = FirebaseFirestore.getInstance()
    private var selectedPhotoUri: Uri? = null

    private val CLOUD_NAME    = "diddjxukp"
    private val UPLOAD_PRESET = "shopix_preset"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySellerEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val uid = auth.currentUser?.uid ?: return

        loadProfile(uid)

        // ✅ Camera icon / profile photo → pick image from gallery
        binding.imgProfile.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*" }
            startActivityForResult(intent, 200)
        }

        // ✅ Back button
        binding.btnBackAction.setOnClickListener { finish() }

        // ✅ Save button
        binding.btnSave.setOnClickListener {
            if (selectedPhotoUri != null) {
                uploadPhotoThenSave(uid)
            } else {
                saveProfile(uid, null)
            }
        }
    }

    // ✅ Load existing data into all fields
    private fun loadProfile(uid: String) {
        db.collection("sellers").document(uid).get()
            .addOnSuccessListener { doc ->
                binding.etFullName.setText(doc.getString("name") ?: "")
                binding.etEmail.setText(doc.getString("email") ?: "")
                binding.etPhone.setText(doc.getString("phone") ?: "")
                binding.etBusinessName.setText(doc.getString("businessName") ?: "")
                binding.etBusinessEmail.setText(doc.getString("businessEmail") ?: "")
                binding.etGst.setText(doc.getString("gstNo") ?: "")

                // Load existing profile photo
                val photoUrl = doc.getString("photoUrl") ?: ""
                if (photoUrl.isNotEmpty()) {
                    Glide.with(this)
                        .load(photoUrl)
                        .circleCrop()
                        .into(binding.imgProfile)
                }
            }
    }

    // ✅ Image selected from gallery → show preview
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 200 && resultCode == Activity.RESULT_OK) {
            selectedPhotoUri = data?.data
            Glide.with(this)
                .load(selectedPhotoUri)
                .circleCrop()
                .into(binding.imgProfile)
        }
    }

    // ✅ Upload photo to Cloudinary first then save
    private fun uploadPhotoThenSave(uid: String) {
        binding.btnSave.isEnabled = false
        binding.btnSave.text = "Uploading..."

        Thread {
            try {
                val inputStream = contentResolver.openInputStream(selectedPhotoUri!!)!!
                val tempFile = File(cacheDir, "profile_${System.currentTimeMillis()}.jpg")
                FileOutputStream(tempFile).use { out -> inputStream.copyTo(out) }

                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart(
                        "file", tempFile.name,
                        tempFile.asRequestBody("image/*".toMediaTypeOrNull())
                    )
                    .addFormDataPart("upload_preset", UPLOAD_PRESET)
                    .build()

                val request = Request.Builder()
                    .url("https://api.cloudinary.com/v1_1/$CLOUD_NAME/image/upload")
                    .post(requestBody).build()

                val response = OkHttpClient().newCall(request).execute()
                val body = response.body?.string()

                if (response.isSuccessful && body != null) {
                    val photoUrl = JSONObject(body).getString("secure_url")
                    runOnUiThread { saveProfile(uid, photoUrl) }
                } else {
                    runOnUiThread {
                        binding.btnSave.isEnabled = true
                        binding.btnSave.text = "Save"
                        Toast.makeText(this, "Photo upload failed", Toast.LENGTH_SHORT).show()
                    }
                }
                tempFile.delete()

            } catch (e: Exception) {
                runOnUiThread {
                    binding.btnSave.isEnabled = true
                    binding.btnSave.text = "Save"
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    // ✅ Save all profile fields to Firestore
    private fun saveProfile(uid: String, photoUrl: String?) {
        val updates = hashMapOf<String, Any>(
            "name"          to binding.etFullName.text.toString().trim(),
            "phone"         to binding.etPhone.text.toString().trim(),
            "businessName"  to binding.etBusinessName.text.toString().trim(),
            "businessEmail" to binding.etBusinessEmail.text.toString().trim(),
            "gstNo"         to binding.etGst.text.toString().trim()
        )
        if (photoUrl != null) updates["photoUrl"] = photoUrl

        // ✅ Handle password change — verify current password first
        val currentPass = binding.etCurrentPassword.text.toString().trim()
        val newPass     = binding.etNewPassword.text.toString().trim()

        if (newPass.isNotEmpty()) {
            if (currentPass.isEmpty()) {
                Toast.makeText(this, "Enter current password to change password", Toast.LENGTH_SHORT).show()
                binding.btnSave.isEnabled = true
                binding.btnSave.text = "Save"
                return
            }
            if (newPass.length < 6) {
                Toast.makeText(this, "New password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                return
            }
            // Re-authenticate then update password
            val email = auth.currentUser?.email ?: ""
            val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(email, currentPass)
            auth.currentUser?.reauthenticate(credential)
                ?.addOnSuccessListener {
                    auth.currentUser?.updatePassword(newPass)
                        ?.addOnSuccessListener {
                            Toast.makeText(this, "Password updated!", Toast.LENGTH_SHORT).show()
                        }
                        ?.addOnFailureListener {
                            Toast.makeText(this, "Password update failed", Toast.LENGTH_SHORT).show()
                        }
                }
                ?.addOnFailureListener {
                    Toast.makeText(this, "Current password is wrong", Toast.LENGTH_SHORT).show()
                    return@addOnFailureListener
                }
        }

        // Save profile data
        db.collection("sellers").document(uid).update(updates)
            .addOnSuccessListener {
                binding.btnSave.isEnabled = true
                binding.btnSave.text = "Save"
                Toast.makeText(this, "Profile updated!", Toast.LENGTH_SHORT).show()
                finish() // go back to profile screen
            }
            .addOnFailureListener {
                binding.btnSave.isEnabled = true
                binding.btnSave.text = "Save"
                Toast.makeText(this, "Update failed: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
