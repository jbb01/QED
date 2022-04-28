package com.jonahbauer.qed.model;

import android.annotation.SuppressLint;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import com.jonahbauer.qed.model.parcel.ParcelExtensions;
import com.jonahbauer.qed.model.util.ParsedLocalDate;
import com.jonahbauer.qed.util.TextUtils;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenCustomHashSet;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(of = "id")
public class Event implements Comparable<Event>, Parcelable {
    public static final long NO_ID = Long.MIN_VALUE;

    private final long id;
    private String title;
    private Double cost;
    private String notes;
    private Integer maxParticipants;

    private ParsedLocalDate start;
    private ParsedLocalDate end;
    private ParsedLocalDate deadline;

    private String hotel;
    private String hotelAddress;

    private String emailOrga;
    private String emailAll;
    private final Set<Registration> participants = new ObjectLinkedOpenCustomHashSet<>(Registration.STRATEGY_ID);

    private transient List<Registration> organizers;
    // hash code of participants map for calculated organizers
    private transient int organizersValid = 0;

    private Instant loaded;

    public boolean hasParticipants() {
        return participants.size() != organizers.size();
    }

    public boolean hasParticipantInformation() {
        return hasParticipants() || !TextUtils.isNullOrBlank(emailAll);
    }

    public boolean hasOrganizerInformation() {
        return !organizers.isEmpty() || !TextUtils.isNullOrBlank(emailOrga);
    }

    @Override
    @NonNull
    public String toString() {
        List<String> entries = new LinkedList<>();
        if (title != null) entries.add( "\"title\":\"" + title + "\"");
        if (start != null) entries.add( "\"start\":\"" + start + "\"");
        if (end != null) entries.add( "\"end\":\"" + end + "\"");
        if (deadline != null) entries.add( "\"deadline\":\"" + deadline + "\"");
        if (cost != null) entries.add( "\"cost\":" + cost);
        if (maxParticipants != null) entries.add( "\"max_participants\":" + maxParticipants);
        if (id != NO_ID) entries.add( "\"id\":" + id);
        if (hotel != null) entries.add( "\"hotel\":\"" + hotel + "\"");
        if (hotelAddress != null) entries.add( "\"hotel_address\":\"" + hotelAddress + "\"");
        if (emailOrga != null) entries.add( "\"email_orga\":\"" + emailOrga + "\"");
        if (emailAll != null) entries.add( "\"email_all\":\"" + emailAll + "\"");
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
            return 0;
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

        dest.writeTypedObject(start, flags);
        dest.writeTypedObject(end, flags);
        dest.writeTypedObject(deadline, flags);

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

            event.start = source.readTypedObject(ParsedLocalDate.CREATOR);
            event.end = source.readTypedObject(ParsedLocalDate.CREATOR);
            event.deadline = source.readTypedObject(ParsedLocalDate.CREATOR);

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
