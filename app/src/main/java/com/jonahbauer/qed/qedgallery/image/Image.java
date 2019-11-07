package com.jonahbauer.qed.qedgallery.image;

import android.util.Log;

import androidx.annotation.NonNull;

import com.jonahbauer.qed.Application;
import com.jonahbauer.qed.qedgallery.album.Album;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Date;

public class Image implements Serializable {
    public Album album;
    public String albumName;
    public int id;
    public String format;
    public String thumbnailPath;
    public String path;
    public String name;
    public String owner;
    public Date uploadDate;
    public Date creationDate;
    public boolean original;
    public boolean available;

    public Image() {
        available = true;
    }

    @Override
    @NonNull
    public String toString() {
        Field[] fields = Image.class.getDeclaredFields();
        Object[] fieldsObject = Arrays.stream(fields).filter(field -> {
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
