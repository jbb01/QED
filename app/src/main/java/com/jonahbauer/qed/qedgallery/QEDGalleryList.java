package com.jonahbauer.qed.qedgallery;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

import com.jonahbauer.qed.Application;
import com.jonahbauer.qed.R;
import com.jonahbauer.qed.activities.LoginActivity;
import com.jonahbauer.qed.networking.login.InvalidCredentialsException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;

@Deprecated
public class QEDGalleryList extends AsyncTask<QEDGalleryListReceiver, Void, String> {
    private QEDGalleryListReceiver receiver;

    @Override
    protected String doInBackground(QEDGalleryListReceiver...galleryListReceivers) {

        if (galleryListReceivers.length == 0) return null;
        receiver = galleryListReceivers[0];
        if (receiver == null) return null;

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


        try {
            HttpsURLConnection httpsURLConnection = (HttpsURLConnection) new URL(Application.getContext().getString(R.string.gallery_server_list)).openConnection();
            httpsURLConnection.setInstanceFollowRedirects(false);
            httpsURLConnection.setRequestMethod("GET");
            httpsURLConnection.setRequestProperty("Cookie", "PHPSESSID=" + sessionId + "; pwhash=" + pwhash + "; userid=" + userid);
            httpsURLConnection.setUseCaches(false);
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

                        httpsURLConnection = (HttpsURLConnection) new URL(Application.getContext().getString(R.string.gallery_server_list)).openConnection();
                        httpsURLConnection.setInstanceFollowRedirects(false);
                        httpsURLConnection.setRequestMethod("GET");
                        httpsURLConnection.setRequestProperty("Cookie", "PHPSESSID=" + sessionId + "; pwhash=" + pwhash + "; userid=" + userid);
                        httpsURLConnection.setUseCaches(false);
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
        } catch (IOException ignored) {}
        return null;
    }

    @Override
    protected void onPostExecute(String string) {
        if (string == null) {
            receiver.onAlbumListReceived(null);
            return;
        }
        List<Album> galleries = new ArrayList<>();
        Matcher matcher = Pattern.compile("<li><a href='album_view\\.php\\?albumid=([\\d]*)&amp;page=1'>([^<]*)</a></li>").matcher(string);
        while (matcher.find()) {
            String id = matcher.group(1);
            String name = matcher.group(2);
            Album album = new Album();
            album.id = Integer.valueOf(id);
            album.name = name;
            galleries.add(album);
        }
        receiver.onAlbumListReceived(galleries);
    }

    private void handleLoginError(InvalidCredentialsException e) {
        Context context;
        if (receiver instanceof Context) context = (Context) receiver;
        else context = Application.getContext();

        Intent intent = new Intent(context, LoginActivity.class);
        context.startActivity(intent);
        if (receiver instanceof Activity) ((Activity) receiver).finish();

        Log.e(Application.LOG_TAG_ERROR, e.getMessage(), e);
    }
}
