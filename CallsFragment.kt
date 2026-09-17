package com.mostafa.callmanager

import android.os.Bundle
import android.provider.CallLog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

/**
 * Top-level "Calls" tab. Hosts 4 sub-tabs: All / Outgoing / Incoming / Missed,
 * each backed by a CallListFragment filtered on CallLog.Calls.TYPE.
 */
class CallsFragment : Fragment(R.layout.fragment_calls) {

    private val titles = arrayOf("الكل", "الصادر", "الوارد", "لم يرد")
    private val filters = arrayOf(
        null,
        CallLog.Calls.OUTGOING_TYPE,
        CallLog.Calls.INCOMING_TYPE,
        CallLog.Calls.MISSED_TYPE
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tabLayout = view.findViewById<TabLayout>(R.id.sub_tabs)
        val viewPager = view.findViewById<ViewPager2>(R.id.view_pager)

        viewPager.adapter = object : FragmentStateAdapter(childFragmentManager, viewLifecycleOwner.lifecycle) {
            override fun getItemCount(): Int = filters.size
            override fun createFragment(position: Int): Fragment =
                CallListFragment.newInstance(filters[position])
        }

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = titles[position]
        }.attach()
    }
}
