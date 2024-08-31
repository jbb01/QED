package eu.jonahbauer.qed.model;

import android.annotation.SuppressLint;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;
import java.util.function.Predicate;

import eu.jonahbauer.qed.model.parcel.LambdaCreator;
import eu.jonahbauer.qed.util.Preferences;
import it.unimi.dsi.fastutil.longs.LongSet;
import lombok.Data;

@Data
public class PersonFilter implements Parcelable, Predicate<Person> {
    public static final PersonFilter EMPTY = new PersonFilter(null, null, null, null, null);

    private final @Nullable String firstName;
    private final @Nullable String lastName;
    private final @Nullable Boolean member;
    private final @Nullable Boolean active;
    private final @Nullable Boolean favorite;
    private final @Nullable LongSet favorites;

    public PersonFilter(
            @Nullable String firstName,
            @Nullable String lastName,
            @Nullable Boolean member,
            @Nullable Boolean active,
            @Nullable Boolean favorite
    ) {
        this.firstName = firstName == null ? null : firstName.trim().toLowerCase(Locale.ROOT);
        this.lastName = lastName == null ? null : lastName.trim().toLowerCase(Locale.ROOT);
        this.member = member;
        this.active = active;
        this.favorite = favorite;
        this.favorites = favorite != null ? Preferences.getDatabase().getFavorites() : null;
    }

    @Override
    public boolean test(Person person) {
        return (firstName == null || person.getFirstName().toLowerCase().contains(firstName))
                && (lastName == null || person.getLastName().toLowerCase().contains(lastName))
                && (member == null || person.getMember() == member)
                && (active == null || person.getActive() == active)
                && (favorite == null || favorites == null || favorite == favorites.contains(person.getId()));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(firstName);
        dest.writeString(lastName);
        dest.writeValue(member);
        dest.writeValue(active);
        dest.writeValue(favorite);
    }

    @SuppressLint("ParcelClassLoader")
    public static final Creator<PersonFilter> CREATOR = new LambdaCreator<>(PersonFilter[]::new, source -> {
        return new PersonFilter(
                source.readString(),
                source.readString(),
                (Boolean) source.readValue(null),
                (Boolean) source.readValue(null),
                (Boolean) source.readValue(null)
        );
    });
}
