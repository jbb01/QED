package com.jonahbauer.qed.model;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.jonahbauer.qed.model.parcel.LambdaCreator;
import com.jonahbauer.qed.model.parcel.ParcelExtensions;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Value
@AllArgsConstructor
@EqualsAndHashCode(of = "version")
public class Release implements Comparable<Release>, Parcelable {
    private static final Pattern VERSION_PATTERN = Pattern.compile(
            "v([0-9]+)\\.([0-9]+)(?:\\.([0-9]+))?(?:-(alpha|beta|rc)([0-9]+)?)?"
    );

    private static final String KEY_VERSION = "tag_name";
    private static final String KEY_URL = "html_url";
    private static final String KEY_CREATED_AT = "created_at";
    private static final String KEY_NOTES = "body";
    private static final String KEY_PRERELEASE = "prerelease";

    /**
     * The version string.
     */
    @NonNull
    String version;
    /**
     * The URL of the release website on GitHub.
     */
    @NonNull
    String url;
    /**
     * The timestamp of when the release was created.
     */
    @NonNull
    Instant createdAt;
    /**
     * The release notes.
     */
    @Nullable
    String notes;
    /**
     * Whether this is a prerelease.
     */
    boolean prerelease;

    public static @NonNull List<Release> parseList(@NonNull JSONArray json) throws JSONException {
        List<Release> out = new ArrayList<>(json.length());
        for (int i = 0; i < json.length(); i++) {
            out.add(parse(json.getJSONObject(i)));
        }
        return Collections.unmodifiableList(out);
    }

    public static @NonNull Release parse(@NonNull JSONObject json) throws JSONException {
        return new Release(json);
    }

    @SuppressWarnings("UnnecessaryInitCause") // would require api 27 to fix
    public Release(@NonNull JSONObject json) throws JSONException {
        try {
            this.version = json.getString(KEY_VERSION);
            this.url = json.getString(KEY_URL);
            this.createdAt = Instant.parse(json.getString(KEY_CREATED_AT));
            this.notes = json.has(KEY_NOTES) ? json.getString(KEY_NOTES) : null;
            this.prerelease = json.getBoolean(KEY_PRERELEASE);
        } catch (DateTimeParseException e) {
            throw (JSONException) new JSONException(e.toString()).initCause(e);
        }
    }

    private Integer parseInt(String str) {
        return str == null ? null : Integer.parseInt(str);
    }

    @Override
    public int compareTo(@NonNull Release o) {
        Matcher matcher = VERSION_PATTERN.matcher(getVersion());
        Matcher oMatcher = VERSION_PATTERN.matcher(o.getVersion());
        if (!matcher.matches() || !oMatcher.matches()) throw new IllegalStateException();

        MatchResult result = matcher.toMatchResult();
        MatchResult oResult = oMatcher.toMatchResult();

        int major = parseInt(Objects.requireNonNull(result.group(1)));
        int minor = parseInt(Objects.requireNonNull(result.group(2)));
        Integer patch = parseInt(result.group(3));
        String suffix = result.group(4);
        Integer suffixVersion = parseInt(result.group(5));

        int oMajor = parseInt(Objects.requireNonNull(oResult.group(1)));
        int oMinor = parseInt(Objects.requireNonNull(oResult.group(2)));
        Integer oPatch = parseInt(oResult.group(3));
        String oSuffix = oResult.group(4);
        Integer oSuffixVersion = parseInt(oResult.group(5));

        if (major != oMajor) return major - oMajor;
        if (minor != oMinor) return minor - oMinor;
        if (patch == null && oPatch != null) return -1;
        if (patch != null && oPatch == null) return 1;
        if (!Objects.equals(patch, oPatch)) return patch.compareTo(oPatch);
        if (suffix == null && oSuffix != null) return 1;
        if (suffix != null && oSuffix == null) return -1;
        if (!Objects.equals(suffix, oSuffix)) return suffix.compareTo(oSuffix);
        if (suffixVersion == null && oSuffixVersion != null) return -1;
        if (suffixVersion != null && oSuffixVersion == null) return 1;
        if (!Objects.equals(suffixVersion, oSuffixVersion)) return suffixVersion.compareTo(oSuffixVersion);

        return getCreatedAt().compareTo(o.getCreatedAt());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(version);
        dest.writeString(url);
        ParcelExtensions.writeInstant(dest, createdAt);
        dest.writeString(notes);
        ParcelExtensions.writeBoolean(dest, prerelease);
    }

    public static final Creator<Release> CREATOR = new LambdaCreator<>(Release[]::new, source -> new Release(
            Objects.requireNonNull(source.readString()),
            Objects.requireNonNull(source.readString()),
            Objects.requireNonNull(ParcelExtensions.readInstant(source)),
            source.readString(),
            ParcelExtensions.readBoolean(source)
    ));
}
