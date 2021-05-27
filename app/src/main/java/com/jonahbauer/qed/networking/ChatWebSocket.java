package com.jonahbauer.qed.networking;

import android.util.Log;

import androidx.annotation.NonNull;

import com.jonahbauer.qed.Application;
import com.jonahbauer.qed.BuildConfig;
import com.jonahbauer.qed.model.Message;
import com.jonahbauer.qed.networking.cookies.QEDCookieHandler;
import com.jonahbauer.qed.networking.exceptions.InvalidCredentialsException;
import com.jonahbauer.qed.networking.exceptions.NetworkException;
import com.jonahbauer.qed.networking.login.QEDLogin;
import com.jonahbauer.qed.util.Preferences;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class ChatWebSocket extends WebSocketListener {
    private final ChatWebSocketListener mListener;

    private WebSocket mWebSocket;

    private int mPosition = -100;
    private boolean mSending;

    private int mRetryCount;

    public ChatWebSocket(@NonNull ChatWebSocketListener listener) {
        this.mListener = listener;
        this.mSending = true;
    }

    public void openSocket() {
        mPosition = -100;

        if (mWebSocket != null)
            mWebSocket.close(1001,null);

        OkHttpClient client = new OkHttpClient.Builder()
                .pingInterval(30, TimeUnit.SECONDS)
                .connectTimeout(5, TimeUnit.SECONDS).build();

        Request.Builder builder = new Request.Builder().url(
                NetworkConstants.CHAT_WEBSOCKET
                        + "?channel=" + Preferences.chat().getChannel()
                        + "&version=" + "2"
                        + "&position=" + mPosition);
        builder.addHeader("Origin", "https://chat.qed-verein.de");

        // add cookies
        try {
            URI uri = new URI("https", "chat.qed-verein.de", "/websocket", null);
            QEDCookieHandler.getInstance().get(uri, Collections.emptyMap())
                            .forEach((key, values) -> values.forEach(value -> builder.addHeader(key, value)));
        } catch (URISyntaxException | IOException ignored) {}

        Request request = builder.build();
        mWebSocket = client.newWebSocket(request, this);
        mWebSocket.send("{\"type\":\"ping\"}");
    }

    public void closeSocket() {
        if (mWebSocket != null) mWebSocket.close(1001, "");
    }

    public boolean send(@NonNull String message) {
        if (!mSending) try {
            mSending = true;

            JSONObject json = new JSONObject();
            json.put("channel", Preferences.chat().getChannel());
            json.put("name", Preferences.chat().getName());
            json.put("message", message);
            json.put("delay", Long.toString(mPosition));
            json.put("publicid", Preferences.chat().isPublicId() ? 1 : 0);
            return mWebSocket.send(json.toString());
        } catch (JSONException e) {
            mSending = false;
            Log.e(Application.LOG_TAG_ERROR, "Chat WebSocket: Unable to create JSON message!", e);
            return false;
        }
        return false;
    }

    @Override
    public void onOpen(@NonNull WebSocket webSocket, @NonNull Response response) {}

    @Override
    public void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
        if (BuildConfig.DEBUG) Log.d(Application.LOG_TAG_DEBUG, text);
        try {
            JSONObject json = new JSONObject(text);
            switch (json.getString("type")) {
                case "ping":
                    webSocket.send("{\"type\":\"pong\"}");
                    break;
                case "pong":
                    if (mListener != null) mListener.onMessage(Message.PONG);
                case "ack":
                    mSending = false;
                    break;
                case "post":
                    int id = json.getInt("id");
                    if (id < mPosition) break;
                    mPosition = id + 1;
                    String name = json.getString("name");
                    String messageStr = json.getString("message");
                    String username = json.getString("username");
                    String color = json.getString("color");
                    String date = json.getString("date");
                    String channel = json.getString("channel");
                    int userid = json.optInt("user_id", -1);
                    int bot = json.getInt("bottag");
                    name = name.trim();
                    if ("null".equals(username)) username = null;

                    Message message = new Message(name, messageStr, date, userid, username, color, id, bot, channel);

                    if (mListener != null) mListener.onMessage(message);
                    break;
            }
        } catch (JSONException e) {
            Log.e(Application.LOG_TAG_ERROR, "Chat WebSocket: Unable to parse message!", e);
        }
    }

    @Override
    public void onClosing(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
        webSocket.close(code, reason);
    }

    @Override
    public void onClosed(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
        if (code == 4000 && reason.contains("Ungültige Anmeldedaten")) {
            try {
                if (mRetryCount++ < 2) {
                    QEDLogin.login(Feature.CHAT);
                    openSocket();
                } else {
                    throw new InvalidCredentialsException();
                }
            } catch (InvalidCredentialsException e) {
                Log.e(Application.LOG_TAG_ERROR, e.getMessage(), e);
                if (mListener != null) mListener.onError(ChatWebSocketListener.REASON_INVALID_CREDENTIALS, e);
            } catch (NetworkException e) {
                Log.e(Application.LOG_TAG_ERROR, e.getMessage(), e);
                if (mListener != null) mListener.onError(ChatWebSocketListener.REASON_NETWORK, e);
            }
        }
    }

    @Override
    public void onFailure(@NonNull WebSocket webSocket, @NonNull Throwable t, Response response) {
        try {
            mSending = false;
            throw (Exception) t;
        } catch (UnknownHostException | SSLException e) {
            Log.e(Application.LOG_TAG_DEBUG, "Chat WebSocket " + webSocket.toString() + " no network!", e);
            if (mListener != null) mListener.onError(ChatWebSocketListener.REASON_NETWORK, t);
        } catch (Exception e) {
            Log.e(Application.LOG_TAG_DEBUG, "Chat WebSocket failed! " + e.getClass().toString(), e);
            if (mListener != null) mListener.onError(null, t);
        }
    }
}
