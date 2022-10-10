package com.jonahbauer.qed.model.viewmodel;

import android.content.SharedPreferences;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.jonahbauer.qed.Application;
import com.jonahbauer.qed.ConnectionStateMonitor;
import com.jonahbauer.qed.R;
import com.jonahbauer.qed.model.Message;
import com.jonahbauer.qed.model.room.Database;
import com.jonahbauer.qed.networking.ChatWebSocket;
import com.jonahbauer.qed.networking.Feature;
import com.jonahbauer.qed.networking.Reason;
import com.jonahbauer.qed.networking.exceptions.InvalidCredentialsException;
import com.jonahbauer.qed.networking.login.QEDLogin;
import com.jonahbauer.qed.util.MessageUtils;
import com.jonahbauer.qed.util.Preferences;

import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.PublishSubject;

import static com.jonahbauer.qed.ConnectionStateMonitor.State.*;

public class ChatViewModel extends AndroidViewModel {

    private final MutableLiveData<String> mChannel = new MutableLiveData<>();
    private final MutableLiveData<String> mName = new MutableLiveData<>();
    private final MutableLiveData<Boolean> mReady = new MutableLiveData<>(false);
    private final PublishSubject<Message> mMessageRX = PublishSubject.create();
    private final MutableLiveData<Observable<Message>> mReplayMessageRX = new MutableLiveData<>();

    private ChatWebSocket mWebSocket;
    private final WebSocketObserver mWebSocketObserver = new WebSocketObserver();
    private int mRetryCount = 0;
    private final CompositeDisposable mDisposable = new CompositeDisposable();
    private final Disposable mDatabaseDisposable;

    private final SharedPreferenceListener mPreferenceListener = new SharedPreferenceListener();

    private final LiveData<ConnectionStateMonitor.State> mConnectionState;
    private final ConnectionStateObserver mConnectionStateObserver = new ConnectionStateObserver();

    public ChatViewModel(@NonNull android.app.Application application) {
        super(application);

        // insert incoming message into the database
        var messageDao = Database.getInstance(application).messageDao();
        var debounced = mMessageRX.debounce(1, TimeUnit.SECONDS);
        mDatabaseDisposable = mMessageRX.subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .filter(message -> message.getType() == Message.Type.POST)
                .buffer(debounced)
                .subscribe(messageDao::insertSync);

        Preferences.getSharedPreferences().registerOnSharedPreferenceChangeListener(mPreferenceListener);

        var app = (Application) getApplication();
        mConnectionState = app.getConnectionStateMonitor().getConnectionState();
        mConnectionState.observeForever(mConnectionStateObserver);

        mName.setValue(Preferences.getChat().getName());
    }

    @MainThread
    public void connect() {
        connect(Preferences.getChat().getChannel());
    }

    @MainThread
    private synchronized void connect(String channel) {
        disconnect();

        mChannel.setValue(channel);
        mRetryCount = 0;

        mReady.setValue(true);

        mWebSocket = new ChatWebSocket(channel);
        mWebSocket.subscribeOn(Schedulers.io())
                .observeOn(Schedulers.computation())
                .map(MessageUtils.dateFixer()::apply)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mWebSocketObserver);
        mDisposable.add(mWebSocket.connect());

        var replay = mMessageRX.replay();
        mDisposable.add(replay.connect());
        mReplayMessageRX.setValue(replay);
    }

    @MainThread
    public void disconnect() {
        publishError(R.string.chat_websocket_closed);
        mDisposable.clear();
        mReplayMessageRX.setValue(null);
    }

    public boolean send(String message) {
        return send(Preferences.getChat().getName(), message, Preferences.getChat().isPublicId());
    }

    public boolean send(String name, String message, boolean publicId) {
        return mWebSocket.send(name, message, publicId);
    }

    public boolean isOpen() {
        return mWebSocket != null && mWebSocket.isOpen();
    }

    public LiveData<Observable<Message>> getMessageRX() {
        return mReplayMessageRX;
    }

    public LiveData<String> getChannel() {
        return mChannel;
    }

    public LiveData<String> getName() {
        return mName;
    }

    /**
     * Whether the chat is ready for messages to be send, that is whether the connection has not
     * been closed and no error occurred.
     */
    public LiveData<Boolean> getReady() {
        return mReady;
    }

    private void publishError(@StringRes int message) {
        mReady.setValue(false);
        mMessageRX.onNext(Message.newErrorMessage(getApplication().getString(message)));
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        mDisposable.clear();
        mDatabaseDisposable.dispose();
        Preferences.getSharedPreferences().unregisterOnSharedPreferenceChangeListener(mPreferenceListener);
        mConnectionState.removeObserver(mConnectionStateObserver);
    }

    private class WebSocketObserver implements Observer<Message> {
        @Override
        public void onSubscribe(@NonNull Disposable d) {
            mDisposable.add(d);
        }

        @Override
        public void onNext(@NonNull Message message) {
            mMessageRX.onNext(message);
        }

        @Override
        public void onError(@NonNull Throwable e) {
            publishError(Reason.guess(e).getStringRes());
            if (e instanceof InvalidCredentialsException) {
                if (mRetryCount++ == 0) {
                    var channel = mChannel.getValue();
                    mDisposable.add(QEDLogin.loginAsync(Feature.CHAT, success -> {
                        if (success) connect(channel);
                    }));
                }
            }
        }

        @Override
        public void onComplete() {
            publishError(R.string.chat_websocket_closed);
        }
    }

    private class SharedPreferenceListener implements SharedPreferences.OnSharedPreferenceChangeListener {

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (Preferences.getChat().getKeys().getChannel().equals(key)) {
                connect(Preferences.getChat().getChannel());
            } else if (Preferences.getChat().getKeys().getName().equals(key)) {
                mName.setValue(Preferences.getChat().getName());
            }
        }
    }
    private class ConnectionStateObserver implements androidx.lifecycle.Observer<ConnectionStateMonitor.State> {
        @Override
        public void onChanged(ConnectionStateMonitor.State state) {
            if (state == NOT_CONNECTED) {
                publishError(R.string.error_network);
            } else if (state == CONNECTED || state == CONNECTED_METERED) {
                connect();
            }
        }
    }
}
