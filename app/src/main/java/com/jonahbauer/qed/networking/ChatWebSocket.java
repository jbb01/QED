package com.jonahbauer.qed.networking;

import android.util.Log;

import androidx.annotation.NonNull;

import com.jonahbauer.qed.BuildConfig;
import com.jonahbauer.qed.model.Message;
import com.jonahbauer.qed.networking.cookies.QEDCookieHandler;
import com.jonahbauer.qed.networking.exceptions.InvalidCredentialsException;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.core.ObservableEmitter;
import io.reactivex.rxjava3.core.ObservableOnSubscribe;
import io.reactivex.rxjava3.disposables.Disposable;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class ChatWebSocket extends WebSocketListener implements ObservableOnSubscribe<Message> {
    private static final String LOG_TAG = ChatWebSocket.class.getName();

    private static final String PING = "{\"type\":\"ping\"}";
    private static final String PONG = "{\"type\":\"pong\"}";

    private final String mChannel;

    private ObservableEmitter<Message> mEmitter;

    private WebSocket mWebSocket;

    private long mPosition = -100;
    private boolean mSending;

    public ChatWebSocket(String channel) {
        this.mChannel = channel;
        this.mSending = true;
    }

    @Override
    public void subscribe(@NonNull ObservableEmitter<Message> emitter) {
        mEmitter = emitter;
        mEmitter.setDisposable(Disposable.fromRunnable(this::close));

        connect();
    }

    private void connect() {
        mPosition = - 100;

        OkHttpClient client = new OkHttpClient.Builder()
                .pingInterval(30, TimeUnit.SECONDS)
                .connectTimeout(5, TimeUnit.SECONDS).build();

        // configure request
        Request.Builder request = new Request.Builder()
                .url(NetworkConstants.CHAT_WEBSOCKET
                        + "?channel=" + mChannel
                        + "&version=" + "2"
                        + "&position=" + mPosition
                )
                .addHeader("Origin", "https://chat.qed-verein.de");

        // add cookies
        try {
            URI uri = new URI("https", "chat.qed-verein.de", "/websocket", null);
            QEDCookieHandler.getInstance().get(uri, Collections.emptyMap())
                            .forEach((key, values) -> values.forEach(value -> request.addHeader(key, value)));
        } catch (URISyntaxException | IOException ignored) {}

        // start socket
        mWebSocket = client.newWebSocket(request.build(), this);
        mWebSocket.send(PING);
    }

    private void close() {
        if (mWebSocket != null) {
            mWebSocket.close(1001, null);
        }
    }

    @Override
    public void onOpen(@NonNull WebSocket webSocket, @NonNull Response response) {
        if (BuildConfig.DEBUG) Log.d(LOG_TAG, "WebSocket opened.");
    }

    @Override
    public void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
        if (BuildConfig.DEBUG) Log.d(LOG_TAG, text);

        Message message = Message.parseJsonMessage(text);
        if (message == null) return;

        switch (message.getType()) {
            case PING:
                webSocket.send(PONG);
                break;
            case ACK:
                mSending = false;
                break;
            case PONG:
                mSending = false;
                mEmitter.onNext(message);
                break;
            case POST:
                if (message.getId() < mPosition) break;
                mPosition = message.getId() + 1;
                mEmitter.onNext(message);
                break;
        }
    }

    @Override
    public void onClosing(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
        webSocket.close(code, reason);
    }

    @Override
    public void onClosed(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
        if (BuildConfig.DEBUG) Log.d(LOG_TAG, "WebSocket closed.");

        if (code == 4000 && reason.contains("Ungültige Anmeldedaten")) {
            mEmitter.onError(new InvalidCredentialsException());
        } else {
            mEmitter.onComplete();
        }
    }

    @Override
    public void onFailure(@NonNull WebSocket webSocket, @NonNull Throwable t, Response response) {
        if (BuildConfig.DEBUG) Log.d(LOG_TAG, "WebSocket failure.", t);

        mEmitter.onError(t);
    }

    public boolean send(String name, String message, boolean publicId) {
        if (!mSending) try {
            mSending = true;

            JSONObject json = new JSONObject();
            json.put("channel", mChannel);
            json.put("name", name);
            json.put("message", message);
            json.put("delay", Long.toString(mPosition));
            json.put("publicid", publicId ? 1 : 0);
            return mWebSocket.send(json.toString());
        } catch (JSONException e) {
            mSending = false;
            Log.e(LOG_TAG, "Unable to create JSON message.", e);
            return false;
        }
        return false;
    }
}
