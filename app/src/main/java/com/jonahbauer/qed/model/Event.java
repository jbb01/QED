package com.jonahbauer.qed.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.Data;

@Data
public class Event implements Comparable<Event>, Parcelable {
    private final long id;
    private String title;
    private int cost;
    private String notes;
    private int maxParticipants;

    private LocalDate start;
    private String startString;

    private LocalDate end;
    private String endString;

    private LocalDate deadline;
    private String deadlineString;

    private String hotel;
    private String hotelAddress;

    private String emailOrga;
    private String emailAll;
    private final Map<String, Registration> participants = new LinkedHashMap<>();

    private Map<String, Registration> organizers;
    // hash code of participants map for calculated organizers
    private int organizersValid = 0;

    private Instant loaded;

    @Override
    @NonNull
    public String toString() {
        List<String> entries = new LinkedList<>();
        if (title != null) entries.add( "\"title\":\"" + title + "\"");
        if (startString != null) entries.add( "\"start\":\"" + startString + "\"");
        if (endString != null) entries.add( "\"end\":\"" + endString + "\"");
        if (deadlineString != null) entries.add( "\"deadline\":\"" + deadlineString + "\"");
        if (cost != -1) entries.add( "\"cost\":" + cost);
        if (maxParticipants != -1) entries.add( "\"maxMember\":" + maxParticipants);
        if (id != -1) entries.add( "\"id\":" + id);
        if (hotel != null) entries.add( "\"hotel\":\"" + hotel + "\"");
        if (hotelAddress != null) entries.add( "\"hotelAddress\":\"" + hotelAddress + "\"");
        if (emailOrga != null) entries.add( "\"emailOrga\":\"" + emailOrga + "\"");
        if (emailAll != null) entries.add( "\"emailAll\":\"" + emailAll + "\"");
        if (!participants.isEmpty()) entries.add( "\"participants\":\"" + participants + "\"");
        return entries.stream().collect(Collectors.joining(", ", "{", "}"));
    }

    public Map<String, Registration> getOrganizers() {
        if (organizers == null || organizersValid != participants.hashCode()) {
            organizers = participants.entrySet().stream()
                                     .filter(entry -> entry.getValue().isOrganizer())
                                     .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            organizersValid = participants.hashCode();
        }

        return organizers;
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
        dest.writeLong(id);
        dest.writeString(title);
        dest.writeString(startString);
        dest.writeSerializable(start);
        dest.writeString(endString);
        dest.writeSerializable(end);
        dest.writeString(deadlineString);
        dest.writeSerializable(deadline);
        dest.writeInt(cost);
        dest.writeInt(maxParticipants);
        dest.writeString(hotel);
        dest.writeString(notes);

        dest.writeInt(participants.size());
        for (Map.Entry<String, Registration> entry : participants.entrySet()) {
            dest.writeString(entry.getKey());
            dest.writeLong(entry.getValue().getId());
        }
        dest.writeLong(loaded != null ? loaded.toEpochMilli() : Long.MIN_VALUE);
    }

    public static final Parcelable.Creator<Event> CREATOR = new Parcelable.Creator<>() {
        @Override
        public Event createFromParcel(@NonNull Parcel source) {
            Event event = new Event(source.readLong());
            event.title = source.readString();
            event.startString = source.readString();
            event.start = (LocalDate) source.readSerializable();
            event.endString = source.readString();
            event.end = (LocalDate) source.readSerializable();
            event.deadlineString = source.readString();
            event.deadline = (LocalDate) source.readSerializable();
            event.cost = source.readInt();
            event.maxParticipants = source.readInt();
            event.hotel = source.readString();
            event.notes = source.readString();

            int participantCount = source.readInt();
            for (int i = 0; i < participantCount; i++) {
                event.participants.put(
                        source.readString(),
                        new Registration(source.readLong())
                );
            }

            var loaded = source.readLong();
            event.loaded = loaded == Long.MIN_VALUE ? null : Instant.ofEpochMilli(loaded);

            return event;
        }

        @Override
        public Event[] newArray(int size) {
            return new Event[size];
        }
    };
}
