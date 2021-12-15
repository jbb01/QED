package com.jonahbauer.qed.networking.cookies;

import android.content.SharedPreferences;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.CookieStore;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import lombok.experimental.UtilityClass;

@UtilityClass
public class QEDCookieHandler {
    private static CookieManager INSTANCE;

    private static final ReentrantLock lock = new ReentrantLock();
    private static final Condition ready = lock.newCondition();

    public void init(SharedPreferences sharedPreferences) {
        if (INSTANCE != null) {
            throw new IllegalStateException("cookie handler is already initialized");
        }

        CookieStore cookieStore = new SharedPreferencesCookieStore(sharedPreferences);
        CookiePolicy cookiePolicy = new QEDCookiePolicy();

        CookieManager manager = new CookieManager(cookieStore, cookiePolicy);

        CookieManager.setDefault(manager);

        lock.lock();
        try {
            INSTANCE = manager;
            ready.signalAll();
        } finally {
            lock.unlock();
        }
    }

    public void invalidate() {
        if (INSTANCE != null) INSTANCE.getCookieStore().removeAll();
    }

    public CookieManager getInstance() {
        await();
        return INSTANCE;
    }

    public void await() {
        if (INSTANCE != null) return;
        lock.lock();
        try {
            while (INSTANCE == null) {
                ready.await();
            }
        } catch (InterruptedException e) {
            var e2 = new IllegalStateException("cookie handler has not yet been initialized");
            e2.addSuppressed(e);
            throw e2;
        } finally {
            lock.unlock();
        }
    }
}
