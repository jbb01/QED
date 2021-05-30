package com.jonahbauer.qed.model;

import android.graphics.Color;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jonahbauer.qed.Application;
import com.jonahbauer.qed.util.Preferences;

import org.jetbrains.annotations.Contract;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * An object representing a message in the qed chat
 */
@Data
@EqualsAndHashCode(of = "id")
public class Message implements Parcelable, Comparable<Message>, Serializable {
    private static final SimpleDateFormat sdfIn = new SimpleDateFormat("yyyy-MM-dd", Locale.GERMANY);
    private static final SimpleDateFormat sdfOut = new SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY);

    public static final Message PONG = new Message("PONG", "PONG", "PONG", 0, "PONG", "000000", 0, 0, "PONG");

    @NonNull private final String name;
    @NonNull private final String message;
    @NonNull private final String date;
    private final long userId;
    @Nullable private final String userName;
    @NonNull private final String color;
    @NonNull private final String channel;
    private final int bottag;
    private final long id;

    private final String dateNoTime;
    private final int transformedColor;

    public Message(@NonNull String name, @NonNull String message, @NonNull String date, long userId, @Nullable String userName, @NonNull String color, long id, int bottag, @NonNull String channel) {
        this.name = name;
        this.message = message;
        this.date = date;
        this.userId = userId;
        this.userName = userName;
        this.color = color;
        this.id = id;
        this.bottag = bottag;
        this.channel = channel;

        Date dateNoTimeObj = null;
        try {
            dateNoTimeObj = sdfIn.parse(date.split(" ")[0]);
        } catch (ParseException ignored) {}

        if (dateNoTimeObj != null) {
            this.dateNoTime = sdfOut.format(dateNoTimeObj).intern();
        } else {
            this.dateNoTime = date.split(" ")[0].intern();
        }

        transformedColor = transformColor(color);
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
        return Long.compare(id,other.id);
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

    private int transformColor(String color) {
        return transformColor(Color.parseColor("#" + color));
    }

    private int transformColor(int color) {
        if (Preferences.general().isNightMode()) {
            return color;
        } else {
            float[] hsv = new float[3];
            Color.colorToHSV(color, hsv);

            hsv[1] = 1/*-(1-hsv[1])*0.2f*/;
            hsv[2] = 0.85f;

            return Color.HSVToColor(hsv);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(message);
        dest.writeString(date);
        dest.writeLong(userId);
        dest.writeString(userName);
        dest.writeString(color);
        dest.writeString(channel);
        dest.writeInt(bottag);
        dest.writeLong(id);
    }

    public static final Parcelable.Creator<Message> CREATOR = new Parcelable.Creator<Message>() {

        @Override
        public Message createFromParcel(Parcel source) {
            String name = source.readString();
            String message = source.readString();
            String date = source.readString();
            long userId = source.readLong();
            String userName = source.readString();
            String color = source.readString();
            String channel = source.readString();
            int bottag = source.readInt();
            long id = source.readLong();

            assert name != null & message != null & date != null & color != null & channel != null;

            return new Message(name, message, date, userId, userName, color, id, bottag, channel);
        }

        @Override
        public Message[] newArray(int size) {
            return new Message[size];
        }
    };
}
