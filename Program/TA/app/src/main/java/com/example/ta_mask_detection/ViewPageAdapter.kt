package com.example.ta_mask_detection

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.example.ta_mask_detection.Onboarding1Fragment
import com.example.ta_mask_detection.Onboarding2Fragment
import com.example.ta_mask_detection.Onboarding3Fragment
import com.example.ta_mask_detection.Onboarding4Fragment

class ViewPageAdapter(fragmentManager: FragmentManager) : FragmentPagerAdapter(fragmentManager){

    public val fragments = listOf(
        Onboarding1Fragment(),
        Onboarding2Fragment(),
        Onboarding3Fragment(),
        Onboarding4Fragment()
    )

    override fun getItem(position: Int): Fragment {
        return fragments[position]
    }

    override fun getCount(): Int {
        return fragments.size
    }
}