package eu.jonahbauer.qed;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.HashMap;
import java.util.Map;

/**
 * Monitors the network connection state using the {@link ConnectivityManager}.
 */
public class ConnectionStateMonitor {
    private static final String LOG_TAG = ConnectionStateMonitor.class.getSimpleName();

    private final ConnectivityManager mConnectivityManager;
    private final NetworkRequest mNetworkRequest;

    private final MutableLiveData<State> mConnectionStateRaw = new MutableLiveData<>(State.UNKNOWN);
    private final LiveData<State> mConnectionState;

    private final Map<Network, State> mNetworks = new HashMap<>();
    private final NetworkCallback mCallback = new NetworkCallback();

    {
        var connectionState = new MediatorLiveData<State>();
        connectionState.addSource(mConnectionStateRaw, state -> {
            if (state != connectionState.getValue()) {
                connectionState.setValue(state);
            }
        });
        mConnectionState = connectionState;
    }

    ConnectionStateMonitor(Context context) {
        mConnectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        mNetworkRequest = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                .build();
        if (BuildConfig.DEBUG) {
            mConnectionState.observeForever(state -> {
                Log.d(LOG_TAG, "Connection state changed to " + state);
            });
        }
    }

    void enable() {
        if (mConnectivityManager != null) {
            mConnectionStateRaw.setValue(State.NOT_CONNECTED);
            mConnectivityManager.registerNetworkCallback(mNetworkRequest, mCallback);
        }
    }

    void disable() {
        if (mConnectivityManager != null) {
            mConnectivityManager.unregisterNetworkCallback(mCallback);
            mConnectionStateRaw.postValue(State.UNKNOWN);
        }
    }

    private void updateLiveData() {
        var max = State.NOT_CONNECTED;
        for (var state : mNetworks.values()) {
            if (state.compareTo(max) > 0) {
                max = state;
            }
        }
        mConnectionStateRaw.postValue(max);
    }

    public LiveData<State> getConnectionState() {
        return mConnectionState;
    }

    private class NetworkCallback extends ConnectivityManager.NetworkCallback {
        @Override
        public void onAvailable(@NonNull Network network) {
            synchronized (mNetworks) {
                mNetworks.put(network, State.NOT_CONNECTED);
                updateLiveData();
            }
        }

        @Override
        public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities networkCapabilities) {
            var state = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
                    ? State.CONNECTED
                    : State.CONNECTED_METERED;
            synchronized (mNetworks) {
                mNetworks.put(network, state);
                updateLiveData();
            }
        }

        @Override
        public void onLost(@NonNull Network network) {
            synchronized (mNetworks) {
                mNetworks.remove(network);
                updateLiveData();
            }
        }
    }

    public enum State {
        /**
         * Either the {@link ConnectivityManager} could not be accessed, or the {@link ConnectionStateMonitor} is disabled.
         */
        UNKNOWN,
        /**
         * No network with capabilities {@link NetworkCapabilities#NET_CAPABILITY_INTERNET} and
         * {@link NetworkCapabilities#NET_CAPABILITY_VALIDATED} was found.
         */
        NOT_CONNECTED,
        /**
         * There is at least one network with capabilities {@link NetworkCapabilities#NET_CAPABILITY_INTERNET} and
         * {@link NetworkCapabilities#NET_CAPABILITY_VALIDATED} but none of them also has
         * {@link NetworkCapabilities#NET_CAPABILITY_NOT_METERED}.
         */
        CONNECTED_METERED,
        /**
         * There is at least one network with capabilities {@link NetworkCapabilities#NET_CAPABILITY_INTERNET},
         * {@link NetworkCapabilities#NET_CAPABILITY_VALIDATED} and
         * {@link NetworkCapabilities#NET_CAPABILITY_NOT_METERED}.
         */
        CONNECTED
    }
}