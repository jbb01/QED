package com.jonahbauer.qed.qeddb.event;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.jonahbauer.qed.qeddb.person.Person;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class Event implements Comparable<Event>, Parcelable {
    private static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY);
    private static final SimpleDateFormat simpleDateFormat2 = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.GERMANY);
    private static final SimpleDateFormat simpleDateFormat3 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.GERMANY);

    public String title;
    public Date start;
    public Date end;
    public Date deadline;
    public String startString;
    public String endString;
    public String deadlineString;
    public int cost;
    public int maxParticipants;
    public long id;
    public String hotel;
    public ArrayList<Person> organizers;
    public ArrayList<Person> participants;

    public Event() {
        this(null, null, null);
    }

    public Event(String title, String start, String end) {
        this(title, start, end, null, null, null, null);
    }

    public Event(String title, String start, String end, String cost, String deadline, String maxParticipants, String hotel) {
        this(null, title,start,end,cost,deadline, maxParticipants,hotel);
    }

    public Event(String id, String title, String start, String end, String cost, String deadline, String maxParticipants, String hotel) {
        organizers = new ArrayList<>();
        participants = new ArrayList<>();

        this.title = title;
        this.startString = start;
        this.endString = end;
        this.deadlineString = deadline;
        this.hotel = hotel;

        try {
            this.id = Long.parseLong(id);
        } catch (NumberFormatException e) {
            this.id = -1;
        }

        try {
            this.cost = Integer.parseInt(cost);
        } catch (NumberFormatException e) {
            this.cost = -1;
        }

        try {
            this.maxParticipants = Integer.parseInt(maxParticipants);
        } catch (NumberFormatException e) {
            this.maxParticipants = -1;
        }

        if (startString != null) try {
            this.start = simpleDateFormat2.parse(startString);
        } catch (ParseException e) {
            try {
                this.start = simpleDateFormat.parse(startString);
            } catch (ParseException ignored) {}
        }

        if (endString != null) try {
            this.end = simpleDateFormat2.parse(endString);
        } catch (ParseException e) {
            try {
                this.end = simpleDateFormat.parse(endString);
            } catch (ParseException ignored) {}
        }

        if (deadlineString != null) try {
            this.deadline = simpleDateFormat3.parse(deadlineString);
        } catch (ParseException ignored) {}
    }

    @Override
    @NonNull
    public String toString() {
        return "{" +
                "\"title\":\"" + title + "\"," +
                "\"start\":\"" + startString + "\"," +
                "\"end\":\"" + endString + "\"," +
                "\"deadline\":\"" + deadlineString + "\"," +
                "\"cost\":" + cost + "," +
                "\"maxMember\":" + maxParticipants + "," +
                "\"id\":" + id + "," +
                "\"hotel\":\"" + hotel + "\"," +
                "\"organizer\":" + organizers + "," +
                "\"members\":\"" + participants + "\"" +
                "}";
    }

    @Override
    public int compareTo(@NonNull Event o) {
        if (this.start != null && o.start != null) {
            return this.start.compareTo(o.start);
        } else {
            return this.startString.compareTo(o.startString);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(title);
        dest.writeString(startString);
        dest.writeSerializable(start);
        dest.writeString(endString);
        dest.writeSerializable(end);
        dest.writeString(deadlineString);
        dest.writeSerializable(deadline);
        dest.writeInt(cost);
        dest.writeInt(maxParticipants);
        dest.writeLong(id);
        dest.writeString(hotel);
        dest.writeTypedList(organizers);
        dest.writeTypedList(participants);
    }

    public static final Parcelable.Creator<Event> CREATOR = new Parcelable.Creator<Event>() {
        @Override
        public Event createFromParcel(@NonNull Parcel source) {
            Event event = new Event();
            event.title = source.readString();
            event.startString = source.readString();
            event.start = (Date) source.readSerializable();
            event.endString = source.readString();
            event.end = (Date) source.readSerializable();
            event.deadlineString = source.readString();
            event.deadline = (Date) source.readSerializable();
            event.cost = source.readInt();
            event.maxParticipants = source.readInt();
            event.id = source.readLong();
            event.hotel = source.readString();
            source.readTypedList(event.organizers, Person.CREATOR);
            source.readTypedList(event.participants, Person.CREATOR);
            return event;
        }

        @Override
        public Event[] newArray(int size) {
            return new Event[size];
        }
    };
}
