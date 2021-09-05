package com.jonahbauer.qed.model.room;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import androidx.room.TypeConverter;

import com.jonahbauer.qed.model.Person;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Converters {
    @TypeConverter
    public static Date dateFromTimestamp(Long value) {
        return value == null ? null : new Date(value);
    }

    @TypeConverter
    public static Long dateToTimestamp(Date date) {
        return date == null ? null : date.getTime();
    }

    @TypeConverter
    public static Map<String, String> mapFromString(String str) {
        try {
            JSONObject json = new JSONObject(str);
            HashMap<String, String> out = new HashMap<>();

            for (Iterator<String> keyIt = json.keys(); keyIt.hasNext();) {
                String key = keyIt.next();

                out.put(key, json.getString(key));
            }

            return out;
        } catch (JSONException e) {
            return null;
        }
    }

    @TypeConverter
    public static String mapToString(Map<String, String> map) {
        JSONObject jsonObject = new JSONObject();
        map.forEach((key, value) -> {
            try {
                jsonObject.put(key, value);
            } catch (JSONException ignored) {}
        });
        return jsonObject.toString();
    }

    @TypeConverter
    public static Bitmap bitmapFromBlob(byte[] blob) {
        if (blob == null) return null;

        return BitmapFactory.decodeByteArray(blob, 0, blob.length);
    }

    @TypeConverter
    public static byte[] bitmapToBlob(Bitmap bitmap) {
        if (bitmap == null) return null;

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        return stream.toByteArray();
    }

    @TypeConverter
    public static List<String> stringListFromString(String str) {
        try {
            JSONArray json = new JSONArray(str);
            List<String> out = new ArrayList<>(json.length());

            for (int i = 0, length = json.length(); i < length; i++) {
                out.add(json.getString(i));
            }

            return out;
        } catch (JSONException e) {
            return null;
        }
    }

    @TypeConverter
    public static String stringListToString(List<String> list) {
        JSONArray jsonArray = new JSONArray();
        list.forEach(jsonArray::put);
        return jsonArray.toString();
    }

    @TypeConverter
    public static List<Person> personListFromString(String str) {
        try {
            JSONArray json = new JSONArray(str);
            List<Person> out = new ArrayList<>(json.length());

            for (int i = 0, length = json.length(); i < length; i += 2) {
                Person person = new Person(json.getLong(i));
                person.setFirstName(json.getString(i + 1));
                out.add(person);
            }

            return out;
        } catch (JSONException e) {
            return null;
        }
    }

    @TypeConverter
    public static String personListToString(List<Person> list) {
        JSONArray jsonArray = new JSONArray();
        list.forEach(person -> {
            jsonArray.put(person.getId());
            jsonArray.put(person.getFirstName());
        });
        return jsonArray.toString();
    }

    @TypeConverter
    public static List<Date> dateListFromString(String str) {
        try {
            JSONArray json = new JSONArray(str);
            List<Date> out = new ArrayList<>(json.length());

            for (int i = 0, length = json.length(); i < length; i++) {
                out.add(new Date(json.getLong(i)));
            }

            return out;
        } catch (JSONException e) {
            return null;
        }
    }

    @TypeConverter
    public static String dateListToString(List<Date> list) {
        JSONArray jsonArray = new JSONArray();
        list.forEach(date -> jsonArray.put(date.getTime()));
        return jsonArray.toString();
    }
}
