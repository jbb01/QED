package com.jonahbauer.qed.networking;

import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import com.jonahbauer.qed.Application;
import com.jonahbauer.qed.BuildConfig;
import com.jonahbauer.qed.Pref;
import com.jonahbauer.qed.R;
import com.jonahbauer.qed.chat.Message;
import com.jonahbauer.qed.networking.login.InvalidCredentialsException;
import com.jonahbauer.qed.networking.login.QEDLogin;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.SoftReference;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class ChatWebSocket extends WebSocketListener {
    private final Application mApplication;
    private final SharedPreferences mSharedPreferences;
    private final ChatWebSocketListener mListener;

    private WebSocket mWebSocket;

    private int mPosition = -100;
    private boolean mSending;

    public ChatWebSocket(@NonNull ChatWebSocketListener listener) {
        this.mListener = listener;

        SoftReference<Application> applicationReference = Application.getApplicationReference();
        mApplication = applicationReference.get();

        if (mApplication == null) {
            AssertionError error = new AssertionError("Application is null.");

            listener.onError(null, error);
            mSharedPreferences = null;

            throw error;
        }

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mApplication);

        mSending = true;
    }

    public void openSocket() {
        mPosition = -100;

        if (mWebSocket != null)
            mWebSocket.close(1001,null);

        OkHttpClient client = new OkHttpClient.Builder()
                .pingInterval(30, TimeUnit.SECONDS)
                .connectTimeout(5, TimeUnit.SECONDS).build();

        Request.Builder builder = new Request.Builder().url(
                mApplication.getString(R.string.chat_websocket)
                        + "?channel=" + mSharedPreferences.getString(Pref.Chat.CHANNEL,"")
                        + "&version=" + "2"
                        + "&position=" + mPosition);
        builder.addHeader("Origin", "https://chat.qed-verein.de");

        try {
            QEDLogin.ensureDataAvailable(Feature.CHAT);

            HashMap<String,String> cookies = new HashMap<>();
            QEDLogin.addCookies(Feature.CHAT, cookies);

            builder.addHeader("Cookie", QEDLogin.loadCookies(Feature.CHAT, cookies));
            Request request = builder.build();
            mWebSocket = client.newWebSocket(request, this);
            mWebSocket.send("{\"type\":\"ping\"}");
        } catch (InvalidCredentialsException e) {
            Log.e(Application.LOG_TAG_ERROR, e.getMessage(), e);
            if (mListener != null) mListener.onError(ChatWebSocketListener.REASON_INVALID_CREDENTIALS, e);
        } catch (NoNetworkException e) {
            Log.e(Application.LOG_TAG_ERROR, e.getMessage(), e);
            if (mListener != null) mListener.onError(ChatWebSocketListener.REASON_NETWORK, e);
        }
    }

    public void closeSocket() {
        if (mWebSocket != null) mWebSocket.close(1001, "");
    }

    public boolean send(@NonNull String message) {
        if (!mSending) try {
            mSending = true;

            JSONObject json = new JSONObject();
            json.put("channel", mSharedPreferences.getString(Pref.Chat.CHANNEL, ""));
            json.put("name", mSharedPreferences.getString(Pref.Chat.NAME, ""));
            json.put("message", message);
            json.put("delay", Long.toString(mPosition));
            json.put("publicid", mSharedPreferences.getBoolean(Pref.Chat.PUBLIC_ID, false) ? 1 : 0);
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
                QEDLogin.login(Feature.CHAT);
                openSocket();
            } catch (InvalidCredentialsException e) {
                Log.e(Application.LOG_TAG_ERROR, e.getMessage(), e);
                if (mListener != null) mListener.onError(ChatWebSocketListener.REASON_INVALID_CREDENTIALS, e);
            } catch (NoNetworkException e) {
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
