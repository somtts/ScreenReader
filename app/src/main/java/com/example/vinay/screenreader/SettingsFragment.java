package com.example.vinay.screenreader;

import android.os.Bundle;
import android.preference.PreferenceFragment;

/**
 * Created by Vinay on 01-07-2016.
 */
public  class SettingsFragment extends PreferenceFragment {

    public void OnCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);
    }
}
