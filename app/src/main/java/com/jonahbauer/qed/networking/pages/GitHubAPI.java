package com.jonahbauer.qed.networking.pages;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;
import com.jonahbauer.qed.model.Release;
import com.jonahbauer.qed.networking.NetworkConstants;
import com.jonahbauer.qed.networking.NetworkUtils;
import lombok.experimental.UtilityClass;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.jsoup.HttpStatusException;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

@UtilityClass
public class GitHubAPI {
    public static final String URL_RELEASES = NetworkConstants.GIT_HUB_API + "/releases?per_page=%d&page=%d";
    public static final String URL_LATEST_RELEASE = NetworkConstants.GIT_HUB_API + "/releases/latest";
    public static final String CONTENT_TYPE_GITHUB = "application/vnd.github+json";

    /**
     * Queries the GitHub-API for the latest release.
     * @return the latest release
     * @throws JSONException if the api response could not be parsed
     * @throws IOException if an I/O error occurs
     */
    @WorkerThread
    public static @NonNull Release getLatestRelease() throws JSONException, IOException {
        var value = connectAndParse(URL_LATEST_RELEASE);
        if (value instanceof JSONObject) {
            return Release.parse((JSONObject) value);
        } else {
            throw new JSONException("Unexpected return type " + (value != null ? value.getClass() : null) + " from " + GitHubAPI.URL_LATEST_RELEASE);
        }
    }

    /**
     * Queries the GitHub-API for a list of releases. The list is sorted from most recent to oldest. Prereleases
     * are included.
     * @param pageSize the number of results per page
     * @param page the index of the page
     * @return a list of releases
     * @throws JSONException if the api response could not be parsed
     * @throws IOException if an I/O error occurs
     */
    @WorkerThread
    public static @NonNull List<Release> getAllReleases(int pageSize, int page) throws JSONException, IOException {
        var value = connectAndParse(String.format(Locale.ROOT, GitHubAPI.URL_RELEASES, pageSize, page));
        if (value instanceof JSONArray) {
            return Release.parseList((JSONArray) value);
        } else {
            throw new JSONException("Unexpected return type " + (value != null ? value.getClass() : null) + " from " + GitHubAPI.URL_RELEASES);
        }
    }

    /**
     * Connects to the specified url and parses the result as JSON.
     * @param url a GitHub API URL
     * @return the response as a {@link JSONObject}, {@link JSONArray}, {@link String}, {@link Boolean},
     * {@link Integer}, {@link Long}, {@link Double} or {@code null}.
     * @throws JSONException if the response could not be parsed as JSON
     * @throws IOException if an I/O error occurred or the response code was not 200.
     */
    @WorkerThread
    private static Object connectAndParse(String url) throws JSONException, IOException {
        var connection = (HttpsURLConnection) new URL(url).openConnection();
        connection.addRequestProperty("Accept", CONTENT_TYPE_GITHUB);
        connection.connect();

        if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
            throw new HttpStatusException("Unexpected response code", connection.getResponseCode(), url);
        }

        var content = NetworkUtils.readPage(connection);
        return new JSONTokener(content).nextValue();
    }
}
