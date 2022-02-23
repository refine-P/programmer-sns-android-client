package com.example.programmersnsandroidclient.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.programmersnsandroidclient.databinding.FragmentUserContentsBinding
import com.example.programmersnsandroidclient.model.TimelineState
import com.example.programmersnsandroidclient.view.adapter.LoadMoreAdapter
import com.example.programmersnsandroidclient.view.adapter.LoadMoreArgs
import com.example.programmersnsandroidclient.view.adapter.TimelineAdapter
import com.example.programmersnsandroidclient.viewmodel.SnsUserContentsViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class UserContentsFragment : Fragment() {
    private val snsUserContentsViewModel: SnsUserContentsViewModel by viewModels()
    private val args: UserContentsFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        snsUserContentsViewModel.init(args.userId)
        val binding = FragmentUserContentsBinding.inflate(inflater, container, false)

        binding.viewModel = snsUserContentsViewModel
        binding.lifecycleOwner = viewLifecycleOwner

        val timelineAdapter = TimelineAdapter()
        val loadMoreAdapter =
            LoadMoreAdapter(viewLifecycleOwner, LoadMoreArgs(snsUserContentsViewModel.isLoading) {
                snsUserContentsViewModel.loadMore()
            })

        val recyclerView = binding.timeline
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.addItemDecoration(
            DividerItemDecoration(context, LinearLayoutManager(context).orientation)
        )
        recyclerView.adapter = ConcatAdapter(timelineAdapter, loadMoreAdapter)

        snsUserContentsViewModel.timeline.observe(viewLifecycleOwner) { timeline ->
            // タイムラインのInit or Refresh時は最上部にスクロールする
            when (timeline.state) {
                TimelineState.INIT, TimelineState.REFRESH -> {
                    timelineAdapter.submitList(timeline.contents) {
                        recyclerView.scrollToPosition(0)
                    }
                }
                else -> {
                    timelineAdapter.submitList(timeline.contents)
                }
            }
        }
        return binding.root
    }
}