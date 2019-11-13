package com.jonahbauer.qed.networking;

import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import com.jonahbauer.qed.Application;
import com.jonahbauer.qed.BuildConfig;
import com.jonahbauer.qed.R;
import com.jonahbauer.qed.activities.LoginActivity;
import com.jonahbauer.qed.chat.Message;
import com.jonahbauer.qed.networking.login.InvalidCredentialsException;
import com.jonahbauer.qed.networking.login.QEDLogin;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class ChatWebSocket extends WebSocketListener {

    private final Application application;
    private final SharedPreferences sharedPreferences;
    private final ChatWebSocketListener listener;

    private WebSocket webSocket;

    private int position = -100;
    private boolean forcedLogin = false;
    private boolean sending;

    public ChatWebSocket(ChatWebSocketListener listener) {
        this.listener = listener;
        application = Application.getContext();
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(application);

        sending = true;
    }

    public void openSocket() {
        position = -100;

        if (webSocket != null)
            webSocket.close(1001,null);

        OkHttpClient client = new OkHttpClient.Builder()
                .pingInterval(30, TimeUnit.SECONDS)
                .connectTimeout(5, TimeUnit.SECONDS).build();

        Request.Builder builder = new Request.Builder().url(
                application.getString(R.string.chat_websocket)
                        + "?channel=" + sharedPreferences.getString(application.getString(R.string.preferences_chat_channel_key),"")
                        + "&version=" + "2"
                        + "&position=" + position);
        builder.addHeader("Origin", "https://chat.qed-verein.de");

        String userId = application.loadData(Application.KEY_USERID, false);
        String pwHash = application.loadData(Application.KEY_CHAT_PWHASH, true);

        if (userId == null || userId.equals("") || pwHash == null || pwHash.equals("")) {
            if (!login()) {
                return;
            } else {
                userId = application.loadData(Application.KEY_USERID, false);
                pwHash = application.loadData(Application.KEY_CHAT_PWHASH, true);
            }
        }

        if (userId == null || userId.equals("") || pwHash == null || pwHash.equals("")) {
            throw new AssertionError();
        }

        builder.addHeader("Cookie", "userid=" + userId + ", pwhash=" + pwHash);
        Request request = builder.build();
        webSocket = client.newWebSocket(request, this);
        webSocket.send("{\"type\":\"ping\"}");
    }

    public void closeSocket() {
        if (webSocket != null) webSocket.close(1001, "");
    }

    public boolean send(@NonNull String message) {
        if (!sending) try {
            sending = true;

            JSONObject json = new JSONObject();
            json.put("channel", sharedPreferences.getString(application.getString(R.string.preferences_chat_channel_key), ""));
            json.put("name", sharedPreferences.getString(application.getString(R.string.preferences_chat_name_key), ""));
            json.put("message", message);
            json.put("delay", Long.toString(position));
            json.put("publicid", sharedPreferences.getBoolean(application.getString(R.string.preferences_chat_publicId_key), false) ? 1 : 0);
            return webSocket.send(json.toString());
        } catch (JSONException e) {
            sending = false;
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
                    if (listener != null) listener.onMessage(Message.PONG);
                case "ack":
                    sending = false;
                    break;
                case "post":
                    int id = json.getInt("id");
                    if (id < position) break;
                    position = id + 1;
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

                    if (listener != null) listener.onMessage(message);
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
            if (login()) {
                openSocket();
            }
        }
    }

    @Override
    public void onFailure(@NonNull WebSocket webSocket, @NonNull Throwable t, Response response) {
        try {
            sending = false;
            throw (Exception) t;
        } catch (UnknownHostException | SSLException e) {
            Log.e(Application.LOG_TAG_DEBUG, "Chat WebSocket " + webSocket.toString() + " no network!", e);
            if (listener != null) listener.onNetworkError();
        } catch (Exception e) {
            Log.e(Application.LOG_TAG_DEBUG, "Chat WebSocket failed! " + e.getClass().toString(), e);
            if (listener != null) listener.onUnknownError();
        }
    }

    private boolean login() {
        try {
            QEDLogin.loginChat();
        } catch (NoNetworkException e) {
            Log.e(Application.LOG_TAG_ERROR, e.getMessage(), e);
            if (listener != null) listener.onNetworkError();
            return false;
        } catch (InvalidCredentialsException e) {
            Log.e(Application.LOG_TAG_ERROR, e.getMessage(), e);
            forceLogin();
            return false;
        }
        return true;
    }

    private void forceLogin() {
        if (!forcedLogin) {
            forcedLogin = true;
            Intent intent = new Intent(application, LoginActivity.class);
            intent.putExtra(LoginActivity.DONT_START_MAIN, true);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            application.startActivity(intent);
        }
        if (listener != null) listener.onUnknownError();
    }
}
