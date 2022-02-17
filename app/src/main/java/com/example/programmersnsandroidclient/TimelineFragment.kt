package com.example.programmersnsandroidclient

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.programmersnsandroidclient.databinding.FragmentTimelineBinding
import com.google.android.material.snackbar.Snackbar

class TimelineFragment : Fragment() {
    private val snsViewModel: SnsViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentTimelineBinding.inflate(inflater, container, false)
        binding.viewModel = snsViewModel
        binding.lifecycleOwner = viewLifecycleOwner

        val timelineAdapter = TimelineAdapter()
        val loadMoreAdapter = LoadMoreAdapter(viewLifecycleOwner, snsViewModel)

        val recyclerView = binding.timeline
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.addItemDecoration(
            DividerItemDecoration(context, LinearLayoutManager(context).orientation)
        )
        recyclerView.adapter = ConcatAdapter(timelineAdapter, loadMoreAdapter)

        // TODO: refreshしたときにもトップにスクロールする
        // timelineにどの操作によって更新されたかの状態を持たせるといいかも？
        snsViewModel.timeline.observe(viewLifecycleOwner) { timeline ->
            // We need to scroll to the top when we fetch the TL for the first time.
            if (timelineAdapter.itemCount == 0) {  // The first time to fetch the TL.
                timelineAdapter.submitList(timeline) { recyclerView.scrollToPosition(0) }
            } else {
                timelineAdapter.submitList(timeline)
            }
        }

        binding.postButton.setOnClickListener {
            findNavController().navigate(R.id.action_send)
        }

        val savedStateHandle = findNavController().currentBackStackEntry!!.savedStateHandle
        savedStateHandle.getLiveData<Boolean>(SendSnsPostFragment.SEND_SUCCESSFUL).observe(viewLifecycleOwner) {
            val message = if (it) {
                R.string.send_success
            } else {
                R.string.send_failure
            }
            Snackbar.make(binding.sendSnackbar, message, Snackbar.LENGTH_SHORT).show()

            // 値を使うのは一回だけにしたいので、使ったら削除する
            savedStateHandle.remove<Boolean>(SendSnsPostFragment.SEND_SUCCESSFUL)
        }
        return binding.root
    }
}