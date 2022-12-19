package com.jonahbauer.qed.model;

import android.annotation.SuppressLint;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import com.jonahbauer.qed.model.parcel.ParcelExtensions;
import com.jonahbauer.qed.model.parcel.LambdaCreator;
import com.jonahbauer.qed.model.util.ParsedLocalDate;
import com.jonahbauer.qed.util.TextUtils;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenCustomHashSet;
import lombok.*;

@Data
@EqualsAndHashCode(of = "id")
public class Event implements Comparable<Event>, Parcelable {
    public static final long NO_ID = Long.MIN_VALUE;

    private final long id;
    private String title;
    private Double cost;
    private String notes;
    private Integer maxParticipants;
    private String paymentReference;

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
    @Getter(AccessLevel.PRIVATE)
    @Setter(AccessLevel.PRIVATE)
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
        var joiner = new StringJoiner(", ", "{", "}");
        if (title != null) joiner.add( "\"title\":\"" + title + "\"");
        if (start != null) joiner.add( "\"start\":\"" + start + "\"");
        if (end != null) joiner.add( "\"end\":\"" + end + "\"");
        if (deadline != null) joiner.add( "\"deadline\":\"" + deadline + "\"");
        if (cost != null) joiner.add( "\"cost\":" + cost);
        if (maxParticipants != null) joiner.add( "\"max_participants\":" + maxParticipants);
        if (id != NO_ID) joiner.add( "\"id\":" + id);
        if (hotel != null) joiner.add( "\"hotel\":\"" + hotel + "\"");
        if (hotelAddress != null) joiner.add( "\"hotel_address\":\"" + hotelAddress + "\"");
        if (emailOrga != null) joiner.add( "\"email_orga\":\"" + emailOrga + "\"");
        if (emailAll != null) joiner.add( "\"email_all\":\"" + emailAll + "\"");
        if (!participants.isEmpty()) joiner.add( "\"participants\":\"" + participants + "\"");
        return joiner.toString();
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
        dest.writeString(paymentReference);
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

    @SuppressLint("ParcelClassLoader")
    public static final Creator<Event> CREATOR = new LambdaCreator<>(Event[]::new, source -> {
        Event event = new Event(source.readLong());
        event.title = source.readString();
        event.cost = (Double) source.readValue(null);
        event.paymentReference = source.readString();
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
    });
}
