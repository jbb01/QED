package com.jonahbauer.qed.chat;

import android.util.Log;

import androidx.annotation.NonNull;

import com.jonahbauer.qed.Application;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;

@SuppressWarnings("unused")
public class Message {
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

    public static Message interpretJSONMessage(String jsonMessage) {
        return interpretJSONMessage(jsonMessage, true);
    }

    public static Message interpretJSONMessage(String jsonMessage, boolean format) {
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
            if (format) {
                name = new String(name.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
                message = new String(message.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
                username = new String(username.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
                color = new String(color.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
                date = new String(date.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
                channel = new String(channel.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
            }
            name = name.trim();
            if ("null".equals(username)) username = null;
            return new Message(name, message, date, userid, username, color, id, bottag, channel);
        } catch (JSONException e) {
            Log.e(Application.LOG_TAG_ERROR, jsonMessage + ": " + e.getMessage(), e);
        }
        return null;
    }

    @Override
    public int hashCode() {
        return id;
    }
}
