package com.afterroot.tagit

import android.app.Fragment
import android.app.FragmentManager
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log

import com.afterroot.tagit.fragment.MainFragment
import com.afterroot.tagit.fragment.SettingsFragment

class CustomFragmentActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_custom_fragment)

        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.title = fragmentTitle

        setFragments()
    }

    private fun setFragments() {
        when (fragmentId) {
            SETTINGS_FRAGMENT -> setFragment(SettingsFragment())
            MAIN_FRAGMENT -> setFragment(MainFragment())
        }
    }

    private val fragmentTitle: String
        get() = intent.getStringExtra(getString(R.string.key_fragment_title))

    private val fragmentId: Int
        get() {
            val extra = intent.getIntExtra(getString(R.string.key_fragment_id), 0)
            Log.d(getString(R.string.key_fragment_id), "id: " + extra)
            return extra
        }

    private var mFragmentManager: FragmentManager? = null
    private fun setFragment(fragment: Fragment) {
        if (mFragmentManager == null) {
            mFragmentManager = fragmentManager
        }
        mFragmentManager!!.beginTransaction().replace(R.id.content_custom_fragment, fragment).commit()
    }

    companion object {
        val SETTINGS_FRAGMENT = 2
        val MAIN_FRAGMENT = 1
    }
}
