package com.afterroot.tagit.fragment

import android.os.Bundle
import android.preference.PreferenceFragment

import com.afterroot.tagit.R

class SettingsFragment : PreferenceFragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.preferences_settings)
    }
}
