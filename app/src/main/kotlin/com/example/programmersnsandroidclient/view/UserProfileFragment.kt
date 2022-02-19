package com.example.programmersnsandroidclient.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.programmersnsandroidclient.R
import com.example.programmersnsandroidclient.databinding.FragmentUserProfileBinding
import com.example.programmersnsandroidclient.viewmodel.SnsViewModel
import com.google.android.material.snackbar.Snackbar

class UserProfileFragment : Fragment() {
    private val snsViewModel: SnsViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentUserProfileBinding.inflate(inflater, container, false)
        binding.viewModel = snsViewModel
        binding.lifecycleOwner = viewLifecycleOwner

        binding.editButton.setOnClickListener {
            findNavController().navigate(R.id.action_edit_user_profile)
        }

        val currentBackStackEntry = findNavController().currentBackStackEntry!!
        val savedStateHandle = currentBackStackEntry.savedStateHandle
        savedStateHandle.getLiveData<Boolean>(EditUserProfileFragment.UPDATE_SUCCESSFUL)
            .observe(currentBackStackEntry) {
                val message = if (it) {
                    R.string.update_success
                } else {
                    R.string.update_failure
                }
                Snackbar.make(binding.updateSnackbar, message, Snackbar.LENGTH_SHORT).show()

                // 値を使うのは一回だけにしたいので、使ったら削除する
                savedStateHandle.remove<Boolean>(EditUserProfileFragment.UPDATE_SUCCESSFUL)
            }

        return binding.root
    }
}
