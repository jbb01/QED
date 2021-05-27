package com.jonahbauer.qed.networking.cookies;

import android.content.SharedPreferences;

import org.json.JSONObject;

import java.lang.reflect.Field;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * A {@link CookieStore} that synchronizes the cookies with an instance of {@link SharedPreferences}.
 * <br>
 * This implementation is based on {@code java.net.InMemoryCookieStore}
 */
public final class SharedPreferencesCookieStore implements CookieStore {

    private final Map<URI, List<HttpCookie>> uriIndex;
    private final ReentrantLock lock;
    private final SharedPreferences sharedPreferences;

    public SharedPreferencesCookieStore(SharedPreferences sharedPreferences) {
        this.uriIndex = new HashMap<>();
        this.lock = new ReentrantLock(false);
        this.sharedPreferences = sharedPreferences;

        loadSharedPreferences();
    }

    /**
     * Add one cookie into cookie store.
     */
    public void add(URI uri, HttpCookie cookie) {
        // pre-condition : argument can't be null
        if (cookie == null) {
            throw new NullPointerException("cookie is null");
        }

        lock.lock();
        try {
            addIndex(uriIndex, getEffectiveURI(uri), cookie);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Get all cookies, which:
     *  1) given uri domain-matches with, or, associated with
     *     given uri when added to the cookie store.
     *  3) not expired.
     * See RFC 2965 sec. 3.3.4 for more detail.
     */
    public List<HttpCookie> get(URI uri) {
        // argument can't be null
        if (uri == null) {
            throw new NullPointerException("uri is null");
        }

        List<HttpCookie> cookies = new ArrayList<>();
        // BEGIN Android-changed: b/25897688 InMemoryCookieStore ignores scheme (http/https)
        lock.lock();
        try {
            // check domainIndex first
            getInternal1(cookies, uriIndex, uri.getHost());
            // check uriIndex then
            getInternal2(cookies, uriIndex, getEffectiveURI(uri));
        } finally {
            lock.unlock();
        }
        // END Android-changed: b/25897688 InMemoryCookieStore ignores scheme (http/https)
        return cookies;
    }

    /**
     * Get all cookies in cookie store, except those have expired
     */
    public List<HttpCookie> getCookies() {
        // BEGIN Android-changed: Remove cookieJar and domainIndex
        List<HttpCookie> rt = new ArrayList<>();

        lock.lock();
        try {
            for (Map.Entry<URI, List<HttpCookie>> entry : uriIndex.entrySet()) {
                boolean changed = false;
                Iterator<HttpCookie> it = entry.getValue().iterator();
                while (it.hasNext()) {
                    HttpCookie cookie = it.next();
                    if (cookie.hasExpired()) {
                        changed = true;
                        it.remove();
                    } else if (!rt.contains(cookie)) {
                        rt.add(cookie);
                    }
                }

                if (changed) {
                    updateSharedPreferences(entry.getKey());
                }
            }
        } finally {
            rt = Collections.unmodifiableList(rt);
            lock.unlock();
        }
        // END Android-changed: Remove cookieJar and domainIndex

        return rt;
    }

