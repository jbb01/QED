package eu.jonahbauer.qed.util;

import eu.jonahbauer.qed.BuildConfig;
import eu.jonahbauer.qed.R;

import eu.jonahbauer.qed.layoutStuff.themes.Theme;
import eu.jonahbauer.qed.model.Language;
import eu.jonahbauer.qed.model.Release;
import eu.jonahbauer.android.preference.annotations.Preference;
import eu.jonahbauer.android.preference.annotations.PreferenceGroup;
import eu.jonahbauer.android.preference.annotations.Preferences;
import eu.jonahbauer.android.preference.annotations.serializer.PreferenceSerializationException;
import eu.jonahbauer.android.preference.annotations.serializer.PreferenceSerializer;
import it.unimi.dsi.fastutil.longs.LongSet;

import java.util.*;
import java.util.stream.Collectors;

@Preferences(name = "eu.jonahbauer.qed.util.Preferences", makeFile = true, fluent = false, r = R.class, value = {
        @PreferenceGroup(name = "general", prefix = "preferences_general_", suffix = "_key", value = {
                @Preference(name = "remember_me", type = boolean.class),
                @Preference(name = "bug_report", type = void.class),
                @Preference(name = "language", type = Language.class, defaultValue = "SYSTEM"),
                @Preference(name = "github", type = void.class),
                @Preference(name = "theme", type = Theme.class, defaultValue = "LIGHT"),
                @Preference(name = "update_check_enabled", type = boolean.class, defaultValue = "true"),
                @Preference(name = "update_check_includes_prereleases", type = boolean.class, defaultValue = "" + BuildConfig.PRERELEASE),
                @Preference(name = "update_check_dont_suggest", type = Set.class, serializer = Preferences$Config.VersionSerializer.class),
                @Preference(name = "username", type = String.class)
        }),
        @PreferenceGroup(name = "chat", prefix = "preferences_chat_", suffix = "_key", value = {
                @Preference(name = "name", type = String.class, defaultValue = ""),
                @Preference(name = "recent_names", type = String.class, serializer = Preferences$Config.LinkedSetSerializer.class),
                @Preference(name = "channel", type = String.class, defaultValue = ""),
                @Preference(name = "recent_channels", type = String.class, serializer = Preferences$Config.LinkedSetSerializer.class),
                @Preference(name = "color", type = void.class),
                @Preference(name = "sense", type = boolean.class),
                @Preference(name = "public_id", type = boolean.class),
                @Preference(name = "linkify", type = boolean.class, defaultValue = "true"),
                @Preference(name = "katex", type = boolean.class),
                @Preference(name = "colorful", type = boolean.class),
                @Preference(name = "db_max_result", type = int.class, defaultValue = "50_000"),
                @Preference(name = "delete_db", type = void.class)
        }),
        @PreferenceGroup(name = "database", prefix = "preferences_database_", suffix = "_key", value = {
                @Preference(name = "favorites", type = String.class, serializer = Preferences$Config.LongSetSerializer.class)
        }),
        @PreferenceGroup(name = "gallery", prefix = "preferences_gallery_", suffix = "_key", value = {
                @Preference(name = "offline_mode", type = boolean.class),
                @Preference(name = "delete_thumbnails", type = void.class),
                @Preference(name = "delete_images", type = void.class),
                @Preference(name = "delete_db", type = void.class),
                @Preference(name = "show_dir", type = void.class)
        })
})
public final class Preferences$Config {

    private Preferences$Config() {
        throw new UnsupportedOperationException();
    }

    public static class LongSetSerializer implements PreferenceSerializer<LongSet, String> {

        @Override
        public String serialize(LongSet longs) throws PreferenceSerializationException {
            if (longs == null || longs.isEmpty()) return null;
            return longs.longStream().mapToObj(String::valueOf).collect(Collectors.joining(","));
        }

        @Override
        public LongSet deserialize(String s) throws PreferenceSerializationException {
            if (s == null || s.isEmpty()) return LongSet.of();
            return LongSet.of(Arrays.stream(s.split(",")).mapToLong(Long::parseLong).toArray());
        }
    }

    public static class LinkedSetSerializer implements PreferenceSerializer<Set<String>, String> {
        @Override
        public String serialize(Set<String> list) throws PreferenceSerializationException {
            if (list.isEmpty()) return null;

            return list.stream()
                    .map(string -> "\"" + string.replaceAll("\"", "\"\"") + "\"")
                    .collect(Collectors.joining(","));
        }

        @Override
        public Set<String> deserialize(String value) throws PreferenceSerializationException {
            if (value == null) return Collections.emptySet();

            boolean quote = false;
            Set<String> out = new LinkedHashSet<>();
            StringBuilder current = new StringBuilder();

            for (int i = 0, length = value.length(); i < length; i++) {
                char chr = value.charAt(i);
                if (chr == ',' && !quote) {
                    out.add(current.toString());
                    current.setLength(0);
                } else if (chr == '"') {
                    if (quote) {
                        if (i + 1 < length && value.charAt(i + 1) == '"') {
                            current.append('"');
                        }

                        quote = false;
                    } else {
                        quote = true;
                    }
                } else {
                    current.append(chr);
                }
            }

            out.add(current.toString());
            return Collections.unmodifiableSet(out);
        }
    }

    public static class VersionSerializer implements PreferenceSerializer<Set<Release.Version>, Set<String>> {

        @Override
        public Set<String> serialize(Set<Release.Version> value) throws PreferenceSerializationException {
            if (value == null) return null;

            var out = new HashSet<String>(value.size(), 1.0f);
            for (Release.Version version : value) {
                out.add(version.toString());
            }
            return out;
        }

        @Override
        public Set<Release.Version> deserialize(Set<String> value) throws PreferenceSerializationException {
            if (value == null) return null;

            var out = new TreeSet<Release.Version>();
            for (String s : value) {
                out.add(new Release.Version(s));
            }
            return Collections.unmodifiableSet(out);
        }
    }
}
