package com.example.vinay.screenreader;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Toast;

public class ServiceSettingsActivity extends PreferenceActivity
implements SharedPreferences.OnSharedPreferenceChangeListener{

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new MyPreferenceFragment()).commit();

        checkValues();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if(key.equals("pref_on_off")){
           boolean status = sharedPreferences.getBoolean("pref_on_off",false);

            startActivityForResult(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS), 0);

        }
        else if(key.equals("language")){

        }
    }

    public static class MyPreferenceFragment extends PreferenceFragment
    {
        @Override
        public void onCreate(final Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);
        }
    }

    private void checkValues()
    {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        boolean on_off = sharedPrefs.getBoolean("pref_on_off", false);
        String language = sharedPrefs.getString("language","1");

        String msg = "Cur Values: ";
        msg += "\n Status = " + on_off;
        msg += "\n language = " + language;

        Toast.makeText(getApplication(), msg, Toast.LENGTH_SHORT).show();
    }



}
