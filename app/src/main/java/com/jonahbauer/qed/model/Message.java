package com.jonahbauer.qed.model;

import android.graphics.Color;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.jonahbauer.qed.model.room.Converters;
import com.jonahbauer.qed.util.Preferences;

import org.jetbrains.annotations.Contract;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * An object representing a message in the qed chat
 */
@Data
@Entity
@EqualsAndHashCode(of = "id")
@TypeConverters(Converters.class)
public class Message implements Parcelable, Comparable<Message>, Serializable {
    private static final String LOG_TAG = Message.class.getName();
    private static final ThreadLocal<SimpleDateFormat> DATE_FORMAT = new ThreadLocal<>() {
        @NonNull
        @Override
        protected SimpleDateFormat initialValue() {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.GERMANY);
            sdf.setTimeZone(TimeZone.getTimeZone("CET"));
            return sdf;
        }
    };

    public static final Message PONG = new Message(0, "PONG", "PONG", new Date(0), 0, "PONG", "000000", 0, "PONG");

    @PrimaryKey
    private final long id;

    @NonNull
    @ColumnInfo(name = "name")
    private final String name;

    @NonNull
    @ColumnInfo(name = "message")
    private final String message;

    @NonNull
    @ColumnInfo(name = "date")
    private final Date date;

    @ColumnInfo(name = "user_id")
    private final long userId;

    @Nullable
    @ColumnInfo(name = "user_name")
    private final String userName;

    @NonNull
    @ColumnInfo(name = "color")
    private final String color;

    @NonNull
    @ColumnInfo(name = "channel")
    private final String channel;

    @ColumnInfo(name = "bottag")
    private final int bottag;

    @Ignore
    @ColorInt
    private final int transformedColor;

    @Ignore
    private final String bannerDate;

    public Message(long id, @NonNull String name, @NonNull String message, @NonNull Date date, long userId, @Nullable String userName, @NonNull String color, int bottag, @NonNull String channel) {
        this.name = name;
        this.message = message;
        this.date = date;
        this.userId = userId;
        this.userName = userName;
        this.color = color;
        this.id = id;
        this.bottag = bottag;
        this.channel = channel;

        this.transformedColor = transformColor(color);
        this.bannerDate = SimpleDateFormat.getDateInstance().format(date).intern();
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
            String name = json.getString("name").trim();
            String message = json.getString("message");
            String username = json.getString("username");
            String color = json.getString("color");
            String dateStr = json.getString("date");
            String channel = json.getString("channel");
            int userid = json.optInt("user_id", -1);
            int id = json.getInt("id");
            int bottag = json.getInt("bottag");

            if ("null".equals(username)) username = null;

            Date date;
            try {
                //noinspection ConstantConditions
                date = DATE_FORMAT.get().parse(dateStr);
                assert date != null;
            } catch (ParseException | NumberFormatException e) {
                Log.w(LOG_TAG, "Message did not contain a valid date: " + jsonMessage);
                return null;
            }

            return new Message(id, name, message, date, userid, username, color, bottag, channel);
        } catch (JSONException e) {
            if (!jsonMessage.contains("\"type\":\"ok\""))
                Log.e(LOG_TAG, jsonMessage + ": " + e.getMessage(), e);
        }
        return null;
    }

    @ColorInt
    private int transformColor(String color) {
        return transformColor(Color.parseColor("#" + color));
    }

    @ColorInt
    private int transformColor(int color) {
        if (Preferences.general().isNightMode()) {
            return color;
        } else {
            float[] hsv = new float[3];
            Color.colorToHSV(color, hsv);

            hsv[1] = 1;
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
        dest.writeSerializable(date);
        dest.writeLong(userId);
        dest.writeString(userName);
        dest.writeString(color);
        dest.writeString(channel);
        dest.writeInt(bottag);
        dest.writeLong(id);
    }

    public static final Parcelable.Creator<Message> CREATOR = new Parcelable.Creator<>() {

        @Override
        public Message createFromParcel(Parcel source) {
            String name = source.readString();
            String message = source.readString();
            Date date = (Date) source.readSerializable();
            long userId = source.readLong();
            String userName = source.readString();
            String color = source.readString();
            String channel = source.readString();
            int bottag = source.readInt();
            long id = source.readLong();

            assert date != null && name != null & message != null & color != null & channel != null;

            return new Message(id, name, message, date, userId, userName, color, bottag, channel);
        }

        @Override
        public Message[] newArray(int size) {
            return new Message[size];
        }
    };
}
