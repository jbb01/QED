package com.jonahbauer.qed.qedgallery.image;

import android.util.Log;

import androidx.annotation.NonNull;

import com.jonahbauer.qed.Application;
import com.jonahbauer.qed.qedgallery.album.Album;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Image implements Serializable {
    static final Set<String> videoFileExtensions = new HashSet<>();
    static final Set<String> audioFileExtensions = new HashSet<>();

    static {
        videoFileExtensions.add("mp4");
        videoFileExtensions.add("mkv");
        videoFileExtensions.add("flv");
        videoFileExtensions.add("mov");
        videoFileExtensions.add("avi");
        videoFileExtensions.add("wmv");
        videoFileExtensions.add("mpeg");

        audioFileExtensions.add("wav");
        audioFileExtensions.add("mp3");
        audioFileExtensions.add("ogg");
        audioFileExtensions.add("m4a");
        audioFileExtensions.add("flac");
        audioFileExtensions.add("opus");
    }

    public transient Album album;
    public String albumName;
    public int id;
    public String format;
    public String path;
    public String name;
    public String owner;
    public Date uploadDate;
    public Date creationDate;
    public boolean original;
    public boolean available;

    public Map<String, String> data = new HashMap<>();

    public Image() {
        available = true;
    }

    @Override
    @NonNull
    public String toString() {
        Field[] fields = Image.class.getDeclaredFields();
        Object[] fieldsObject = Arrays.stream(fields).filter(field -> {
            if (field.getName().equals("album")) return false;
            try {
                return !(field.getName().equals("$change") || field.getName().equals("serialVersionUID") || field.get(this) == null);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            return false;
        }).toArray();
        fields = Arrays.copyOf(fieldsObject, fieldsObject.length, Field[].class);

        try {
            StringBuilder builder = new StringBuilder();
            builder.append("{");

            for (int i = 0; i < fields.length; i++) {
                Object value = fields[i].get(this);
                builder.append("\"").append(fields[i].getName()).append("\":\"").append(value != null ? value.toString() : "null").append("\"");

                if (i < fields.length - 1) builder.append(", ");
                else builder.append("}");
            }

            return builder.toString();
        } catch (IllegalAccessException e) {
            Log.e(Application.LOG_TAG_ERROR, e.getMessage(), e);
        }

        return "";
    }
}
