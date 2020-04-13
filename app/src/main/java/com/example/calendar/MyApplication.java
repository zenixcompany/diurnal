package com.example.calendar;

import android.app.Application;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;

import androidx.annotation.NonNull;

public class MyApplication extends Application {
    private static MyApplication myApplication;

    @Override
    public void onCreate() {
        super.onCreate();

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

        myApplication = this;
    }

    public static synchronized MyApplication getInstance() {
        return myApplication;
    }

    public void setConnectivityListener(ConnectivityReceiver.ConnectivityReceiverListener listener) {
        ConnectivityReceiver.receiverListener = listener;
    }
}
