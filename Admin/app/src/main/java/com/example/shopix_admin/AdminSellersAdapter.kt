package com.example.shopix_admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.shopix_admin.databinding.ItemAdminSellerBinding

class AdminSellersAdapter(
    private val items: MutableList<AdminSeller>,
    private val onAction: (AdminSeller, String) -> Unit
) : RecyclerView.Adapter<AdminSellersAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemAdminSellerBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemAdminSellerBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val seller = items[position]
        with(holder.binding) {
            txtName.text = seller.fullName
            txtShop.text = seller.shopName
            txtEmail.text = seller.email
            txtGstin.text = "GSTIN: ${seller.gstin}"
            txtIncome.text = "₹%.2f".format(seller.totalIncome)
            txtJoined.text = "Joined: ${seller.createdAt}"

            // Verified badge and button text
            if (seller.isVerified) {
                txtVerified.text = "✅ Verified"
                txtVerified.setTextColor(root.context.getColor(R.color.admin_success))
                btnVerify.text = "Unverify"
                btnVerify.backgroundTintList = root.context.getColorStateList(R.color.admin_warning)
            } else {
                txtVerified.text = "⏳ Pending"
                txtVerified.setTextColor(root.context.getColor(R.color.admin_warning))
                btnVerify.text = "Verify"
                btnVerify.backgroundTintList = root.context.getColorStateList(R.color.admin_primary)
            }

            // Block status and button text
            if (seller.isBlocked) {
                txtBlocked.text = "🚫 Blocked"
                txtBlocked.setTextColor(root.context.getColor(R.color.admin_error))
                btnBlockUnblock.text = "Unblock"
                btnBlockUnblock.backgroundTintList = root.context.getColorStateList(R.color.admin_success)
                btnBlockUnblock.setOnClickListener { onAction(seller, "unblock") }
            } else {
                txtBlocked.text = "✅ Active"
                txtBlocked.setTextColor(root.context.getColor(R.color.admin_success))
                btnBlockUnblock.text = "Block"
                btnBlockUnblock.backgroundTintList = root.context.getColorStateList(R.color.admin_error)
                btnBlockUnblock.setOnClickListener { onAction(seller, "block") }
            }

            btnVerify.setOnClickListener { onAction(seller, "verify") }
        }
    }

    override fun getItemCount() = items.size

    fun updateList(newList: List<AdminSeller>) {
        items.clear()
        items.addAll(newList)
        notifyDataSetChanged()
    }
}