package com.shopix.buyer

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.shopix.buyer.databinding.ActivityOrderSuccessBinding

class OrderSuccessActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityOrderSuccessBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ✅ Now receives real data from CartActivity
        val totalAmount = intent.getDoubleExtra("totalAmount", 0.0)
        val orderId = intent.getStringExtra("orderId") ?: "—"
        val placedAt = intent.getStringExtra("placedAt") ?: "—"
        val status = intent.getStringExtra("status") ?: "Paid"

        binding.txtTotal.text = "₹%.2f".format(totalAmount)
        binding.txtOrderId.text = "#$orderId"
        binding.txtPlacedAt.text = placedAt
        binding.txtStatus.text = status

        binding.btnViewOrders.setOnClickListener {
            startActivity(Intent(this, OrdersActivity::class.java))
            finish()
        }
        
        // FIX 6: OrderSuccessActivity continue shopping button
        binding.btnContinue.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
            finish()
        }
    }
}