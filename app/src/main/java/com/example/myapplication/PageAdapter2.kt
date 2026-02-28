package com.example.myapplication

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter

class PageAdapter2(fragmentManager: FragmentManager, lifecycle: Lifecycle,private val accessToken: String) :
FragmentStateAdapter(fragmentManager, lifecycle) {

    var fragments: ArrayList<Fragment> = arrayListOf(
        createFragmentWithToken(HistoryBodyDataDay()),
        createFragmentWithToken(HistoryBodyDataWeek()),
        createFragmentWithToken(HistoryBodyDataMonth()),
        createFragmentWithToken(HistoryBodyDataYear())
    )

    private fun createFragmentWithToken(fragment: Fragment): Fragment {
        val bundle = Bundle()
        bundle.putString("accessToken", accessToken)
        fragment.arguments = bundle
        return fragment
    }

    override fun getItemCount(): Int {
        return fragments.size
    }

    override fun createFragment(position: Int): Fragment {
        return fragments[position]
    }
}