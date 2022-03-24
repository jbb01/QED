package com.jonahbauer.qed.model;

import android.annotation.SuppressLint;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import com.jonahbauer.qed.model.parcel.ParcelExtensions;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenCustomHashSet;
import lombok.Data;

@Data
public class Event implements Comparable<Event>, Parcelable {
    private final long id;
    private String title;
    private Double cost;
    private String notes;
    private Integer maxParticipants;

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
    private final Set<Registration> participants = new ObjectLinkedOpenCustomHashSet<>(Registration.STRATEGY_ID);

    private transient List<Registration> organizers;
    // hash code of participants map for calculated organizers
    private transient int organizersValid = 0;

    private Instant loaded;

    public boolean hasParticipantInformation() {
        return participants.size() != organizers.size()
                || (emailAll != null && !emailAll.trim().equals(""));
    }

    public boolean hasOrganizerInformation() {
        return !organizers.isEmpty()
                || (emailOrga != null && !emailOrga.trim().equals(""));
    }

    @Override
    @NonNull
    public String toString() {
        List<String> entries = new LinkedList<>();
        if (title != null) entries.add( "\"title\":\"" + title + "\"");
        if (startString != null) entries.add( "\"start\":\"" + startString + "\"");
        if (endString != null) entries.add( "\"end\":\"" + endString + "\"");
        if (deadlineString != null) entries.add( "\"deadline\":\"" + deadlineString + "\"");
        if (cost != null) entries.add( "\"cost\":" + cost);
        if (maxParticipants != null) entries.add( "\"maxMember\":" + maxParticipants);
        if (id != -1) entries.add( "\"id\":" + id);
        if (hotel != null) entries.add( "\"hotel\":\"" + hotel + "\"");
        if (hotelAddress != null) entries.add( "\"hotelAddress\":\"" + hotelAddress + "\"");
        if (emailOrga != null) entries.add( "\"emailOrga\":\"" + emailOrga + "\"");
        if (emailAll != null) entries.add( "\"emailAll\":\"" + emailAll + "\"");
        if (!participants.isEmpty()) entries.add( "\"participants\":\"" + participants + "\"");
        return entries.stream().collect(Collectors.joining(", ", "{", "}"));
    }

    public List<Registration> getOrganizers() {
        if (organizers == null || organizersValid != participants.hashCode()) {
            organizers = participants.stream()
                                     .filter(Registration::isOrganizer)
                                     .collect(Collectors.toList());
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
        dest.writeValue(cost);
        dest.writeString(notes);
        dest.writeValue(maxParticipants);

        ParcelExtensions.writeLocalDate(dest, start);
        dest.writeString(startString);

        ParcelExtensions.writeLocalDate(dest, end);
        dest.writeString(endString);

        ParcelExtensions.writeLocalDate(dest, deadline);
        dest.writeString(deadlineString);

        dest.writeString(hotel);
        dest.writeString(hotelAddress);

        dest.writeString(emailOrga);
        dest.writeString(emailAll);

        ParcelExtensions.writeTypedCollection(dest, participants);
        ParcelExtensions.writeInstant(dest, loaded);
    }

    public static final Parcelable.Creator<Event> CREATOR = new Parcelable.Creator<>() {
        @Override
        @SuppressLint("ParcelClassLoader") // no class loader required since only primitive wrappers are read
        public Event createFromParcel(@NonNull Parcel source) {
            Event event = new Event(source.readLong());
            event.title = source.readString();
            event.cost = (Double) source.readValue(null);
            event.notes = source.readString();
            event.maxParticipants = (Integer) source.readValue(null);

            event.start = ParcelExtensions.readLocalDate(source);
            event.startString = source.readString();

            event.end = ParcelExtensions.readLocalDate(source);
            event.endString = source.readString();

            event.deadline = ParcelExtensions.readLocalDate(source);
            event.deadlineString = source.readString();

            event.hotel = source.readString();
            event.hotelAddress = source.readString();

            event.emailOrga = source.readString();
            event.emailAll = source.readString();

            ParcelExtensions.readTypedCollection(source, event.participants, Registration.CREATOR);

            event.loaded = ParcelExtensions.readInstant(source);

            return event;
        }

        @Override
        public Event[] newArray(int size) {
            return new Event[size];
        }
    };
}
