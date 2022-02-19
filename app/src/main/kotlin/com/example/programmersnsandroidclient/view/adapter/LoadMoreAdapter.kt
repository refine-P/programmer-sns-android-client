package com.example.programmersnsandroidclient.view.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.RecyclerView
import com.example.programmersnsandroidclient.databinding.LoadMoreBinding

data class LoadMoreArgs(
    val isLoading: LiveData<Boolean>,
    val onClickListener: View.OnClickListener,
)

class LoadMoreAdapter(
    private val lifecycleOwner: LifecycleOwner,
    private val loadMoreArgs: LoadMoreArgs
) : RecyclerView.Adapter<LoadMoreAdapter.ViewHolder>() {
    class ViewHolder(private val binding: LoadMoreBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(lifecycleOwner: LifecycleOwner, loadMoreArgs: LoadMoreArgs) {
            binding.lifecycleOwner = lifecycleOwner
            binding.args = loadMoreArgs
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
        viewHolder.bind(lifecycleOwner, loadMoreArgs)
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = 1
}