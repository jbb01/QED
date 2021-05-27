package com.jonahbauer.qed;

import android.content.res.Resources;

import androidx.annotation.NonNull;

public final class Pref {
    public static FCM FCM;

    static void init(@NonNull Resources resources) {
        FCM = new FCM(
                resources.getString(R.string.preferences_ping_notification_key),
                resources.getString(R.string.preferences_ping_notification_server_key)
        );
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
