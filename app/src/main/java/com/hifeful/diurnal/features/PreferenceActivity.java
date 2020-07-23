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
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.hifeful.diurnal.BuildConfig;
import com.hifeful.diurnal.R;
import com.hifeful.diurnal.application.MyApplication;
import com.hifeful.diurnal.data.User;
import com.google.firebase.auth.FirebaseAuth;
import com.makeramen.roundedimageview.RoundedImageView;
import com.squareup.picasso.Picasso;

import java.util.Locale;

public class PreferenceActivity extends AppCompatActivity {
    private RoundedImageView mUserPhoto;
    private TextView mUsername;
    private TextView mUserEmail;
    private TextView mUserPhotoLimit;
    private ProgressBar mUserPhotoCount;

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

        // UI
        CardView mUserCard = findViewById(R.id.user_info);
        mUserPhoto = mUserCard.findViewById(R.id.user_photo);
        mUsername = mUserCard.findViewById(R.id.user_name);
        mUserEmail = mUserCard.findViewById(R.id.user_email);
        mUserPhotoLimit = mUserCard.findViewById(R.id.user_photos_message);
        mUserPhotoCount = mUserCard.findViewById(R.id.user_photos_count);

        mUser = getIntent().getParcelableExtra("user");

        fillUserInformation();

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.preferences, new MainPreference())
                    .commit();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    public static class MainPreference extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.main_preference, rootKey);

            Preference versionPref = findPreference("version");
            assert versionPref != null;
            versionPref.setSummary(BuildConfig.VERSION_NAME);

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

    private void setUpPhotosCount() {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();

        firestore.collection("users").document(firebaseUser.getUid()).get()
                .addOnCompleteListener(task -> {
                    Locale currentLocale;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
                        currentLocale = getResources().getConfiguration().getLocales().get(0);
                    } else {
                        currentLocale = getResources().getConfiguration().locale;
                    }
                    Long photosCount;
                    if (task.isSuccessful()) {
                        DocumentSnapshot snapshot = task.getResult();
                        if (snapshot != null && snapshot.exists()) {
                            photosCount = snapshot.getLong("photo_count");
                        } else {
                            photosCount = 0L;
                        }
                    } else {
                        photosCount = 0L;
                    }
                    String photoInfo = String.format(currentLocale, " (%d/50)", photosCount);
                    mUserPhotoLimit.append(photoInfo);
                    mUserPhotoCount.setProgress(photosCount.intValue());
                });
    }

    private void fillUserInformation() {
        Picasso.with(this).load(mUser.getPhotoUrl()).into(mUserPhoto);

        mUsername.setText(mUser.getUsername());
        mUserEmail.setText(mUser.getEmail());
        setUpPhotosCount();
    }
}