package com.example.shopix_admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.shopix_admin.databinding.ItemAdminUserBinding

class AdminUsersAdapter(
    private val items: MutableList<AdminUser>,
    private val onAction: (AdminUser, String) -> Unit
) : RecyclerView.Adapter<AdminUsersAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemAdminUserBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemAdminUserBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = items[position]
        with(holder.binding) {
            txtName.text = user.fullName
            txtEmail.text = user.email
            txtPhone.text = user.phone
            txtCity.text = "${user.city}, ${user.state}"
            txtJoined.text = "Joined: ${user.createdAt}"
            
            // Set avatar text
            txtAvatar.text = if (user.fullName.isNotEmpty()) user.fullName[0].uppercase().toString() else "U"

            if (user.isBlocked) {
                txtStatus.text = "🚫 Blocked"
                txtStatus.setTextColor(root.context.getColor(R.color.admin_error))
                btnBlock.text = "Unblock"
                btnBlock.backgroundTintList = root.context.getColorStateList(R.color.admin_success)
                btnBlock.setOnClickListener { onAction(user, "unblock") }
            } else {
                txtStatus.text = "✅ Active"
                txtStatus.setTextColor(root.context.getColor(R.color.admin_success))
                btnBlock.text = "Block"
                btnBlock.backgroundTintList = root.context.getColorStateList(R.color.admin_error)
                btnBlock.setOnClickListener { onAction(user, "block") }
            }
        }
    }

    override fun getItemCount() = items.size

    fun updateList(newList: List<AdminUser>) {
        items.clear()
        items.addAll(newList)
        notifyDataSetChanged()
    }
}