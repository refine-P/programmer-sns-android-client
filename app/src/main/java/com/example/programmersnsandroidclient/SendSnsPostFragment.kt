package com.example.programmersnsandroidclient

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.programmersnsandroidclient.databinding.FragmentSendSnsPostBinding
import com.example.programmersnsandroidclient.sns.SnsModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SendSnsPostFragment : Fragment() {
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentSendSnsPostBinding.inflate(inflater, container, false)
        binding.sendButton.setOnClickListener {
            scope.launch {
                SnsModel.sendSnsPost(binding.snsPostContent.text.toString())
            }
            findNavController().popBackStack()
        }
        return binding.root
    }
}