package com.jonahbauer.qed.qedgallery;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.jonahbauer.qed.Application;
import com.jonahbauer.qed.R;
import com.jonahbauer.qed.activities.LoginActivity;
import com.jonahbauer.qed.networking.login.InvalidCredentialsException;
import com.jonahbauer.qed.qeddb.person.Person;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;

@Deprecated
public class QEDGalleryAlbum extends AsyncTask<Object, Void, String> {
    private QEDGalleryAlbumReceiver receiver;
    private Album album;
    private boolean usedFilters = false;

    @SuppressWarnings("unchecked")
    @Override
    protected String doInBackground(Object...objects) {
        EnumSet<Filter> filters;

        if (objects.length < 3) return null;
        if (objects[0] instanceof QEDGalleryAlbumReceiver) receiver = (QEDGalleryAlbumReceiver) objects[0];
        else return null;

        if (objects[1] instanceof Album) album = (Album) objects[1];
        else return null;

        if (objects[2] instanceof EnumSet) filters = (EnumSet) objects[2];
        else return null;

        Application application = Application.getContext();

        String userid = application.loadData(Application.KEY_USERID, false);
        String pwhash = application.loadData(Application.KEY_GALLERY_PWHASH, true);
        String sessionId = application.loadData(Application.KEY_GALLERY_PHPSESSID, true);

        if (pwhash == null || sessionId == null || pwhash.equals("") || sessionId.equals("")) {
            try {
                QEDGalleryLogin.login();
            } catch (InvalidCredentialsException e) {
                handleLoginError(e);
                return null;
            }
            userid = application.loadData(Application.KEY_USERID, false);
            pwhash = application.loadData(Application.KEY_GALLERY_PWHASH, true);
            sessionId = application.loadData(Application.KEY_GALLERY_PHPSESSID, true);
        }

        StringBuilder filterStringBuilder = new StringBuilder().append("");
        if (!filters.isEmpty()) {
            if (objects.length < 7) return null;
            usedFilters = true;

            Map<Filter, String> filterValues;
            if (objects[6] instanceof Map) filterValues = (Map) objects[6];
            else return null;

            for (Filter filter : filters) {
                filterStringBuilder.append(filter.query).append(filterValues.getOrDefault(filter,""));
            }
        }

        try {
            HttpsURLConnection httpsURLConnection = openConnection(sessionId, pwhash, userid, filterStringBuilder.toString());
            httpsURLConnection.connect();

            // if server trys to redirect to login page
            // -> login, if success: continue, if fail: switch to offline
            String location = httpsURLConnection.getHeaderField("Location");
            if (location != null && location.startsWith("account.php")) {
                httpsURLConnection.disconnect();
                try {
                    if (QEDGalleryLogin.login()) {
                        userid = application.loadData(Application.KEY_USERID, false);
                        pwhash = application.loadData(Application.KEY_GALLERY_PWHASH, true);
                        sessionId = application.loadData(Application.KEY_GALLERY_PHPSESSID, true);

                        httpsURLConnection = openConnection(sessionId, pwhash, userid, filterStringBuilder.toString());
                        httpsURLConnection.connect();

                        location = httpsURLConnection.getHeaderField("Location");

                        if (location != null && location.startsWith("account.php")) {
                            return null;
                        }
                    } else {
                        return null;
                    }
                } catch (InvalidCredentialsException e) {
                    handleLoginError(e);
                    return null;
                }
            }

            BufferedReader in = new BufferedReader(new InputStreamReader(httpsURLConnection.getInputStream()));
            String inputLine;
            StringBuilder builder = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                builder.append(inputLine);
            }
            in.close();

            httpsURLConnection.disconnect();

            return builder.toString();
        } catch (IOException e) {
            Log.e(Application.LOG_TAG_ERROR, e.getMessage(), e);
        }
        return null;
    }

    @Override
    protected void onPostExecute(String string) {
        Album album;
        if (usedFilters) album = new Album();
        else album = this.album;

        if (string == null) {
            receiver.onGalleryAlbumReceived(null);
            return;
        }

        Matcher matcher = Pattern.compile("image\\.php\\?imageid=(\\d*)&amp;type=thumbnail' alt='([^']*)'").matcher(string);
        while (matcher.find()) {
            String id = matcher.group(1);
            Image image = new Image();
            image.id = Integer.valueOf(id);
            image.album = this.album;
            image.name = matcher.group(2);
            album.images.add(image);
        }

        Matcher matcher1 = Pattern.compile("album_view\\.php\\?albumid=\\d*&amp;page=1&amp;byowner=(\\d*)'>Bilder von ([^<]*)").matcher(string);
        while (matcher1.find()) {
            Person person = new Person();
            person.id = Integer.valueOf(matcher1.group(1));
            person.firstName = matcher1.group(2);
            album.persons.add(person);
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.GERMANY);
        Matcher matcher2 = Pattern.compile("album_view\\.php\\?albumid=\\d*&amp;page=1&amp;byday=([\\d\\-]*)").matcher(string);
        while (matcher2.find()) {
            try {
                album.dates.add(sdf.parse(matcher2.group(1)));
            } catch (ParseException ignored) {}
        }

        Matcher matcher3 = Pattern.compile("album_view\\.php\\?albumid=\\d*&amp;page=1&amp;bycategory=([^']*)").matcher(string);
        while (matcher3.find()) {
            if (matcher3.group(1).equals(""))
                album.categories.add("Sonstige");
            else
                album.categories.add(Uri.decode(matcher3.group(1)));
        }

        Matcher matcher4 = Pattern.compile("<th>Albumersteller:</th><td>([^<]*)</td>").matcher(string);
        if (matcher4.find()) {
            album.owner = matcher4.group(1);
        }

        Matcher matcher5 = Pattern.compile("<th>Erstellt am:</th><td>([^<]*)</td>").matcher(string);
        if (matcher5.find()) {
            album.creationDate = matcher5.group(1);
        }

        receiver.onGalleryAlbumReceived(album);
    }

    private void handleLoginError(InvalidCredentialsException e) {
        Context context;
        if (receiver instanceof Context) context = (Context) receiver;
        else context = Application.getContext();

        Intent intent = new Intent(context, LoginActivity.class);
        intent.putExtra(LoginActivity.DONT_START_MAIN, true);
        context.startActivity(intent);
//        if (receiver instanceof Activity) ((Activity) receiver).finish();

        Log.e(Application.LOG_TAG_ERROR, e.getMessage(), e);
    }

    private HttpsURLConnection openConnection(String sessionId, String pwhash, String userid, String filter) throws IOException {
        HttpsURLConnection httpsURLConnection = (HttpsURLConnection) new URL(Application.getContext().getString(R.string.gallery_server_album) + album.id + filter).openConnection();
        httpsURLConnection.setInstanceFollowRedirects(false);
        httpsURLConnection.setRequestMethod("GET");
        httpsURLConnection.setRequestProperty("Cookie", "PHPSESSID=" + sessionId + "; pwhash=" + pwhash + "; userid=" + userid);
        httpsURLConnection.setUseCaches(false);
        return httpsURLConnection;
    }

    public enum Filter {
        BY_DATE("&byday="), BY_PERSON("&byowner="), BY_CATEGORY("&bycategory=");

        private String query;

        Filter(String query) {
            this.query = query;
        }
    }
}
