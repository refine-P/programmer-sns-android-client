package com.example.programmersnsandroidclient

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.programmersnsandroidclient.databinding.FragmentSendSnsPostBinding
import com.example.programmersnsandroidclient.viewmodel.SnsViewModel

class SendSnsPostFragment : Fragment() {
    companion object {
        const val SEND_SUCCESSFUL = "SEND_SUCCESSFUL"
    }

    private val snsViewModel: SnsViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentSendSnsPostBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner

        val savedStateHandle = findNavController().previousBackStackEntry!!.savedStateHandle
        binding.sendButton.setOnClickListener {
            snsViewModel.sendSuccessful.observe(viewLifecycleOwner) {
                savedStateHandle.set(SEND_SUCCESSFUL, it)
                findNavController().popBackStack()
            }
            snsViewModel.sendSnsPost(binding.snsPostContent.text.toString())
        }

        return binding.root
    }
}