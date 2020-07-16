package com.hifeful.diurnal.features;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.cardview.widget.CardView;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import com.hifeful.diurnal.R;
import com.hifeful.diurnal.application.MyApplication;
import com.hifeful.diurnal.data.User;
import com.google.firebase.auth.FirebaseAuth;
import com.makeramen.roundedimageview.RoundedImageView;
import com.squareup.picasso.Picasso;

public class PreferenceActivity extends AppCompatActivity {
    // UI
    private CardView mUserCard;
    private RoundedImageView mUserPhoto;
    private TextView mUsername;
    private TextView mUserEmail;

    // Variables
    private User mUser;

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

        mUserCard = findViewById(R.id.user_info);
        mUserPhoto = mUserCard.findViewById(R.id.user_photo);
        mUsername = mUserCard.findViewById(R.id.user_name);
        mUserEmail = mUserCard.findViewById(R.id.user_email);

        mUser = getIntent().getParcelableExtra("user");

        fillUserInformation();

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

        @Override
        public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
            ListView listView = view.findViewById(android.R.id.list);
            if (listView != null) {
                listView.setVerticalScrollBarEnabled(false);
            }

            super.onViewCreated(view, savedInstanceState);
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

    private void fillUserInformation() {
        Picasso.with(this).load(mUser.getPhotoUrl()).into(mUserPhoto);

        mUsername.setText(mUser.getUsername());
        mUserEmail.setText(mUser.getEmail());
    }
}