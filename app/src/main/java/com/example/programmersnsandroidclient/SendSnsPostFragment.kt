package com.example.programmersnsandroidclient

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.programmersnsandroidclient.databinding.FragmentSendSnsPostBinding

class SendSnsPostFragment : Fragment() {
    private val snsViewModel: SnsViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentSendSnsPostBinding.inflate(inflater, container, false)
        binding.sendButton.setOnClickListener {
            snsViewModel.sendSnsPost(binding.snsPostContent.text.toString())
            findNavController().popBackStack()
        }
        return binding.root
    }
}