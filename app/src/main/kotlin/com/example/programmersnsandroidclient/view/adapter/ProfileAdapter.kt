package com.example.programmersnsandroidclient.view.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.RecyclerView
import com.example.programmersnsandroidclient.databinding.UserProfileBinding
import com.example.programmersnsandroidclient.model.SnsUser

class ProfileAdapter(
    private val lifecycleOwner: LifecycleOwner,
    private val user: LiveData<SnsUser>
) : RecyclerView.Adapter<ProfileAdapter.ViewHolder>() {
    class ViewHolder(private val binding: UserProfileBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(lifecycleOwner: LifecycleOwner, user: LiveData<SnsUser>) {
            binding.lifecycleOwner = lifecycleOwner
            binding.user = user
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = UserProfileBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(lifecycleOwner, user)
    }

    override fun getItemCount() = 1
}