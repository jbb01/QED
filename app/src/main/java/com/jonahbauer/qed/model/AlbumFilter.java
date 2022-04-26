package com.jonahbauer.qed.model;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.jonahbauer.qed.model.parcel.ParcelExtensions;
import lombok.Data;
import lombok.experimental.ExtensionMethod;
import org.jetbrains.annotations.Contract;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
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

    public static AlbumFilter parse(@Nullable Uri uri) {
        if (uri == null) return null;

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
        if ("".equals(category)) {
            category = Album.CATEGORY_ETC;
        } else {
            try {
                category = URLDecoder.decode(category, "UTF-8");
            } catch (UnsupportedEncodingException ignored) {}
        }

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
            out += "&bycategory=";
            if (!Album.CATEGORY_ETC.equals(category)) {
                try {
                    out += URLEncoder.encode(category, "UTF-8");
                } catch (UnsupportedEncodingException ignored) {
                }
            }
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

    public static final Creator<AlbumFilter> CREATOR = new Creator<>() {
        @Override
        @SuppressLint("ParcelClassLoader")
        public AlbumFilter createFromParcel(@NonNull Parcel in) {
            return new AlbumFilter(
                    in.readLocalDate(),
                    in.readLocalDate(),
                    (Long) in.readValue(null),
                    in.readString()
            );
        }

        @Override
        public AlbumFilter[] newArray(int size) {
            return new AlbumFilter[size];
        }
    };
}
