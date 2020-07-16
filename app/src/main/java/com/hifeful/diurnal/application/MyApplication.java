package com.hifeful.diurnal.application;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

public class MyApplication extends Application {
    private static MyApplication singleton = null;

    // Night mode
    public static final String NIGHT_MODE = "NIGHT_MODE";
    private boolean mIsNightModeEnabled = false;

    @Override
    public void onCreate() {
        super.onCreate();
        singleton = this;

        // Internet connectivity
        ConnectivityManager connectivityManager = (ConnectivityManager)
                this.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkRequest request = new NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build();

        assert connectivityManager != null;
        connectivityManager.registerNetworkCallback(request, new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                if (ConnectivityReceiver.receiverListener != null) {
                    ConnectivityReceiver.receiverListener.onNetworkConnectionChanged(true);
                }
            }

            @Override
            public void onLost(@NonNull Network network) {
                if (ConnectivityReceiver.receiverListener != null) {
                    ConnectivityReceiver.receiverListener.onNetworkConnectionChanged(false);
                }
            }
        });

        // Night mode
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mIsNightModeEnabled = sharedPreferences.getBoolean(NIGHT_MODE, false);

    }

    public static synchronized MyApplication getInstance() {
        if (singleton == null) {
            singleton = new MyApplication();
        }
        return singleton;
    }

    public void setConnectivityListener(ConnectivityReceiver.ConnectivityReceiverListener listener) {
        ConnectivityReceiver.receiverListener = listener;
    }

    public boolean isNightModeEnabled() {
        return mIsNightModeEnabled;
    }

    public void setIsNightModeEnabled(boolean isNightModeEnabled) {
        this.mIsNightModeEnabled = isNightModeEnabled;

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(NIGHT_MODE, isNightModeEnabled);
        editor.apply();
    }
}
