package eu.jonahbauer.qed.model;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import eu.jonahbauer.qed.model.parcel.LambdaCreator;
import eu.jonahbauer.qed.model.parcel.ParcelExtensions;
import eu.jonahbauer.qed.network.util.NetworkConstants;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigInteger;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Pattern;

@Value
@EqualsAndHashCode(of = "version")
@AllArgsConstructor
public class Release implements Comparable<Release>, Parcelable {

    private static final String KEY_VERSION = "tag_name";
    private static final String KEY_URL = "html_url";
    private static final String KEY_CREATED_AT = "created_at";
    private static final String KEY_NOTES = "body";
    private static final String KEY_PRERELEASE = "prerelease";

    /**
     * The version string.
     */
    @NonNull Version version;
    /**
     * The URL of the release website on GitHub.
     */
    @NonNull String url;
    /**
     * The timestamp of when the release was created.
     */
    @NonNull Instant createdAt;
    /**
     * The release notes.
     */
    @Nullable String notes;
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

    public Release(@NonNull JSONObject json) throws JSONException {
        this(
                json.getString(KEY_VERSION),
                json.getString(KEY_URL),
                parseInstant(json.getString(KEY_CREATED_AT)),
                json.has(KEY_NOTES) ? json.getString(KEY_NOTES) : null,
                json.getBoolean(KEY_PRERELEASE)
        );
    }

    public Release(@NonNull String version, @NonNull Instant createdAt) {
        this(new Version(version), createdAt);
    }

    public Release(@NonNull Version version, @NonNull Instant createdAt) {
        this(version, "https://github.com/jbb01/QED/releases/" + version, createdAt, null, version.isPrerelease());
    }

    public Release(@NonNull String version, @NonNull String url, @NonNull Instant createdAt, @Nullable String notes, boolean prerelease) {
        this(new Version(version), url, createdAt, notes, prerelease);
    }

    @SuppressWarnings("UnnecessaryInitCause") // would require api 27 to fix
    private static @NonNull Instant parseInstant(@NonNull String str) throws JSONException {
        try {
            return Instant.parse(str);
        } catch (DateTimeParseException e) {
            throw (JSONException) new JSONException(e.toString()).initCause(e);
        }
    }

    @Override
    public int compareTo(@NonNull Release other) {
        return this.getVersion().compareTo(other.getVersion());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        version.writeToParcel(dest, 0);
        dest.writeString(url);
        ParcelExtensions.writeInstant(dest, createdAt);
        dest.writeString(notes);
        ParcelExtensions.writeBoolean(dest, prerelease);
    }

    public static final Creator<Release> CREATOR = new LambdaCreator<>(Release[]::new, source -> new Release(
            Objects.requireNonNull(Version.CREATOR.createFromParcel(source)),
            Objects.requireNonNull(source.readString()),
            Objects.requireNonNull(ParcelExtensions.readInstant(source)),
            source.readString(),
            ParcelExtensions.readBoolean(source)
    ));

    /**
     * @see <a href="https://semver.org/">Semantic Versioning 2.0.0</a>
     */
    @Value
    public static class Version implements Parcelable, Comparable<Version> {
        private static final String NUMERIC_IDENTIFIER = "0|[1-9]\\d*";
        private static final String ALPHANUMERIC_IDENTIFIER = "\\d*[a-zA-Z-][0-9a-zA-Z-]*";
        private static final String PRERELEASE_IDENTIFIER = ALPHANUMERIC_IDENTIFIER + "|" + NUMERIC_IDENTIFIER;
        private static final String BUILD_IDENTIFIER = ALPHANUMERIC_IDENTIFIER + "|" + "[0-9]+";

        private static final String MAJOR = "(" + NUMERIC_IDENTIFIER + ")";
        private static final String MINOR = "(" + NUMERIC_IDENTIFIER + ")";
        private static final String PATCH = "(" + NUMERIC_IDENTIFIER + ")";

        private static final String PRE_RELEASE = "((?:" + PRERELEASE_IDENTIFIER + ")(?:\\.(?:" + PRERELEASE_IDENTIFIER + "))*)";
        private static final String BUILD = "((?:" + BUILD_IDENTIFIER + ")(?:\\.(?:" + BUILD_IDENTIFIER + "))*)";

        private static final String VERSION_CORE = MAJOR + "\\." + MINOR + "\\." + PATCH;

        private static final Pattern PRERELEASE_IDENTIFIER_PATTERN = Pattern.compile(PRERELEASE_IDENTIFIER);
        private static final Pattern BUILD_IDENTIFIER_PATTERN = Pattern.compile(BUILD_IDENTIFIER);
        private static final Pattern NUMERIC_IDENTIFIER_PATTERN = Pattern.compile(NUMERIC_IDENTIFIER);
        private static final Pattern VERSION_PATTERN = Pattern.compile(
                VERSION_CORE + "(?:-" + PRE_RELEASE + ")?" + "(?:\\+" + BUILD + ")?"
        );

