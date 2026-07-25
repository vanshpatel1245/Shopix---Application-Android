package com.example.seller

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.seller.databinding.ActivityEditProductBinding

class EditProductActivity : BaseActivity() {

    private lateinit var binding: ActivityEditProductBinding
    private val db      = FirebaseFirestore.getInstance()
    private val auth    = FirebaseAuth.getInstance()
    private var selectedImageUri: Uri? = null
    private var existingImageUrl: String = ""

    private val categories = listOf(
        "Mobiles & Tablets", "Fashion", "Electronics",
        "Home & Furniture", "TVs & Appliances", "Beauty & Food"
    )

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            Glide.with(this).load(uri).into(binding.imgProductPreview)
            binding.imgProductPreview.visibility = View.VISIBLE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProductBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val product = intent.getSerializableExtra("product") as? SellerProduct
        if (product == null) { finish(); return }

        existingImageUrl = product.imageUrl
        binding.btnBack.setOnClickListener { finish() }
        binding.btnCancel.setOnClickListener { finish() }

        // Pre-fill all fields
        binding.etProductName.setText(product.name)
        binding.etDescription.setText(product.description)
        binding.etPrice.setText(product.price.toString())
        binding.etSalePrice.setText(product.salePrice.toString())
        binding.etStock.setText(product.stock.toString())
        binding.etSku.setText(product.skuId)

        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categories)
        binding.spinnerCategory.setAdapter(adapter)
        binding.spinnerCategory.setText(product.category, false)

        // Load existing image if available
        if (existingImageUrl.isNotEmpty()) {
            Glide.with(this).load(existingImageUrl).into(binding.imgProductPreview)
            binding.imgProductPreview.visibility = View.VISIBLE
        }

        // ✅ Replace image
        binding.btnReplaceImage.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        // ✅ Save updates to Firestore
        binding.btnSave.setOnClickListener {
            updateProduct(product.id)
        }
    }

    private fun updateProduct(productId: String) {
        val uid = auth.currentUser?.uid ?: return
        binding.btnSave.isEnabled = false

        fun saveUpdates(imageUrl: String) {
            val updates = hashMapOf<String, Any>(
                "name"        to binding.etProductName.text.toString().trim(),
                "description" to binding.etDescription.text.toString().trim(),
                "price"       to (binding.etPrice.text.toString().toDoubleOrNull() ?: 0.0),
                "salePrice"   to (binding.etSalePrice.text.toString().toDoubleOrNull() ?: 0.0),
                "stock"       to (binding.etStock.text.toString().toIntOrNull() ?: 0),
                "skuId"       to binding.etSku.text.toString().trim(),
                "category"    to binding.spinnerCategory.text.toString(),
                "imageUrl"    to imageUrl,
                "sellerId"    to uid
            )
            db.collection("products").document(productId).update(updates)
                .addOnSuccessListener {
                    Toast.makeText(this, "Product updated!", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    binding.btnSave.isEnabled = true
                    Toast.makeText(this, "Update failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        if (selectedImageUri != null) {
            // ✅ Upload new image to Cloudinary
            binding.btnSave.text = "Uploading..."
            CloudinaryUploader.uploadImage(
                context   = this,
                imageUri  = selectedImageUri!!,
                onSuccess = { imageUrl -> runOnUiThread { saveUpdates(imageUrl) } },
                onFailure = { error ->
                    runOnUiThread {
                        binding.btnSave.isEnabled = true
                        binding.btnSave.text = "Save"
                        Toast.makeText(this, "Image upload failed: $error", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        } else {
            saveUpdates(existingImageUrl) // keep existing image if no new one picked
        }
    }
}