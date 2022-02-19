package com.example.programmersnsandroidclient

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.example.programmersnsandroidclient.databinding.ActivityMainBinding
import com.example.programmersnsandroidclient.databinding.DrawerHeaderBinding
import com.example.programmersnsandroidclient.viewmodel.SnsViewModel

class MainActivity : AppCompatActivity() {
    private val snsViewModel: SnsViewModel by viewModels()

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
        snsViewModel.currentUser.observe(this) {
            userContentsMenu.isEnabled = true
        }

        val drawerHeaderBinding =
            DrawerHeaderBinding.inflate(layoutInflater, activityMainBinding.navView, false)
        drawerHeaderBinding.lifecycleOwner = this
        drawerHeaderBinding.viewModel = snsViewModel

        // app:headerLayout を NavigationView に追加しても動かないので、ここで動的に追加する
        // https://kcpoipoi.hatenablog.com/entry/2019/01/02/204840
        activityMainBinding.navView.addHeaderView(drawerHeaderBinding.root)
    }
}