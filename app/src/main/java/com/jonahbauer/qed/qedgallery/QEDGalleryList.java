package com.jonahbauer.qed.qedgallery;

import android.os.AsyncTask;

import com.jonahbauer.qed.Application;
import com.jonahbauer.qed.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;

public class QEDGalleryList extends AsyncTask<Object, Void, String> {
    private QEDGalleryListReceiver receiver;

    @Override
    protected String doInBackground(Object...objects) {
        char[] sessionId;
        char[] pwhash;
        char[] userid;

        if (objects.length < 4) return null;
        if (objects[0] instanceof QEDGalleryListReceiver) receiver = (QEDGalleryListReceiver) objects[0];
        else return null;

        if (objects[1] instanceof char[]) sessionId = (char[]) objects[1];
        else return null;

        if (objects[2] instanceof char[]) pwhash = (char[]) objects[2];
        else return null;

        if (objects[3] instanceof char[]) userid = (char[]) objects[3];
        else return null;

        try {
            HttpsURLConnection httpsURLConnection = (HttpsURLConnection) new URL(Application.getContext().getString(R.string.gallery_server_list)).openConnection();
            httpsURLConnection.setRequestMethod("GET");
            httpsURLConnection.setRequestProperty("Cookie", "PHPSESSID=" + String.valueOf(sessionId) + "; pwhash=" + String.valueOf(pwhash) + "; userid=" + String.valueOf(userid));
            httpsURLConnection.setUseCaches(false);
            httpsURLConnection.connect();

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
            receiver.onGalleryListReceived(null);
            return;
        }
        List<Gallery> galleries = new ArrayList<>();
        Matcher matcher = Pattern.compile("<li><a href='album_view\\.php\\?albumid=([\\d]*)&amp;page=1'>([^<]*)<\\/a><\\/li>").matcher(string);
        while (matcher.find()) {
            String id = matcher.group(1);
            String name = matcher.group(2);
            Gallery gallery = new Gallery();
            gallery.id = Integer.valueOf(id);
            gallery.name = name;
            galleries.add(gallery);
        }
        receiver.onGalleryListReceived(galleries);
    }
}
