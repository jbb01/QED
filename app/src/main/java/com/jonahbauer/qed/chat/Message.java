package com.jonahbauer.qed.chat;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jonahbauer.qed.Application;

import org.jetbrains.annotations.Contract;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * An object representing a message in the qed chat
 */
public class Message {
    public static final Message PONG = new Message("PONG", "PONG", "PONG", 0, "PONG", "PONG", 0, 0, "PONG");

    public final String name;
    public final String message;
    public final String date;
    public final int userId;
    public final String userName;
    public final String color;
    public final String channel;
    public final int bottag;
    public final int id;

    public Message(String name, String message, String date, int userId, String userName, String color, int id, int bottag, String channel) {
        this.name = name;
        this.message = message;
        this.date = date;
        this.userId = userId;
        this.userName = userName;
        this.color = color;
        this.id = id;
        this.bottag = bottag;
        this.channel = channel;
    }

    @NonNull
    public String toString() {
        return "{"
                + "\"id\":\"" + id + "\", "
                + "\"name\":\"" + name + "\", "
                + "\"message\":\"" + message + "\", "
                + "\"date\":\"" + date + "\", "
                + "\"userId\":\"" + userId + "\", "
                + "\"userName\":\"" + userName + "\", "
                + "\"color\":\"" + color + "\", "
                + "\"bottag\":\"" + bottag + "\", "
                + "\"channel\":\"" + channel + "\"}";
    }


    public int compareTo(Message other) {
        return Integer.compare(id,other.id);
    }

    /**
     * Parses a string as obtained by the chat web socket to a message object
     * @param jsonMessage a json formatted message string
     * @return a message object representing the given message, null if an error occurred
     */
    @Contract("null -> null")
    public static Message interpretJSONMessage(@Nullable String jsonMessage) {
        if (jsonMessage == null) return null;
        try {
            JSONObject json = new JSONObject(jsonMessage);
            String name = json.getString("name");
            String message = json.getString("message");
            String username = json.getString("username");
            String color = json.getString("color");
            String date = json.getString("date");
            String channel = json.getString("channel");
            int userid = json.optInt("user_id", -1);
            int id = json.getInt("id");
            int bottag = json.getInt("bottag");
            name = name.trim();
            if ("null".equals(username)) username = null;
            return new Message(name, message, date, userid, username, color, id, bottag, channel);
        } catch (JSONException e) {
            if (!jsonMessage.contains("\"type\":\"ok\""))
                Log.e(Application.LOG_TAG_ERROR, jsonMessage + ": " + e.getMessage(), e);
        }
        return null;
    }

    @Override
    public int hashCode() {
        return id;
    }
}
