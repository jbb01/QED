package com.jonahbauer.qed;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;

import androidx.annotation.NonNull;

/**
 * Listens to network connection and reports to application
 *
 * Only network connection, not internet connection is checked
 */
public class ConnectionStateMonitor extends ConnectivityManager.NetworkCallback {
    private final Application mApplication;
    private final NetworkRequest mNetworkRequest;

    ConnectionStateMonitor(Application application) {
        mNetworkRequest = new NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR).addTransportType(NetworkCapabilities.TRANSPORT_WIFI).build();
        this.mApplication = application;
    }

    void enable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) mApplication.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            connectivityManager.registerNetworkCallback(mNetworkRequest, this);
        }
    }

    public void disable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) mApplication.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            connectivityManager.unregisterNetworkCallback(this);
        }
    }

    @Override
    public void onAvailable(@NonNull Network network) {
        mApplication.setOnline(true);
    }

    @Override
    public void onLost(@NonNull Network network) {
        mApplication.setOnline(false);
    }

    @Override
    public void onUnavailable() {
        mApplication.setOnline(false);
    }
}