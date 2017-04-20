package com.afterroot.photos;

import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.afterroot.photos.fragment.SettingsFragment;

public class CustomFragmentActivity extends AppCompatActivity {

    public static final int SETTINGS_FRAGMENT = 2;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_fragment);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(getFragmentTitle());

        initFragments();
    }

    public void initFragments(){
        switch (getFragmentId()){
            case SETTINGS_FRAGMENT:
                SettingsFragment settingsFragment = new SettingsFragment();
                setFragment(settingsFragment);
                break;
        }
    }

    public String getFragmentTitle(){
        return getIntent().getStringExtra(getString(R.string.key_fragment_title));
    }

    private int getFragmentId(){
        int extra = getIntent().getIntExtra(getString(R.string.key_fragment_id), 0);
        Log.d(getString(R.string.key_fragment_id), "id: " + extra);
        return extra;
    }

    FragmentManager mFragmentManager;
    private void setFragment(Fragment fragment){
        if (mFragmentManager == null){
            mFragmentManager = getFragmentManager();
            mFragmentManager.beginTransaction().replace(R.id.content_custom_fragment, fragment).commit();
        } else {
            mFragmentManager.beginTransaction().replace(R.id.content_custom_fragment, fragment).commit();
        }
    }
}
