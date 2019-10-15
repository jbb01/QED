package com.jonahbauer.qed.qeddb.event;

import androidx.annotation.NonNull;

import com.jonahbauer.qed.qeddb.person.Person;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class Event implements Comparable<Event> {
    public String name;
    public Date start;
    public Date end;
    public Date deadline;
    public String startString;
    public String endString;
    public String deadlineString;
    public String cost;
    public String maxMember;
    public String id;
    public String hotel;
    public List<Person> organizer;
    public List<Person> members;

    public Event() {
        organizer = new ArrayList<>();
        members = new ArrayList<>();
    }

    @NonNull
    @Override
    public String toString() {
        Field[] fields = Event.class.getDeclaredFields();
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
            e.printStackTrace();
        }

        return "";
    }

    @Override
    public int compareTo(@NonNull Event o) {
        if (this.start != null && o.start != null) {
            return this.start.compareTo(o.start);
        } else {
            return this.startString.compareTo(o.startString);
        }
    }
}
