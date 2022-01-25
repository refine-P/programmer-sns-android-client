package com.example.programmersnsandroidclient

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.example.programmersnsandroidclient.databinding.LoadMoreBinding

class LoadMoreAdapter(
    private val viewLifecycleOwner: LifecycleOwner,
    private val viewModel: SnsViewModel
) : RecyclerView.Adapter<LoadMoreAdapter.ViewHolder>() {
    class ViewHolder(private val binding: LoadMoreBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(viewLifecycleOwner: LifecycleOwner, viewModel: SnsViewModel) {
            binding.lifecycleOwner = viewLifecycleOwner
            binding.viewModel = viewModel
        }
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LoadMoreBinding.inflate(LayoutInflater.from(viewGroup.context), viewGroup, false)

        return ViewHolder(view)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        viewHolder.bind(viewLifecycleOwner, viewModel)
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = 1
}