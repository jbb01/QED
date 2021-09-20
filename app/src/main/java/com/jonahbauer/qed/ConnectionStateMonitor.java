package com.jonahbauer.qed;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Handler;

import androidx.annotation.NonNull;

/**
 * Listens to network connection and reports to application
 *
 * Only network connection, not internet connection is checked
 */
public class ConnectionStateMonitor extends ConnectivityManager.NetworkCallback {
    private final Application mApplication;
    private final NetworkRequest mNetworkRequest;
    private final Handler mHandler;

    ConnectionStateMonitor(Application application) {
        mNetworkRequest = new NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR).addTransportType(NetworkCapabilities.TRANSPORT_WIFI).build();
        mApplication = application;
        mHandler = new Handler(mApplication.getMainLooper());
    }

    void enable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) mApplication.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            connectivityManager.registerNetworkCallback(mNetworkRequest, this);
        }
    }

    void disable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) mApplication.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            connectivityManager.unregisterNetworkCallback(this);
        }
    }

    @Override
    public void onAvailable(@NonNull Network network) {
        mHandler.post(() -> mApplication.setOnline(true));
    }

    @Override
    public void onLost(@NonNull Network network) {
        mHandler.post(() -> mApplication.setOnline(false));
    }

    @Override
    public void onUnavailable() {
        mHandler.post(() -> mApplication.setOnline(false));
    }
}