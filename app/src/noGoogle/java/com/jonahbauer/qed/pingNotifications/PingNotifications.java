package com.jonahbauer.qed.pingNotifications;

import android.content.Context;

public class PingNotifications {
    private static PingNotifications instance;

    static {
        instance = new PingNotifications();
    }

    public static PingNotifications getInstance(@SuppressWarnings("unused") Context context) {
        return instance;
    }

    public void registrationStage0() {}
}