    /**
     * Get all URIs, which are associated with at least one cookie
     * of this cookie store.
     */
    public List<URI> getURIs() {
        lock.lock();
        try {
            List<URI> result = new ArrayList<>(uriIndex.keySet());
            result.remove(null);
            return Collections.unmodifiableList(result);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Remove a cookie from store
     */
    public boolean remove(URI uri, HttpCookie ck) {
        // argument can't be null
        if (ck == null) {
            throw new NullPointerException("cookie is null");
        }

        lock.lock();
        try {
            uri = getEffectiveURI(uri);
            if (uriIndex.get(uri) == null) {
                return false;
            } else {
                List<HttpCookie> cookies = uriIndex.get(uri);
                if (cookies != null) {
                    if (cookies.remove(ck)) {
                        updateSharedPreferences(uri);
                        return true;
                    } else {
                        return false;
                    }
                } else {
                    return false;
                }
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Remove all cookies in this cookie store.
     */
    public boolean removeAll() {
        lock.lock();
        boolean result;

        try {
            result = !uriIndex.isEmpty();
            uriIndex.clear();
            sharedPreferences.edit().clear().apply();
        } finally {
            lock.unlock();
        }

        return result;
    }


    /* ---------------- Private operations -------------- */


    /*
     * This is almost the same as HttpCookie.domainMatches except for
     * one difference: It won't reject cookies when the 'H' part of the
     * domain contains a dot ('.').
     * I.E.: RFC 2965 section 3.3.2 says that if host is x.y.domain.com
     * and the cookie domain is .domain.com, then it should be rejected.
     * However that's not how the real world works. Browsers don't reject and
     * some sites, like yahoo.com do actually expect these cookies to be
     * passed along.
     * And should be used for 'old' style cookies (aka Netscape type of cookies)
     */
    private boolean netscapeDomainMatches(String domain, String host) {
        if (domain == null || host == null) {
            return false;
        }

        // if there's no embedded dot in domain and domain is not .local
        boolean isLocalDomain = ".local".equalsIgnoreCase(domain);
        int embeddedDotInDomain = domain.indexOf('.');
        if (embeddedDotInDomain == 0) {
            embeddedDotInDomain = domain.indexOf('.', 1);
        }
        if (!isLocalDomain && (embeddedDotInDomain == -1 || embeddedDotInDomain == domain.length() - 1)) {
            return false;
        }

        // if the host name contains no dot and the domain name is .local
        int firstDotInHost = host.indexOf('.');
        if (firstDotInHost == -1 && isLocalDomain) {
            return true;
        }

        int domainLength = domain.length();
        int lengthDiff = host.length() - domainLength;
        if (lengthDiff == 0) {
            // if the host name and the domain name are just string-compare euqal
            return host.equalsIgnoreCase(domain);
        } else if (lengthDiff > 0) {
            // need to check H & D component
            String D = host.substring(lengthDiff);

            return (D.equalsIgnoreCase(domain));
        } else if (lengthDiff == -1) {
            // if domain is actually .host
            return (domain.charAt(0) == '.' &&
                    host.equalsIgnoreCase(domain.substring(1)));
        }

        return false;
    }

    private void getInternal1(List<HttpCookie> cookies, Map<URI, List<HttpCookie>> cookieIndex, String host) {
        // BEGIN Android-changed: b/25897688 InMemoryCookieStore ignores scheme (http/https)
        // Use a separate list to handle cookies that need to be removed so
        // that there is no conflict with iterators.
        ArrayList<HttpCookie> toRemove = new ArrayList<>();
        for (Map.Entry<URI, List<HttpCookie>> entry : cookieIndex.entrySet()) {
            boolean changed = false;

            List<HttpCookie> lst = entry.getValue();
            for (HttpCookie c : lst) {
                String domain = c.getDomain();
                if ((c.getVersion() == 0 && netscapeDomainMatches(domain, host)) ||
                        (c.getVersion() == 1 && HttpCookie.domainMatches(domain, host))) {

                    // the cookie still in main cookie store
                    if (!c.hasExpired()) {
                        // don't add twice
                        if (!cookies.contains(c)) {
                            cookies.add(c);
                            changed = true;
                        }
                    } else {
                        toRemove.add(c);
                        changed = true;
                    }
                }
            }
            // Clear up the cookies that need to be removed
            for (HttpCookie c : toRemove) {
                lst.remove(c);
            }

            toRemove.clear();

            if (changed) {
                updateSharedPreferences(entry.getKey());
            }
        }
        // END Android-changed: b/25897688 InMemoryCookieStore ignores scheme (http/https)
    }

    // @param cookies           [OUT] contains the found cookies
    // @param cookieIndex       the index
    // @param comparator        the prediction to decide whether or not
    //                          a cookie in index should be returned
    private void getInternal2(List<HttpCookie> cookies, Map<URI, List<HttpCookie>> cookieIndex, URI comparator) {
        // BEGIN Android-changed: b/25897688 InMemoryCookieStore ignores scheme (http/https)
        // Removed cookieJar
        for (URI index : cookieIndex.keySet()) {
            if ((index == comparator) || (index != null && comparator.compareTo(index) == 0)) {
                boolean changed = false;

                List<HttpCookie> indexedCookies = cookieIndex.get(index);
                // check the list of cookies associated with this domain
                if (indexedCookies != null) {
                    Iterator<HttpCookie> it = indexedCookies.iterator();
                    while (it.hasNext()) {
                        HttpCookie ck = it.next();
                        // the cookie still in main cookie store
                        if (!ck.hasExpired()) {
                            // don't add twice
                            if (!cookies.contains(ck)) {
                                changed = true;
                                cookies.add(ck);
                            }
                        } else {
                            changed = true;
                            it.remove();
                        }
                    }
                } // end of indexedCookies != null

                if (changed) {
                    updateSharedPreferences(index);
                }
            } // end of comparator.compareTo(index) == 0
        } // end of cookieIndex iteration
        // END Android-changed: b/25897688 InMemoryCookieStore ignores scheme (http/https)
    }

    // add 'cookie' indexed by 'index' into 'indexStore'
    private void addIndex(Map<URI, List<HttpCookie>> indexStore, URI index, HttpCookie cookie) {
        // Android-changed: "index" can be null. We only use the URI based
        // index on Android and we want to support null URIs. The underlying
        // store is a HashMap which will support null keys anyway.
        // if (index != null) {
        List<HttpCookie> cookies = indexStore.get(index);
        if (cookies != null) {
            // there may already have the same cookie, so remove it first
            cookies.remove(cookie);

            cookies.add(cookie);
        } else {
            cookies = new ArrayList<>();
            cookies.add(cookie);
            indexStore.put(index, cookies);
        }

        updateSharedPreferences(index);
    }


    //
    // for cookie purpose, the effective uri should only be http://host
    // the path will be taken into account when path-match algorithm applied
    //
    private URI getEffectiveURI(URI uri) {
        // Android-added: Fix NullPointerException
        if (uri == null) {
            return null;
        }
        try {
            return new URI("http",
                    uri.getHost(),
                    null,  // path component
                    null,  // query component
                    null   // fragment component
            );
        } catch (URISyntaxException ignored) {
            return uri;
        }
    }

    private void loadSharedPreferences() {
        lock.lock();
        try {
            // Load cookies from sharedpreferences
            sharedPreferences.getAll().forEach((index, cookies) -> {
                if (!(cookies instanceof Set<?>)) return;

                try {
                    URI uri = new URI(index);

                    ((Set<?>) cookies).stream()
                                      .filter(c -> c instanceof String)
                                      .map(c -> (String) c)
                                      .map(this::deserializeCookie)
                                      .filter(Objects::nonNull)
                                      .forEach(cookie -> addIndex(uriIndex, uri, cookie));
                } catch (Exception ignored) {}
            });

            // Store cookies to sharedpreferences
            SharedPreferences.Editor editor = sharedPreferences.edit().clear();

            uriIndex.forEach((index, cookies) -> {
                Set<String> serializedCookies = cookies.stream()
                                                       .map(this::serializeCookie)
                                                       .filter(Objects::nonNull)
                                                       .collect(Collectors.toSet());

                if (serializedCookies != null && !serializedCookies.isEmpty()) {
                    editor.putStringSet(index.toString(), serializedCookies);
                }
            });

            editor.apply();
        } finally {
            lock.unlock();
        }
    }

    private void updateSharedPreferences(URI index) {
        List<HttpCookie> cookies = uriIndex.get(index);

        Set<String> serializedCookies = null;

        if (cookies != null) {
            serializedCookies = cookies.stream()
                                       .map(this::serializeCookie)
                                       .filter(Objects::nonNull)
                                       .collect(Collectors.toSet());
        }


        lock.lock();
        try {
            if (serializedCookies != null && !serializedCookies.isEmpty()) {
                sharedPreferences.edit()
                                 .putStringSet(index.toString(), serializedCookies)
                                 .apply();
            } else {
                sharedPreferences.edit()
                                 .remove(index.toString())
                                 .apply();
            }
        } finally {
            lock.unlock();
        }
    }

    private String serializeCookie(HttpCookie cookie) {
        try {
            Field whenCreated = HttpCookie.class.getDeclaredField("whenCreated");
            whenCreated.setAccessible(true);

            JSONObject json = new JSONObject();
            json.put("comment", cookie.getComment());
            json.put("commenturl", cookie.getCommentURL());
            json.put("discard", cookie.getDiscard());
            json.put("domain", cookie.getDomain());
            json.put("whenCreated", whenCreated.getLong(cookie));
            json.put("httponly", cookie.isHttpOnly());
            json.put("max-age", cookie.getMaxAge());
            json.put("path", cookie.getPath());
            json.put("port", cookie.getPortlist());
            json.put("secure", cookie.getSecure());
            json.put("version", cookie.getVersion());
            json.put("name", cookie.getName());
            json.put("value", cookie.getValue());

            return json.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private HttpCookie deserializeCookie(String string) {
        try {
            Field whenCreated = HttpCookie.class.getDeclaredField("whenCreated");
            whenCreated.setAccessible(true);

            JSONObject json = new JSONObject(string);

            String name = json.getString("name");
            String value = json.getString("value");

            HttpCookie out = new HttpCookie(name, value);

            long maxAge = -1;

            long oldMaxAge = json.getLong("max-age");
            if (oldMaxAge != -1) {
                long expirationTime = json.getLong("whenCreated") + oldMaxAge * 1000;
                long creationTime = whenCreated.getLong(out);

                if (creationTime > expirationTime) {
                    return null;
                }

                maxAge = (expirationTime - creationTime) / 1000;
            }

            out.setMaxAge(maxAge);
            if (json.has("comment")) out.setComment(json.getString("comment"));
            if (json.has("commenturl")) out.setCommentURL(json.getString("commenturl"));
            if (json.has("discard")) out.setDiscard(json.getBoolean("discard"));
            if (json.has("domain")) out.setDomain(json.getString("domain"));
            if (json.has("httponly")) out.setHttpOnly(json.getBoolean("httponly"));
            if (json.has("path")) out.setPath(json.getString("path"));
            if (json.has("port")) out.setPortlist(json.getString("port"));
            if (json.has("secure")) out.setSecure(json.getBoolean("secure"));
            if (json.has("version")) out.setVersion(json.getInt("version"));

            return out;
        } catch (Exception e) {
            return null;
        }
    }
}
