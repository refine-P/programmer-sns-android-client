package com.example.programmersnsandroidclient.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.programmersnsandroidclient.databinding.FragmentEditUserProfileBinding
import com.example.programmersnsandroidclient.viewmodel.SnsUserViewModel

class EditUserProfileFragment : Fragment() {
    companion object {
        const val UPDATE_SUCCESSFUL = "UPDATE_SUCCESSFUL"
    }

    private val snsUserViewModel: SnsUserViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentEditUserProfileBinding.inflate(inflater, container, false)
        binding.viewModel = snsUserViewModel
        binding.lifecycleOwner = viewLifecycleOwner

        val savedStateHandle = findNavController().previousBackStackEntry!!.savedStateHandle
        binding.updateButton.setOnClickListener {
            snsUserViewModel.updateSuccessful.observe(viewLifecycleOwner) {
                savedStateHandle.set(UPDATE_SUCCESSFUL, it)
                findNavController().popBackStack()
            }
            snsUserViewModel.updateUserProfile(
                binding.snsUserName.text.toString(),
                binding.snsUserDiscription.text.toString()
            )
        }

        return binding.root
    }
}