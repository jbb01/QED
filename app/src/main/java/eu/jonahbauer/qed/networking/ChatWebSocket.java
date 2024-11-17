package eu.jonahbauer.qed.networking;

import android.util.Log;

import androidx.annotation.NonNull;

import eu.jonahbauer.qed.BuildConfig;
import eu.jonahbauer.qed.model.Message;
import eu.jonahbauer.qed.network.util.NetworkConstants;
import eu.jonahbauer.qed.networking.cookies.QEDCookieHandler;
import eu.jonahbauer.qed.networking.exceptions.InvalidCredentialsException;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.exceptions.Exceptions;
import io.reactivex.rxjava3.functions.Consumer;
import io.reactivex.rxjava3.internal.util.ExceptionHelper;
import io.reactivex.rxjava3.observables.ConnectableObservable;
import io.reactivex.rxjava3.subjects.PublishSubject;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class ChatWebSocket extends ConnectableObservable<Message> {
    private static final String LOG_TAG = ChatWebSocket.class.getName();

    private static final String PING = "{\"type\":\"ping\"}";
    private static final String PONG = "{\"type\":\"pong\"}";

    private final String mChannel;

    private WebSocket mWebSocket;
    private final Listener mWebSocketListener = new Listener();

    private volatile long mPosition = -100;
    private final AtomicBoolean mSending = new AtomicBoolean(true);
    private final AtomicBoolean mConnected = new AtomicBoolean(false);
    private final AtomicBoolean mOpen = new AtomicBoolean(false);

    private PublishSubject<Message> mPublishSubject = PublishSubject.create();

    public ChatWebSocket(String channel) {
        this.mChannel = channel;
    }

    @Override
    protected void subscribeActual(@NonNull Observer<? super Message> observer) {
        mPublishSubject.subscribe(observer);
    }

    @Override
    public void connect(@NonNull Consumer<? super Disposable> connection) {
        if (!mConnected.compareAndSet(false, true)) {
            throw new IllegalStateException();
        }

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
        mWebSocket = client.newWebSocket(request.build(), mWebSocketListener);
        mWebSocket.send(PING);

        try {
            connection.accept(Disposable.fromAction(this::close));
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            throw ExceptionHelper.wrapOrThrow(ex);
        }
    }

    @Override
    public void reset() {
        if (mPublishSubject.hasComplete() || mPublishSubject.hasThrowable()) {
            mPosition = -100;
            mWebSocket = null;
            mConnected.set(false);
            mOpen.set(false);
            mPublishSubject = PublishSubject.create();
        }
    }

    public void close() {
        if (mWebSocket != null) {
            mWebSocket.close(1001, null);
        } else {
            mPublishSubject.onComplete();
        }
    }

    public boolean isOpen() {
        return mOpen.get();
    }

    public boolean send(String name, String message, boolean publicId) {
        if (mSending.compareAndSet(false, true)) try {
            JSONObject json = new JSONObject();
            json.put("channel", mChannel);
            json.put("name", name);
            json.put("message", message);
            json.put("delay", Long.toString(mPosition));
            json.put("publicid", publicId ? 1 : 0);
            return mWebSocket.send(json.toString());
        } catch (JSONException e) {
            mSending.set(false);
            Log.e(LOG_TAG, "Unable to create JSON message.", e);
            return false;
        }
        return false;
    }

    private class Listener extends WebSocketListener {

        @Override
        public void onOpen(@NonNull WebSocket webSocket, @NonNull Response response) {
            if (BuildConfig.DEBUG) Log.d(LOG_TAG, "WebSocket opened.");
            mOpen.set(true);
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
                    mSending.set(false);
                    break;
                case PONG:
                    mSending.set(false);
                    mPublishSubject.onNext(message);
                    break;
                case POST:
                    if (message.getId() < mPosition) break;
                    mPosition = message.getId() + 1;
                    mPublishSubject.onNext(message);
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
                mPublishSubject.onError(new InvalidCredentialsException());
            } else {
                mPublishSubject.onComplete();
            }

            mOpen.set(false);
        }

        @Override
        public void onFailure(@NonNull WebSocket webSocket, @NonNull Throwable t, Response response) {
            if (BuildConfig.DEBUG) Log.d(LOG_TAG, "WebSocket failure.", t);

            mPublishSubject.onError(t);
            mOpen.set(false);
        }
    }
}
