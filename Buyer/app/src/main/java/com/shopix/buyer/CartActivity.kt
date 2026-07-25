package com.shopix.buyer

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.razorpay.Checkout
import com.razorpay.PaymentData
import com.razorpay.PaymentResultWithDataListener
import com.shopix.buyer.databinding.ActivityCartBinding
import org.json.JSONObject

class CartActivity : BaseActivity(), PaymentResultWithDataListener {

    private lateinit var binding: ActivityCartBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var cartItems = mutableListOf<CartItem>()
    private lateinit var cartAdapter: CartAdapter
    private var currentTotal = 0.0
    private var cartListener: com.google.firebase.firestore.ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCartBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Preload Razorpay for better UX
        Checkout.preload(applicationContext)

        if (savedInstanceState != null) {
            currentTotal = savedInstanceState.getDouble("currentTotal")
            val savedItems = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                savedInstanceState.getParcelableArrayList("cartItems", CartItem::class.java)
            } else {
                @Suppress("DEPRECATION")
                savedInstanceState.getParcelableArrayList("cartItems")
            }
            if (savedItems != null) {
                cartItems = savedItems.toMutableList()
            }
        }

        binding.btnBack.setOnClickListener { finish() }
        setupBottomNav(binding.bottomNav, R.id.nav_cart)
        setupRecyclerView()
        setupSwipeRefresh()
        loadCartFromFirestore()

        binding.btnCheckout.setOnClickListener {
            if (cartItems.isEmpty()) {
                Toast.makeText(this, "Cart is empty!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            startRazorpayPayment()
        }
    }

    private fun startRazorpayPayment() {
        if (!binding.btnCheckout.isEnabled) {
            android.util.Log.d("ShopixCheckout", "Checkout button is already disabled (payment in progress)")
            return
        }
        
        // Defensive check for activity state
        if (isFinishing || isDestroyed) return

        val subtotal = cartItems.sumOf { it.salePrice * it.quantity }
        val shipping = if (subtotal > 0) 40.0 else 0.0
        currentTotal = subtotal + shipping

        if (currentTotal < 1.0) {
            Toast.makeText(this, "Minimum order amount is ₹1.00", Toast.LENGTH_SHORT).show()
            return
        }

        android.util.Log.d("ShopixCheckout", "Starting payment for amount: $currentTotal")
        
        val checkout = Checkout()
        try {
            checkout.setKeyID(RazorpayConfig.KEY_ID)
            // Use mipmap for better compatibility with Razorpay SDK
            checkout.setImage(R.mipmap.ic_launcher)

            val options = JSONObject().apply {
                put("name", RazorpayConfig.APP_NAME)
                put("description", RazorpayConfig.DESCRIPTION)
                put("theme.color", RazorpayConfig.THEME_COLOR)
                put("currency", RazorpayConfig.CURRENCY)
                put("amount", Math.round(currentTotal * 100))
                
                val prefill = JSONObject()
                auth.currentUser?.email?.let { if (it.isNotEmpty()) prefill.put("email", it) }
                auth.currentUser?.phoneNumber?.let { if (it.isNotEmpty()) prefill.put("contact", it) }
                
                if (prefill.length() > 0) {
                    put("prefill", prefill)
                }
                
                // Add retry options to handle network issues
                put("retry", JSONObject().apply {
                    put("enabled", true)
                    put("max_count", 4)
                })
                
                // Add "send_sms_hash" for automatic OTP reading if needed
                put("send_sms_hash", true)
            }
            
            binding.btnCheckout.isEnabled = false
            android.util.Log.d("ShopixCheckout", "Opening Razorpay Checkout with options: $options")
            checkout.open(this, options)
        } catch (e: Exception) {
            binding.btnCheckout.isEnabled = true
            android.util.Log.e("ShopixCheckout", "Critical error in Razorpay Checkout: ${e.message}", e)
            Toast.makeText(this, "Could not start payment: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onPaymentSuccess(razorpayPaymentId: String?, paymentData: PaymentData?) {
        Toast.makeText(this, "Payment successful!", Toast.LENGTH_SHORT).show()
        saveOrderToFirestore(razorpayPaymentId ?: "")
    }

    override fun onPaymentError(errorCode: Int, errorDescription: String?, paymentData: PaymentData?) {
        binding.btnCheckout.isEnabled = true
        Toast.makeText(this, "Payment failed: $errorDescription", Toast.LENGTH_LONG).show()
    }

    private fun saveOrderToFirestore(paymentId: String) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            android.util.Log.e("ShopixCheckout", "User ID is null during saveOrderToFirestore")
            Toast.makeText(this, "Authentication error. Please try again.", Toast.LENGTH_SHORT).show()
            binding.btnCheckout.isEnabled = true
            return
        }

        if (cartItems.isEmpty()) {
            android.util.Log.e("ShopixCheckout", "Cart items empty during saveOrderToFirestore")
            return
        }

        val sellerIds = cartItems.map { it.sellerId }.distinct()
        val counterRef = db.collection("counters").document("orders")

        db.runTransaction { transaction ->
            val snap = transaction.get(counterRef)
            val next = (snap.getLong("count") ?: 0L) + 1
            transaction.set(counterRef, mapOf("count" to next))
            
            val orderId = "ord%06d".format(next)
            
            val orderData = hashMapOf(
                "orderId" to orderId,
                "userId" to uid,
                "sellerIds" to sellerIds,
                "items" to cartItems.map {
                    hashMapOf(
                        "productId" to it.productId,
                        "name" to it.name,
                        "price" to it.price,
                        "salePrice" to it.salePrice,
                        "quantity" to it.quantity,
                        "imageUrl" to it.imageUrl,
                        "sellerId" to it.sellerId,
                        "skuId" to it.skuId,
                        "gstNo" to it.gstNo
                    )
                },
                "totalAmount" to currentTotal,
                "status" to "pending",
                "paymentId" to paymentId,
                "placedAt" to com.google.firebase.Timestamp.now()
            )
            transaction.set(db.collection("orders").document(orderId), orderData)
            next
        }.addOnSuccessListener { next ->
            val orderId = "ord%06d".format(next)
            clearCart(uid) {
                val intent = Intent(this, OrderSuccessActivity::class.java).apply {
                    putExtra("orderId", orderId)
                    putExtra("totalAmount", currentTotal)
                    putExtra("status", "Paid")
                    putExtra("timestamp", System.currentTimeMillis())
                }
                startActivity(intent)
                finish()
            }
        }.addOnFailureListener { e ->
            binding.btnCheckout.isEnabled = true
            Toast.makeText(this, "Order failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupRecyclerView() {
        val uid = auth.currentUser?.uid ?: return
        cartAdapter = CartAdapter(
            items = emptyList(),
            onRemove = { item -> 
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Remove Item")
                    .setMessage("Are you sure you want to remove this item from your cart?")
                    .setPositiveButton("Remove") { _, _ -> removeCartItem(item) }
                    .setNegativeButton("Cancel", null)
                    .show()
            },
            onQtyChange = { item, newQty ->
                db.collection("users").document(uid).collection("cart").document(item.id).update("quantity", newQty)
            }
        )
        binding.rvCart.layoutManager = LinearLayoutManager(this)
        binding.rvCart.adapter = cartAdapter
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setColorSchemeResources(R.color.shopix_primary)
        binding.swipeRefresh.setOnRefreshListener {
            loadCartFromFirestore()
        }
    }

    private fun loadCartFromFirestore() {
        val uid = auth.currentUser?.uid ?: return
        binding.swipeRefresh.isRefreshing = true
        
        cartListener?.remove()
        cartListener = db.collection("users").document(uid).collection("cart")
            .addSnapshotListener { snapshots, error ->
                binding.swipeRefresh.isRefreshing = false
                if (error != null) {
                    Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                val newList = snapshots?.documents?.mapNotNull { doc ->
                    CartItem(
                        id = doc.id,
                        productId = doc.getString("productId") ?: "",
                        name = doc.getString("name") ?: "",
                        price = doc.getDouble("price") ?: 0.0,
                        salePrice = doc.getDouble("salePrice") ?: 0.0,
                        imageUrl = doc.getString("imageUrl") ?: "",
                        quantity = (doc.getLong("quantity") ?: 1).toInt(),
                        sellerId = doc.getString("sellerId") ?: "",
                        skuId = doc.getString("skuId") ?: "",
                        gstNo = doc.getString("gstNo") ?: ""
                    )
                } ?: emptyList()

                cartItems.clear()
                cartItems.addAll(newList)
                cartAdapter.updateList(newList)
                updateOrderSummary()
                toggleEmptyState()
            }
    }

    private fun removeCartItem(item: CartItem) {
        val uid = auth.currentUser?.uid ?: return
        if (item.id.isNotEmpty()) {
            db.collection("users").document(uid).collection("cart").document(item.id).delete()
        }
    }

    private fun clearCart(uid: String, onDone: () -> Unit) {
        db.collection("users").document(uid).collection("cart").get().addOnSuccessListener { snapshot ->
            val batch = db.batch()
            snapshot.documents.forEach { batch.delete(it.reference) }
            batch.commit().addOnSuccessListener { onDone() }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.bottomNav.selectedItemId = R.id.nav_cart
        loadCartFromFirestore()
    }

    override fun onPause() {
        super.onPause()
        cartListener?.remove()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putDouble("currentTotal", currentTotal)
        outState.putParcelableArrayList("cartItems", ArrayList(cartItems))
    }

    private fun updateOrderSummary() {
        val subtotal = cartItems.sumOf { it.salePrice * it.quantity }
        val shipping = if (subtotal > 0) 40.0 else 0.0
        val total = subtotal + shipping
        binding.txtSubtotal.text = "₹%.2f".format(subtotal)
        binding.txtShipping.text = "₹%.2f".format(shipping)
        binding.txtTotal.text = "₹%.2f".format(total)
        binding.txtItems.text = "${cartItems.size} items"
    }

    private fun toggleEmptyState() {
        val isEmpty = cartItems.isEmpty()
        binding.rvCart.visibility = if (isEmpty) View.GONE else View.VISIBLE
        binding.layoutEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.cardSummary.visibility = if (isEmpty) View.GONE else View.VISIBLE
        
        if (isEmpty) {
            binding.btnContinueShopping.setOnClickListener {
                val intent = Intent(this, HomeActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                startActivity(intent)
                finish()
            }
        }
    }
}