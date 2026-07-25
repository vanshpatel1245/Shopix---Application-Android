package com.shopix.buyer

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.shopix.buyer.databinding.ActivityEditProfileBinding
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

class EditProfileActivity : BaseActivity() {

    private lateinit var binding: ActivityEditProfileBinding
    private val auth = FirebaseAuth.getInstance()
    private val db   = FirebaseFirestore.getInstance()
    private var selectedPhotoUri: Uri? = null

    // Cloudinary credentials
    private val CLOUD_NAME    = "diddjxukp"
    private val UPLOAD_PRESET = "shopix_preset"

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            selectedPhotoUri = result.data?.data
            if (!isFinishing && !isDestroyed) {
                Glide.with(this).load(selectedPhotoUri)
                    .circleCrop()
                    .into(binding.imgProfile)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        val uid = auth.currentUser?.uid ?: return

        // Load existing profile including photo
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                if (isFinishing || isDestroyed) return@addOnSuccessListener
                if (doc.exists()) {
                    binding.etFullName.setText(doc.getString("name") ?: "")
                    binding.etEmail.setText(doc.getString("email") ?: "")
                    binding.etPhone.setText(doc.getString("phone") ?: "")
                    binding.etAddress.setText(doc.getString("address") ?: "")
                    binding.etCity.setText(doc.getString("city") ?: "")
                    binding.etState.setText(doc.getString("state") ?: "")

                    val photoUrl = doc.getString("photoUrl") ?: ""
                    if (photoUrl.isNotEmpty()) {
                        Glide.with(this).load(photoUrl)
                            .circleCrop()
                            .placeholder(R.drawable.ic_launcher_background)
                            .into(binding.imgProfile)
                    }
                }
            }

        // Pick photo using Result API
        binding.imgProfile.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*" }
            pickImageLauncher.launch(intent)
        }

        binding.btnUpdate.setOnClickListener {
            if (selectedPhotoUri != null) {
                uploadPhotoThenSave(uid)
            } else {
                saveProfile(uid, null)
            }
        }

        binding.btnCancel.setOnClickListener { finish() }
    }

    private fun uploadPhotoThenSave(uid: String) {
        binding.btnUpdate.isEnabled = false
        binding.btnUpdate.text = "Uploading photo..."

        Thread {
            try {
                val inputStream = contentResolver.openInputStream(selectedPhotoUri!!)!!
                val tempFile = File(cacheDir, "profile_${System.currentTimeMillis()}.jpg")
                FileOutputStream(tempFile).use { out -> inputStream.copyTo(out) }

                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", tempFile.name,
                        tempFile.asRequestBody("image/*".toMediaTypeOrNull()))
                    .addFormDataPart("upload_preset", UPLOAD_PRESET)
                    .build()

                val request = Request.Builder()
                    .url("https://api.cloudinary.com/v1_1/$CLOUD_NAME/image/upload")
                    .post(requestBody).build()

                val response = OkHttpClient().newCall(request).execute()
                val body = response.body?.string()

                if (response.isSuccessful && body != null) {
                    val photoUrl = JSONObject(body).getString("secure_url")
                    runOnUiThread { 
                        if (!isFinishing && !isDestroyed) {
                            saveProfile(uid, photoUrl) 
                        }
                    }
                } else {
                    runOnUiThread {
                        if (!isFinishing && !isDestroyed) {
                            binding.btnUpdate.isEnabled = true
                            binding.btnUpdate.text = "Update"
                            Toast.makeText(this, "Photo upload failed", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                tempFile.delete()
            } catch (e: Exception) {
                runOnUiThread {
                    if (!isFinishing && !isDestroyed) {
                        binding.btnUpdate.isEnabled = true
                        binding.btnUpdate.text = "Update"
                        Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }.start()
    }

    private fun saveProfile(uid: String, photoUrl: String?) {
        val updates = hashMapOf<String, Any>(
            "name"    to binding.etFullName.text.toString().trim(),
            "phone"   to binding.etPhone.text.toString().trim(),
            "address" to binding.etAddress.text.toString().trim(),
            "city"    to binding.etCity.text.toString().trim(),
            "state"   to binding.etState.text.toString().trim()
        )
        if (photoUrl != null) updates["photoUrl"] = photoUrl

        db.collection("users").document(uid).update(updates)
            .addOnSuccessListener {
                if (isFinishing || isDestroyed) return@addOnSuccessListener
                Toast.makeText(this, "Profile updated!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                if (isFinishing || isDestroyed) return@addOnFailureListener
                binding.btnUpdate.isEnabled = true
                binding.btnUpdate.text = "Update"
                Toast.makeText(this, "Update failed: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}