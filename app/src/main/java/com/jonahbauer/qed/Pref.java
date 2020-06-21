package com.jonahbauer.qed;

import android.content.res.Resources;

import androidx.annotation.NonNull;

public final class Pref {
    public static General General;
    public static Chat Chat;
    public static Gallery Gallery;
    public static FCM FCM;

    static void init(@NonNull Resources resources) {
        Chat = new Chat(
                resources.getString(R.string.preferences_chat_name_key),
                resources.getString(R.string.preferences_chat_channel_key),
                resources.getString(R.string.preferences_chat_showSense_key),
                resources.getString(R.string.preferences_chat_publicId_key),
                resources.getString(R.string.preferences_chat_showLinks_key),
                resources.getString(R.string.preferences_chat_katex_key),
                resources.getString(R.string.preferences_chat_delete_db_key),
                resources.getString(R.string.preferences_chat_colorful_messages_key),
                resources.getString(R.string.preferences_chat_db_max_search_result_key)
        );

        General = new General(
                resources.getString(R.string.preferences_general_bug_report_key),
                resources.getString(R.string.preferences_general_github_key),
                resources.getString(R.string.preferences_loggedIn_key),
                resources.getString(R.string.preferences_drawerSelection_key)
        );

        Gallery = new Gallery(
                resources.getString(R.string.preferences_gallery_delete_thumbnails_key),
                resources.getString(R.string.preferences_gallery_delete_images_key),
                resources.getString(R.string.preferences_gallery_offline_mode_key),
                resources.getString(R.string.preferences_gallery_show_dir_key),
                resources.getString(R.string.preferences_gallery_delete_db_key)
        );

        FCM = new FCM(
                resources.getString(R.string.preferences_ping_notification_key),
                resources.getString(R.string.preferences_ping_notification_server_key)
        );
    }

    public static final class Chat {
        public final String NAME;
        public final String CHANNEL;
        public final String SHOW_SENSE;
        public final String PUBLIC_ID;
        public final String SHOW_LINKS;
        public final String KATEX;
        public final String DELETE_CHAT_DB;
        public final String COLORFUL_MESSAGES;
        public final String DATABASE_MAX_SEARCH_RESULTS;

        private Chat(String NAME, String CHANNEL, String SHOW_SENSE, String PUBLIC_ID, String SHOW_LINKS, String KATEX, String DELETE_CHAT_DB, String COLORFUL_MESSAGES, String DATABASE_MAX_SEARCH_RESULTS) {
            this.NAME = NAME;
            this.CHANNEL = CHANNEL;
            this.SHOW_SENSE = SHOW_SENSE;
            this.PUBLIC_ID = PUBLIC_ID;
            this.SHOW_LINKS = SHOW_LINKS;
            this.KATEX = KATEX;
            this.DELETE_CHAT_DB = DELETE_CHAT_DB;
            this.COLORFUL_MESSAGES = COLORFUL_MESSAGES;
            this.DATABASE_MAX_SEARCH_RESULTS = DATABASE_MAX_SEARCH_RESULTS;
        }
    }
    public static final class General {
        public final String BUG_REPORT;
        public final String GITHUB;
        public final String LOGGED_IN;
        public final String DRAWER_SELECTION;

        private General(String BUG_REPORT, String GITHUB, String LOGGED_IN, String DRAWER_SELECTION) {
            this.BUG_REPORT = BUG_REPORT;
            this.GITHUB = GITHUB;
            this.LOGGED_IN = LOGGED_IN;
            this.DRAWER_SELECTION = DRAWER_SELECTION;
        }
    }
    public static final class Gallery {
        public final String DELETE_THUMBNAILS;
        public final String DELETE_IMAGES;
        public final String OFFLINE_MODE;
        public final String SHOW_GALLERY_DIR;
        public final String DELETE_GALLERY_DB;

        private Gallery(String DELETE_THUMBNAILS, String DELETE_IMAGES, String OFFLINE_MODE, String SHOW_GALLERY_DIR, String DELETE_GALLERY_DB) {
            this.DELETE_THUMBNAILS = DELETE_THUMBNAILS;
            this.DELETE_IMAGES = DELETE_IMAGES;
            this.OFFLINE_MODE = OFFLINE_MODE;
            this.SHOW_GALLERY_DIR = SHOW_GALLERY_DIR;
            this.DELETE_GALLERY_DB = DELETE_GALLERY_DB;
        }
    }
    public static final class FCM {
        public final String PING_NOTIFICATION;
        public final String PING_NOTIFICATION_SERVER;

        private FCM(String PING_NOTIFICATION, String PING_NOTIFICATION_SERVER) {
            this.PING_NOTIFICATION = PING_NOTIFICATION;
            this.PING_NOTIFICATION_SERVER = PING_NOTIFICATION_SERVER;
        }
    }
}
