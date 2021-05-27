package com.jonahbauer.qed.networking.cookies;

import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.net.URI;

/**
 * A {@link CookiePolicy} that accepts only cookies for the host {@code qed-verein.de}.
 */
public final class QEDCookiePolicy implements CookiePolicy {
    private static final String ALLOWED_HOST = "qed-verein.de";

    @Override
    public boolean shouldAccept(URI uri, HttpCookie cookie) {
        String host = uri.getHost();

        return host.endsWith(ALLOWED_HOST);
    }
}
