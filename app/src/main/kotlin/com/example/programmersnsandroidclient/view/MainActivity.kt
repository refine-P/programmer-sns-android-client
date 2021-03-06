package com.example.programmersnsandroidclient.view

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.example.programmersnsandroidclient.MainNavDirections
import com.example.programmersnsandroidclient.R
import com.example.programmersnsandroidclient.databinding.ActivityMainBinding
import com.example.programmersnsandroidclient.databinding.DrawerHeaderBinding
import com.example.programmersnsandroidclient.viewmodel.SnsUserViewModel
import com.pixplicity.sharp.Sharp
import dagger.hilt.android.AndroidEntryPoint
import jdenticon.Jdenticon

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val snsUserViewModel: SnsUserViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val activityMainBinding = ActivityMainBinding.inflate(layoutInflater)
        activityMainBinding.lifecycleOwner = this
        setContentView(activityMainBinding.root)

        val navHostFragment =
            activityMainBinding.navHostFragment.getFragment<NavHostFragment>()
        val navController = navHostFragment.navController
        val appBarConfiguration =
            AppBarConfiguration(navController.graph, activityMainBinding.drawerLayout)

        activityMainBinding.toolbar.setupWithNavController(navController, appBarConfiguration)
        activityMainBinding.navView.setupWithNavController(navController)

        val userContentsMenu = activityMainBinding.navView.menu.findItem(R.id.user_contents)
        userContentsMenu.isEnabled = false

        val drawerHeaderBinding =
            DrawerHeaderBinding.inflate(layoutInflater, activityMainBinding.navView, false)
        drawerHeaderBinding.lifecycleOwner = this
        drawerHeaderBinding.viewModel = snsUserViewModel

        snsUserViewModel.currentUser.observe(this) { user ->
            userContentsMenu.isEnabled = true
            userContentsMenu.setOnMenuItemClickListener {
                val action = MainNavDirections.actionUserProfile(user.id, user.name)
                navController.navigate(action)
                false  // false ???????????????????????????????????????????????????
            }
            drawerHeaderBinding.userIconHeader.setImageDrawable(
                Sharp.loadString(Jdenticon.toSvg(user.id, 56)).drawable
            )
        }

        // app:headerLayout ??? NavigationView ?????????????????????????????????????????????????????????????????????
        // https://kcpoipoi.hatenablog.com/entry/2019/01/02/204840
        activityMainBinding.navView.addHeaderView(drawerHeaderBinding.root)
    }
}