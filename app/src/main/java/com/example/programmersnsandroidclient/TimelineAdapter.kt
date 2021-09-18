package com.example.programmersnsandroidclient

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.programmersnsandroidclient.sns.SnsContent

private object DiffCallback : DiffUtil.ItemCallback<SnsContent>() {
    override fun areItemsTheSame(oldItem: SnsContent, newItem: SnsContent): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: SnsContent, newItem: SnsContent): Boolean {
        return oldItem == newItem
    }
}

class TimelineAdapter : ListAdapter<SnsContent, TimelineAdapter.ViewHolder>(DiffCallback) {
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val userName: TextView = view.findViewById(R.id.user_name)
        val content: TextView = view.findViewById(R.id.content)
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.sns_post, viewGroup, false)

        return ViewHolder(view)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        // Get element from your dataset at this position and replace the
        // contents of the view with that element
        val snsContent = getItem(position)
        viewHolder.userName.text = snsContent.userName
        viewHolder.content.text = snsContent.content
    }
}