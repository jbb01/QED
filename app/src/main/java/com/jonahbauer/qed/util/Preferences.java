package com.jonahbauer.qed.util;

import android.content.SharedPreferences;
import android.content.res.Resources;

import com.jonahbauer.qed.R;
import com.jonahbauer.qed.activities.MainActivity;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Preferences {
    private boolean mInitialized;
    private SharedPreferences mSharedPreferences;

    private General mGeneral;
    private Chat mChat;
    private Gallery mGallery;

    public void init(SharedPreferences sharedPreferences, Resources resources) {
        if (mInitialized) throw new IllegalStateException("PasswordStorage is already initialized.");
        mSharedPreferences = sharedPreferences;

        mGeneral = new General(resources);
        mChat = new Chat(resources);
        mGallery = new Gallery(resources);

        mInitialized = true;
    }

    public General general() {
        if (!mInitialized) throw new IllegalStateException("Preferences are not initialized.");
        return mGeneral;
    }

    public Chat chat() {
        if (!mInitialized) throw new IllegalStateException("Preferences are not initialized.");
        return mChat;
    }

    public Gallery gallery() {
        if (!mInitialized) throw new IllegalStateException("Preferences are not initialized.");
        return mGallery;
    }

    public class General {
        private final String KEY_DRAWER_SELECTION;
        private final String KEY_REMEMBER_ME;

        private final String KEY_BUG_REPORT;
        private final String KEY_GITHUB;


        private final Keys mKeys;

        private General(Resources resources) {
            KEY_DRAWER_SELECTION = resources.getString(R.string.preferences_general_drawer_selection_key);
            KEY_REMEMBER_ME = resources.getString(R.string.preferences_general_remember_me_key);

            KEY_BUG_REPORT = resources.getString(R.string.preferences_general_bug_report_key);
            KEY_GITHUB = resources.getString(R.string.preferences_general_github_key);

            mKeys = new Keys();
        }

        public MainActivity.DrawerSelection getDrawerSelection() {
            return MainActivity.DrawerSelection.byId(mSharedPreferences.getInt(KEY_DRAWER_SELECTION, -1), MainActivity.DrawerSelection.CHAT);
        }

        public boolean isRememberMe() {
            return mSharedPreferences.getBoolean(KEY_REMEMBER_ME, false);
        }

        public Keys keys() {
            return mKeys;
        }

        public Editor edit() {
            return new Editor(mSharedPreferences.edit());
        }

        public class Keys {
            private Keys() {}

            public String drawerSelection() {
                return KEY_DRAWER_SELECTION;
            }

            public String bugReport() {
                return KEY_BUG_REPORT;
            }

            public String github() {
                return KEY_GITHUB;
            }
        }

        public class Editor extends Preferences.Editor {
            private Editor(SharedPreferences.Editor editor) {
                super(editor);
            }

            public Editor setDrawerSelection(MainActivity.DrawerSelection drawerSelection) {
                mEditor.putInt(KEY_DRAWER_SELECTION, drawerSelection.getId());
                return this;
            }

            public Editor setRememberMe(boolean rememberMe) {
                mEditor.putBoolean(KEY_REMEMBER_ME, rememberMe);
                return this;
            }
        }
    }

    public class Chat {
        private final String KEY_NAME;
        private final String KEY_CHANNEL;
        private final String KEY_SENSE;
        private final String KEY_PUBLIC_ID;
        private final String KEY_LINKIFY;
        private final String KEY_KATEX;
        private final String KEY_COLORFUL;
        private final String KEY_DATABASE_MAX_RESULTS;
        private final String KEY_DATABASE_CLEAR;

        private final Keys mKeys;

        private Chat(Resources resources) {
            KEY_NAME = resources.getString(R.string.preferences_chat_name_key);
            KEY_CHANNEL = resources.getString(R.string.preferences_chat_channel_key);
            KEY_SENSE = resources.getString(R.string.preferences_chat_sense_key);
            KEY_PUBLIC_ID = resources.getString(R.string.preferences_chat_public_id_key);
            KEY_LINKIFY = resources.getString(R.string.preferences_chat_linkify_key);
            KEY_KATEX = resources.getString(R.string.preferences_chat_katex_key);
            KEY_COLORFUL = resources.getString(R.string.preferences_chat_colorful_key);

            KEY_DATABASE_MAX_RESULTS = resources.getString(R.string.preferences_chat_db_max_result_key);
            KEY_DATABASE_CLEAR = resources.getString(R.string.preferences_chat_delete_db_key);

            mKeys = new Keys();
        }

        public String getName() {
            return mSharedPreferences.getString(KEY_NAME, "");
        }

        public String getChannel() {
            return mSharedPreferences.getString(KEY_CHANNEL, "");
        }

        public boolean isSense() {
            return mSharedPreferences.getBoolean(KEY_SENSE, false);
        }

        public boolean isPublicId() {
            return mSharedPreferences.getBoolean(KEY_PUBLIC_ID, false);
        }

        public boolean isLinkify() {
            return mSharedPreferences.getBoolean(KEY_LINKIFY, true);
        }

        public boolean isKatex() {
            return mSharedPreferences.getBoolean(KEY_KATEX, false);
        }

        public boolean isColorful() {
            return mSharedPreferences.getBoolean(KEY_COLORFUL, false);
        }

        public int getDbMaxResults() {
            return mSharedPreferences.getInt(KEY_DATABASE_MAX_RESULTS, 50_000);
        }

        public Keys keys() {
            return mKeys;
        }

        public Editor edit() {
            return new Editor(mSharedPreferences.edit());
        }

        public class Keys {
            private Keys() {}

            public String name() {
                return KEY_NAME;
            }

            public String channel() {
                return KEY_CHANNEL;
            }

            public String sense() {
                return KEY_SENSE;
            }

            public String publicId() {
                return KEY_PUBLIC_ID;
            }

            public String linkify() {
                return KEY_LINKIFY;
            }

            public String katex() {
                return KEY_KATEX;
            }

            public String colorful() {
                return KEY_COLORFUL;
            }

            public String dbMaxResults() {
                return KEY_DATABASE_MAX_RESULTS;
            }

            public String dbClear() {
                return KEY_DATABASE_CLEAR;
            }
        }

        public class Editor extends Preferences.Editor {
            private Editor(SharedPreferences.Editor editor) {
                super(editor);
            }

            public Editor setName(String name) {
                mEditor.putString(KEY_NAME, name);
                return this;
            }

            public Editor setChannel(String channel) {
                mEditor.putString(KEY_CHANNEL, channel);
                return this;
            }

            public Editor setSense(boolean sense) {
                mEditor.putBoolean(KEY_SENSE, sense);
                return this;
            }

            public Editor setPublicId(boolean publicId) {
                mEditor.putBoolean(KEY_PUBLIC_ID, publicId);
                return this;
            }

            public Editor setLinkify(boolean linkify) {
                mEditor.putBoolean(KEY_LINKIFY, linkify);
                return this;
            }

            public Editor setKatex(boolean katex) {
                mEditor.putBoolean(KEY_KATEX, katex);
                return this;
            }

            public Editor setColorful(boolean colorful) {
                mEditor.putBoolean(KEY_COLORFUL, colorful);
                return this;
            }
        }
    }

    public class Gallery {
        private final String KEY_OFFLINE_MODE;

        private final String KEY_DELETE_THUMBNAILS;
        private final String KEY_DELETE_IMAGES;
        private final String KEY_DELETE_DB;

        private final String KEY_SHOW_DIR;

        private final Keys mKeys;

        private Gallery(Resources resources) {
            KEY_OFFLINE_MODE = resources.getString(R.string.preferences_gallery_offline_mode_key);

            KEY_DELETE_THUMBNAILS = resources.getString(R.string.preferences_gallery_delete_thumbnails_key);
            KEY_DELETE_IMAGES = resources.getString(R.string.preferences_gallery_delete_images_key);
            KEY_DELETE_DB = resources.getString(R.string.preferences_gallery_delete_db_key);

            KEY_SHOW_DIR = resources.getString(R.string.preferences_gallery_show_dir_key);

            mKeys = new Keys();
        }

        public boolean isOfflineMode() {
            return mSharedPreferences.getBoolean(KEY_OFFLINE_MODE, false);
        }

        public Keys keys() {
            return mKeys;
        }

        public Editor edit() {
            return new Editor(mSharedPreferences.edit());
        }

        public class Keys {
            private Keys() {}

            public String offlineMode() {
                return KEY_OFFLINE_MODE;
            }

            public String deleteThumbnails() {
                return KEY_DELETE_THUMBNAILS;
            }

            public String deleteImages() {
                return KEY_DELETE_IMAGES;
            }

            public String deleteDatabase() {
                return KEY_DELETE_DB;
            }

            public String showDir() {
                return KEY_SHOW_DIR;
            }
        }

        public class Editor extends Preferences.Editor {
            private Editor(SharedPreferences.Editor editor) {
                super(editor);
            }

            public Editor setOfflineMode(boolean offlineMode) {
                mEditor.putBoolean(KEY_OFFLINE_MODE, offlineMode);
                return this;
            }
        }
    }

    public static abstract class Editor {
        protected final SharedPreferences.Editor mEditor;

        private Editor(SharedPreferences.Editor editor) {
            this.mEditor = editor;
        }

        public void apply() {
            mEditor.apply();
        }

        public boolean commit() {
            return mEditor.commit();
        }
    }
}
