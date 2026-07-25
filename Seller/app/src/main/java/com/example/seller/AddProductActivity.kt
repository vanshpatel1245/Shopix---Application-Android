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
import com.example.seller.databinding.ActivityAddProductBinding

class AddProductActivity : BaseActivity() {

    private lateinit var binding: ActivityAddProductBinding
    private val db   = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var selectedImageUri: Uri? = null
    private var uploadedImageUrl: String = ""  // stores Cloudinary URL after upload

    private val categories = listOf(
        "Select Category", "Mobiles & Tablets", "Fashion",
        "Electronics", "Home & Furniture", "TVs & Appliances",
        "Beauty & Food"
    )

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            uploadedImageUrl = ""
            Glide.with(this).load(uri).into(binding.imgProductPreview)
            binding.imgProductPreview.visibility = View.VISIBLE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddProductBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categories)
        binding.spinnerCategory.setAdapter(adapter)

        // ✅ Step 1 — Seller picks image from phone
        binding.btnChooseFile.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        binding.btnAddProduct.setOnClickListener {
            if (validateInputs()) {
                if (selectedImageUri != null && uploadedImageUrl.isEmpty()) {
                    // ✅ Step 2 — Upload image to Cloudinary first
                    uploadImageThenSave()
                } else {
                    // Save directly if no image or already uploaded
                    saveProductToFirestore(uploadedImageUrl)
                }
            }
        }
    }

    // ✅ Step 2 — Upload to Cloudinary → get URL → save to Firestore
    private fun uploadImageThenSave() {
        binding.btnAddProduct.isEnabled = false
        binding.btnAddProduct.text = "Uploading image..."

        CloudinaryUploader.uploadImage(
            context    = this,
            imageUri   = selectedImageUri!!,
            onSuccess  = { imageUrl ->
                uploadedImageUrl = imageUrl
                // ✅ Step 3 — Now save product with the Cloudinary image URL
                runOnUiThread {
                    saveProductToFirestore(imageUrl)
                }
            },
            onFailure  = { error ->
                runOnUiThread {
                    binding.btnAddProduct.isEnabled = true
                    binding.btnAddProduct.text = "Add Product"
                    Toast.makeText(this, "Image upload failed: $error", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    // ✅ Step 3 — Save product data + image URL to Firestore
    private fun saveProductToFirestore(imageUrl: String) {
        val uid = auth.currentUser?.uid ?: return

        val productData = hashMapOf(
            "name"        to binding.etProductName.text.toString().trim(),
            "description" to binding.etDescription.text.toString().trim(),
            "price"       to (binding.etPrice.text.toString().toDoubleOrNull() ?: 0.0),
            "salePrice"   to (binding.etSalePrice.text.toString().toDoubleOrNull() ?: 0.0),
            "oldPrice"    to (binding.etPrice.text.toString().toDoubleOrNull() ?: 0.0),
            "stock"       to (binding.etStock.text.toString().toIntOrNull() ?: 0),
            "skuId"       to binding.etSku.text.toString().trim(),
            "category"    to binding.spinnerCategory.text.toString(),
            "imageUrl"    to imageUrl,       // ✅ Cloudinary URL saved here
            "sellerId"    to uid,
            "isApproved"  to false,
            "gstNo"       to "",
            "createdAt"   to com.google.firebase.Timestamp.now()
        )

        db.collection("products").add(productData)
            .addOnSuccessListener {
                Toast.makeText(this, "Product submitted for admin approval!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                binding.btnAddProduct.isEnabled = true
                binding.btnAddProduct.text = "Add Product"
                Toast.makeText(this, "Failed to save: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun validateInputs(): Boolean {
        val name     = binding.etProductName.text.toString().trim()
        val price    = binding.etPrice.text.toString().trim()
        val stock    = binding.etStock.text.toString().trim()
        val category = binding.spinnerCategory.text.toString()
        if (name.isEmpty())  { binding.etProductName.error = "Required"; return false }
        if (price.isEmpty()) { binding.etPrice.error = "Required"; return false }
        if (stock.isEmpty()) { binding.etStock.error = "Required"; return false }
        if (category == "Select Category") {
            Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show()
            return false
        }
        if (selectedImageUri == null && uploadedImageUrl.isEmpty()) {
            Toast.makeText(this, "Please select a product image", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }
}