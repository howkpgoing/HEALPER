
package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Button
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.core.view.WindowCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.ui.AppBarConfiguration
import androidx.viewpager2.widget.ViewPager2
import com.example.myapplication.databinding.ActivityMainBinding
import com.google.android.material.navigation.NavigationView
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class HistoryBodyData : AppCompatActivity() {
    private var accessToken = ""
    private val FitBitAPITool = FitBitAPI()
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var toolbar: Toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.history_body_data)
        viewPager = findViewById(R.id.viewPager2)
        tabLayout = findViewById(R.id.tabLayout)
        accessToken = intent.getStringExtra("accessToken").toString()
        FitBitAPITool.setAccessToken(accessToken)
        Log.e("access token test hsd", FitBitAPITool.getAccessToken())
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val pageAdapter = PageAdapter2(supportFragmentManager, lifecycle,accessToken)
        viewPager.adapter = pageAdapter
        val title: ArrayList<String> = arrayListOf("日", "週", "月","年")

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = title[position]
        }.attach()
        toolbar = findViewById(R.id.toolbar)
        drawerLayout = findViewById(R.id.drawerlayout)
        navigationView = findViewById(R.id.navigationView)
        setSupportActionBar(toolbar)
        val toggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            toolbar,
            R.string.navigration_open,
            R.string.navigration_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        navigationView.setNavigationItemSelectedListener { item: MenuItem ->
            drawerLayout.closeDrawer(GravityCompat.START)
            when (item.itemId) {
                R.id.home -> {
                    val intent = Intent(this, MainActivity::class.java)
                    intent.putExtra("accessToken", accessToken)
                    startActivity(intent)
                    true
                }
                R.id.keeper -> {
                    val intent = Intent(this, Keeper::class.java)
                    intent.putExtra("accessToken", accessToken)
                    Log.e("666", accessToken)
                    startActivity(intent)
                    true
                }

                R.id.logout -> {
                    Firebase.auth.signOut()
                    val intent = Intent(this, LoginActivity::class.java)
                    startActivity(intent)
                    true
                }

                R.id.hsd -> {
                    val intent = Intent(this, HistorySportData::class.java)
                    intent.putExtra("accessToken", accessToken)
//                    Log.e("777777777", accessToken)
                    startActivity(intent)
                    true
                }

                R.id.hbd -> {
                    true
                }
                else -> false
            }
    }
}}