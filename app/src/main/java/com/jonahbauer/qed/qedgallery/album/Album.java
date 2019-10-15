package com.jonahbauer.qed.qedgallery.album;

import android.util.Log;

import androidx.annotation.NonNull;

import com.jonahbauer.qed.Application;
import com.jonahbauer.qed.qeddb.person.Person;
import com.jonahbauer.qed.qedgallery.image.Image;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class Album implements Serializable {
    public String name;
    public int id;
    public String owner;
    public String creationDate;
    public List<Person> persons;
    public List<Date> dates;
    public List<String> categories;
    public List<Image> images;
    public boolean imageListDownloaded;

    public Album() {
        persons = new ArrayList<>();
        dates = new ArrayList<>();
        categories = new ArrayList<>();
        images = new ArrayList<>();
    }

    @NonNull
    @Override
    public String toString() {
        Field[] fields = Album.class.getDeclaredFields();
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
                builder.append("\"").append(fields[i].getName()).append("\":\"").append(value.toString()).append("\"");

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
