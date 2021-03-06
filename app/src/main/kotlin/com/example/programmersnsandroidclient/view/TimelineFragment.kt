package com.example.programmersnsandroidclient.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.programmersnsandroidclient.MainNavDirections
import com.example.programmersnsandroidclient.R
import com.example.programmersnsandroidclient.databinding.FragmentTimelineBinding
import com.example.programmersnsandroidclient.model.TimelineState
import com.example.programmersnsandroidclient.view.adapter.LoadMoreAdapter
import com.example.programmersnsandroidclient.view.adapter.LoadMoreArgs
import com.example.programmersnsandroidclient.view.adapter.TimelineAdapter
import com.example.programmersnsandroidclient.viewmodel.TimelineViewModel
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TimelineFragment : Fragment() {
    private val timelineViewModel: TimelineViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentTimelineBinding.inflate(inflater, container, false)
        binding.viewModel = timelineViewModel
        binding.lifecycleOwner = viewLifecycleOwner

        val timelineAdapter = TimelineAdapter { userId, userName ->
            val action = MainNavDirections.actionUserProfile(userId, userName)
            findNavController().navigate(action)
        }
        val loadMoreAdapter =
            LoadMoreAdapter(viewLifecycleOwner, LoadMoreArgs(timelineViewModel.isLoading) {
                timelineViewModel.loadMore()
            })

        val recyclerView = binding.timeline
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.addItemDecoration(
            DividerItemDecoration(context, LinearLayoutManager(context).orientation)
        )
        recyclerView.adapter = ConcatAdapter(timelineAdapter, loadMoreAdapter)

        timelineViewModel.timeline.observe(viewLifecycleOwner) { timeline ->
            // ?????????????????????Init or Refresh???????????????????????????????????????
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

        binding.postButton.setOnClickListener {
            findNavController().navigate(R.id.action_send)
        }

        val savedStateHandle = findNavController().currentBackStackEntry!!.savedStateHandle
        savedStateHandle.getLiveData<Boolean>(SendSnsPostFragment.SEND_SUCCESSFUL)
            .observe(viewLifecycleOwner) {
                val message = if (it) {
                    R.string.send_success
                } else {
                    R.string.send_failure
                }
                Snackbar.make(binding.sendSnackbar, message, Snackbar.LENGTH_SHORT).show()

                // ???????????????????????????????????????Refresh?????????????????????????????????????????????????????????
                if (it) timelineViewModel.refresh()

                // ???????????????????????????????????????????????????????????????????????????
                savedStateHandle.remove<Boolean>(SendSnsPostFragment.SEND_SUCCESSFUL)
            }
        return binding.root
    }
}