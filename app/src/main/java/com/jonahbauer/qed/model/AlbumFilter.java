package com.jonahbauer.qed.model;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.jonahbauer.qed.model.parcel.ParcelExtensions;
import com.jonahbauer.qed.model.parcel.LambdaCreator;
import lombok.Data;
import lombok.experimental.ExtensionMethod;
import org.jetbrains.annotations.Contract;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Data
@ExtensionMethod(ParcelExtensions.class)
public class AlbumFilter implements Parcelable {
    public static final AlbumFilter EMPTY = new AlbumFilter(null, null, null, null);

    @Nullable
    private final LocalDate day;
    @Nullable
    private final LocalDate upload;
    @Nullable
    private final Long owner;
    @Nullable
    private final String category;

    public static @NonNull AlbumFilter parse(@Nullable Uri uri) {
        if (uri == null) return AlbumFilter.EMPTY;

        LocalDate day = null;
        LocalDate upload = null;
        Long owner = null;
        String category;

        try {
            day = LocalDate.from(DateTimeFormatter.ISO_LOCAL_DATE.parse(uri.getQueryParameter("byday")));
        } catch (Exception ignored) {}

        try {
            upload = LocalDate.from(DateTimeFormatter.ISO_LOCAL_DATE.parse(uri.getQueryParameter("byupload")));
        } catch (Exception ignored) {}

        try {
            owner = Long.parseLong(uri.getQueryParameter("byowner"));
        } catch (Exception ignored) {}

        category = uri.getQueryParameter("bycategory");

        return new AlbumFilter(day, upload, owner, category);
    }

    @NonNull
    public String toString() {
        String out = "";
        if (day != null) {
            out += "&byday=" + DateTimeFormatter.ISO_LOCAL_DATE.format(day);
        }
        if (upload != null) {
            out += "&byupload=" + DateTimeFormatter.ISO_LOCAL_DATE.format(upload);
        }
        if (owner != null) {
            out += "&byowner=" + owner;
        }
        if (category != null) {
            out += "&bycategory=" + category;
        }
        return out;
    }

    @Contract(pure = true)
    public boolean isEmpty() {
        return day == null && upload == null && owner == null && category == null;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeLocalDate(day);
        dest.writeLocalDate(upload);
        dest.writeValue(owner);
        dest.writeString(category);
    }

    @SuppressLint("ParcelClassLoader")
    public static final Creator<AlbumFilter> CREATOR = new LambdaCreator<>(AlbumFilter[]::new, source -> {
        return new AlbumFilter(
                source.readLocalDate(),
                source.readLocalDate(),
                (Long) source.readValue(null),
                source.readString()
        );
    });
}
