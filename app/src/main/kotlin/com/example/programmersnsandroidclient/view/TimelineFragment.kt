package com.example.programmersnsandroidclient.view

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
import com.example.programmersnsandroidclient.view.adapter.LoadMoreAdapter
import com.example.programmersnsandroidclient.view.adapter.LoadMoreArgs
import com.example.programmersnsandroidclient.R
import com.example.programmersnsandroidclient.view.adapter.TimelineAdapter
import com.example.programmersnsandroidclient.databinding.FragmentTimelineBinding
import com.example.programmersnsandroidclient.model.TimelineState
import com.example.programmersnsandroidclient.viewmodel.SnsViewModel
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
        val loadMoreAdapter =
            LoadMoreAdapter(viewLifecycleOwner, LoadMoreArgs(snsViewModel.isLoading) {
                snsViewModel.loadMore()
            })

        val recyclerView = binding.timeline
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.addItemDecoration(
            DividerItemDecoration(context, LinearLayoutManager(context).orientation)
        )
        recyclerView.adapter = ConcatAdapter(timelineAdapter, loadMoreAdapter)

        snsViewModel.timeline.observe(viewLifecycleOwner) { timeline ->
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

                // 投稿成功時はタイムラインをRefreshして、自分の投稿が確認できるようにする
                if (it) snsViewModel.refresh()

                // 値を使うのは一回だけにしたいので、使ったら削除する
                savedStateHandle.remove<Boolean>(SendSnsPostFragment.SEND_SUCCESSFUL)
            }
        return binding.root
    }
}