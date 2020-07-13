package com.example.calendar.features;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.example.calendar.R;
import com.example.calendar.application.MyApplication;
import com.google.firebase.auth.FirebaseAuth;

public class PreferenceActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (MyApplication.getInstance().isNightModeEnabled()) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
        setContentView(R.layout.activity_preference);
        setUpToolbar();

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.preferences, new MainPreference())
                    .commit();
        }
    }

    public static class MainPreference extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.main_preference, rootKey);

            Preference logOutPref = findPreference("log_out");
            assert logOutPref != null;
            logOutPref.setOnPreferenceClickListener(preference -> {
                logOut();
                return true;
            });

            SwitchPreferenceCompat themeSwitchPref = findPreference("dark_theme");
            assert themeSwitchPref != null;
            themeSwitchPref.setOnPreferenceClickListener(preference -> {
                SharedPreferences switchPrefStatus = PreferenceManager.
                        getDefaultSharedPreferences(getContext());
                boolean isNightModeOn = switchPrefStatus.getBoolean("dark_theme", false);

                MyApplication.getInstance().setIsNightModeEnabled(isNightModeOn);
                getActivity().recreate();
                return true;
            });
        }
        private void logOut() {
            if (getActivity() != null) {
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(getActivity(), LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                getActivity().finish();
            }
        }
    }

    //    @Override
//    public boolean onPreferenceStartFragment(PreferenceFragmentCompat caller, Preference pref) {
//        return false;
//    }

    private void setUpToolbar() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
    }
}