package com.example.programmersnsandroidclient.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.programmersnsandroidclient.databinding.FragmentSendSnsPostBinding
import com.example.programmersnsandroidclient.viewmodel.SendSnsPostViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SendSnsPostFragment : Fragment() {
    companion object {
        const val SEND_SUCCESSFUL = "SEND_SUCCESSFUL"
    }

    private val sendSnsPostViewModel: SendSnsPostViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentSendSnsPostBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner

        val savedStateHandle = findNavController().previousBackStackEntry!!.savedStateHandle
        binding.sendButton.setOnClickListener {
            sendSnsPostViewModel.sendSuccessful.observe(viewLifecycleOwner) {
                savedStateHandle.set(SEND_SUCCESSFUL, it)
                findNavController().popBackStack()
            }
            sendSnsPostViewModel.sendSnsPost(binding.snsPostContent.text.toString())
        }

        return binding.root
    }
}