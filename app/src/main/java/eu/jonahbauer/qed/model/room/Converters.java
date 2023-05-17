package eu.jonahbauer.qed.model.room;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import androidx.room.TypeConverter;

import eu.jonahbauer.qed.model.Person;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import lombok.experimental.UtilityClass;

@UtilityClass
@SuppressWarnings("unused")
public class Converters {
    //<editor-fold desc="Time" defaultstate="collapse">
    @TypeConverter
    public static Instant instantFromLong(Long value) {
        return value == null ? null : Instant.ofEpochSecond(value);
    }

    @TypeConverter
    public static Long instantToLong(Instant instant) {
        return instant == null ? null : instant.getEpochSecond();
    }

    @TypeConverter
    public static LocalDate localDateFromLong(Long value) {
        return value == null ? null : LocalDate.ofEpochDay(value);
    }

    @TypeConverter
    public static Long localDateToLong(LocalDate date) {
        return date == null ? null : date.toEpochDay();
    }

    @TypeConverter
    public static List<LocalDate> localDateListFromBlob(byte[] list) {
        ArrayList<LocalDate> out = new ArrayList<>(list.length / 8);

        ByteBuffer buffer = ByteBuffer.wrap(list);
        LongBuffer longBuffer = buffer.asLongBuffer();
        while (longBuffer.hasRemaining()) {
            out.add(LocalDate.ofEpochDay(longBuffer.get()));
        }

        return out;
    }

    @TypeConverter
    public static byte[] localDateListToBlob(List<LocalDate> list) {
        ByteBuffer buffer = ByteBuffer.allocate(list.size() * 8);
        LongBuffer longBuffer = buffer.asLongBuffer();

        for (LocalDate date : list) {
            longBuffer.put(date.toEpochDay());
        }

        return buffer.array();
    }
    //</editor-fold>

    //<editor-fold desc="Collections" defaultstate="collapse">
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
    //</editor-fold>

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
    public static List<Person> personListFromString(String str) {
        try {
            JSONArray json = new JSONArray(str);
            List<Person> out = new ArrayList<>(json.length());

            for (int i = 0, length = json.length(); i < length; i += 2) {
                Person person = new Person(json.getLong(i));
                person.setUsername(json.getString(i + 1));
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
            jsonArray.put(person.getUsername());
        });
        return jsonArray.toString();
    }
}
