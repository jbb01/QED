package com.jonahbauer.qed.networking.cookies;

import android.content.SharedPreferences;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.CookieStore;

import lombok.experimental.UtilityClass;

@UtilityClass
public class QEDCookieHandler {
    private static CookieManager INSTANCE;

    public void init(SharedPreferences sharedPreferences) {
        if (INSTANCE != null) {
            throw new IllegalStateException("cookie handler is already initialized");
        }

        CookieStore cookieStore = new SharedPreferencesCookieStore(sharedPreferences);
        CookiePolicy cookiePolicy = new QEDCookiePolicy();

        CookieManager manager = new CookieManager(cookieStore, cookiePolicy);

        CookieManager.setDefault(manager);

        INSTANCE = manager;
    }

    public void invalidate() {
        if (INSTANCE != null) INSTANCE.getCookieStore().removeAll();
    }

    public CookieManager getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException("cookie handler has not yet been initialized");
        } else {
            return INSTANCE;
        }
    }
}