        private static final Comparator<Version> COMPARATOR = Comparator
                .comparing(Version::getMajor)
                .thenComparing(Version::getMinor)
                .thenComparing(Version::getPatch)
                .thenComparing(Version::getPrerelease, Version::comparePrereleaseIdentifiers);

        int major;
        int minor;
        int patch;

        @NonNull List<String> prerelease;
        @NonNull List<String> build;

        public Version(@NonNull String version) {
            var matcher = VERSION_PATTERN.matcher(version.startsWith("v") ? version.substring(1) : version);
            if (!matcher.matches()) {
                throw new IllegalArgumentException("Invalid version string: " + version);
            }

            this.major = Integer.parseInt(Objects.requireNonNull(matcher.group(1)));
            this.minor = Integer.parseInt(Objects.requireNonNull(matcher.group(2)));
            this.patch = Integer.parseInt(Objects.requireNonNull(matcher.group(3)));

            var prerelease = matcher.group(4);
            this.prerelease = prerelease == null
                    ? Collections.emptyList()
                    : List.of(prerelease.split("\\."));

            var build = matcher.group(5);
            this.build = build == null
                    ? Collections.emptyList()
                    : List.of(build.split("\\."));
        }

        public Version(int major, int minor, int patch) {
            this(major, minor, patch, Collections.emptyList());
        }

        public Version(int major, int minor, int patch, @NonNull List<String> prerelease) {
            this(major, minor, patch, prerelease, Collections.emptyList());
        }

        public Version(int major, int minor, int patch, @NonNull List<String> prerelease, @NonNull List<String> build) {
            if (major < 0) throw new IllegalArgumentException("major must be non-negative");
            if (minor < 0) throw new IllegalArgumentException("minor must be non-negative");
            if (patch < 0) throw new IllegalArgumentException("patch must be non-negative");

            prerelease = List.copyOf(prerelease);
            for (String pre : prerelease) {
                if (!PRERELEASE_IDENTIFIER_PATTERN.matcher(pre).matches()) {
                    throw new IllegalArgumentException("prerelease must match " + PRERELEASE_IDENTIFIER);
                }
            }

            build = List.copyOf(build);
            for (String b : build) {
                if (!BUILD_IDENTIFIER_PATTERN.matcher(b).matches()) {
                    throw new IllegalArgumentException("build must match " + BUILD_IDENTIFIER);
                }
            }

            this.major = major;
            this.minor = minor;
            this.patch = patch;
            this.prerelease = prerelease;
            this.build = build;
        }

        public boolean isPrerelease() {
            return !prerelease.isEmpty();
        }

        @Override
        public int compareTo(@NonNull Version other) {
            return COMPARATOR.compare(this, other);
        }

        @NonNull
        @Override
        public String toString() {
            return "v" + major + "." + minor + "." + patch
                    + (prerelease.isEmpty() ? "" : "-" + String.join(".", prerelease))
                    + (build.isEmpty() ? "" : "+" + String.join(".", build));
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(@NonNull Parcel parcel, int i) {
            parcel.writeInt(major);
            parcel.writeInt(minor);
            parcel.writeInt(patch);
            parcel.writeStringList(prerelease);
            parcel.writeStringList(build);
        }

        public static final Creator<Version> CREATOR = new LambdaCreator<>(Version[]::new, source -> {
            var major = source.readInt();
            var minor = source.readInt();
            var patch = source.readInt();

            var prerelease = new ArrayList<String>();
            source.readStringList(prerelease);

            var build = new ArrayList<String>();
            source.readStringList(build);

            return new Version(major, minor, patch, Collections.unmodifiableList(prerelease), Collections.unmodifiableList(build));
        });

        private static int comparePrereleaseIdentifiers(@NonNull List<String> a, @NonNull List<String> b) {
            if (a.isEmpty() && b.isEmpty()) return 0;

            // When major, minor, and patch are equal, a pre-release version has lower precedence than a normal version
            if (a.isEmpty()) return 1;
            if (b.isEmpty()) return -1;

            for (int i = 0, length = Math.min(a.size(), b.size()); i < length; i++) {
                var val = comparePrereleaseIdentifier(a.get(i), b.get(i));
                if (val != 0) return val;
            }

            // A larger set of pre-release fields has a higher precedence than a smaller set, if all of the preceding
            // identifiers are equal.
            if (a.size() != b.size()) {
                return a.size() - b.size();
            }

            return 0;
        }

        private static int comparePrereleaseIdentifier(@NonNull String a, @NonNull String b) {
            boolean numericA = NUMERIC_IDENTIFIER_PATTERN.matcher(a).matches();
            boolean numericB = NUMERIC_IDENTIFIER_PATTERN.matcher(b).matches();

            // Identifiers consisting of only digits are compared numerically.
            if (numericA && numericB) {
                if (a.length() <= 18 && b.length() <= 18) {
                    return Long.compare(Long.parseLong(a), Long.parseLong(b));
                } else {
                    return new BigInteger(a).compareTo(new BigInteger(b));
                }
            }

            // Numeric identifiers always have lower precedence than non-numeric identifiers.
            if (numericA) {
                return -1;
            } else if (numericB) {
                return 1;
            }

            return a.compareTo(b);
        }
    }
}
