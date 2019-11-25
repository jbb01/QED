package com.jonahbauer.qed.pingNotifications;

import android.content.Context;

import org.jetbrains.annotations.Contract;

public class PingNotifications {
    private final static PingNotifications instance;

    static {
        instance = new PingNotifications();
    }

    @Contract(pure = true)
    public static PingNotifications getInstance(@SuppressWarnings("unused") Context context) {
        return instance;
    }

    public void registrationStage0() {}
}
