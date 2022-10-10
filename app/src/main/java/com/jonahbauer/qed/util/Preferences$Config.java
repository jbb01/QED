package com.jonahbauer.qed.util;

import com.jonahbauer.qed.R;

import eu.jonahbauer.android.preference.annotations.Preference;
import eu.jonahbauer.android.preference.annotations.PreferenceGroup;
import eu.jonahbauer.android.preference.annotations.Preferences;

@Preferences(name = "com.jonahbauer.qed.util.Preferences", makeFile = true, fluent = false, r = R.class, value = {
        @PreferenceGroup(name = "general", prefix = "preferences_general_", suffix = "_key", value = {
                @Preference(name = "remember_me", type = boolean.class),
                @Preference(name = "bug_report", type = void.class),
                @Preference(name = "github", type = void.class),
                @Preference(name = "night_mode", type = boolean.class)
        }),
        @PreferenceGroup(name = "chat", prefix = "preferences_chat_", suffix = "_key", value = {
                @Preference(name = "name", type = String.class, defaultValue = ""),
                @Preference(name = "channel", type = String.class, defaultValue = ""),
                @Preference(name = "color", type = void.class),
                @Preference(name = "sense", type = boolean.class),
                @Preference(name = "public_id", type = boolean.class),
                @Preference(name = "linkify", type = boolean.class, defaultValue = "true"),
                @Preference(name = "katex", type = boolean.class),
                @Preference(name = "colorful", type = boolean.class),
                @Preference(name = "db_max_result", type = int.class, defaultValue = "50_000"),
                @Preference(name = "delete_db", type = void.class)
        }),
        @PreferenceGroup(name = "gallery", prefix = "preferences_gallery_", suffix = "_key", value = {
                @Preference(name = "offline_mode", type = boolean.class),
                @Preference(name = "delete_thumbnails", type = void.class),
                @Preference(name = "delete_images", type = void.class),
                @Preference(name = "delete_db", type = void.class),
                @Preference(name = "show_dir", type = void.class)
        })
})
class Preferences$Config {}