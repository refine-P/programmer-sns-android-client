package com.example.programmersnsandroidclient

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.programmersnsandroidclient.databinding.FragmentEditUserProfileBinding

class EditUserProfileFragment : Fragment() {
    private val snsViewModel: SnsViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentEditUserProfileBinding.inflate(inflater, container, false)
        binding.viewModel = snsViewModel
        binding.lifecycleOwner = viewLifecycleOwner

        binding.updateButton.setOnClickListener {
            snsViewModel.updateUserProfile(
                binding.snsUserName.text.toString(),
                binding.snsUserDiscription.text.toString()
            )
            findNavController().popBackStack()
        }

        val prefs = activity?.getPreferences(Context.MODE_PRIVATE) ?: return binding.root
        snsViewModel.currentUser.observe(viewLifecycleOwner) {
            with(prefs.edit()) {
                putString("user_id", it.id)
                apply()
            }
        }

        return binding.root
    }
}