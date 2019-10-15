package com.jonahbauer.qed.qedgallery;

import android.os.AsyncTask;

import com.jonahbauer.qed.Application;
import com.jonahbauer.qed.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;

@Deprecated
public class QEDGalleryImageInfo extends AsyncTask<Object, Void, String> {
    private QEDGalleryImageInfoReceiver receiver;
    private Image image;
    private SimpleDateFormat sdf;

    @Override
    protected String doInBackground(Object...objects) {
        if (objects.length < 2) return null;
        if (objects[0] instanceof QEDGalleryImageInfoReceiver) receiver = (QEDGalleryImageInfoReceiver) objects[0];
        else return null;

        if (objects[1] instanceof Image) image = (Image) objects[1];
        else return null;

        Application application = Application.getContext();

        String userid = application.loadData(Application.KEY_USERID, false);
        String pwhash = application.loadData(Application.KEY_GALLERY_PWHASH, true);
        String sessionId = application.loadData(Application.KEY_GALLERY_PHPSESSID, true);

        if (userid == null || pwhash == null || sessionId == null)
            return null;

        try {
            HttpsURLConnection httpsURLConnection = (HttpsURLConnection) new URL(Application.getContext().getString(R.string.gallery_server_image_info) + image.id).openConnection();
            httpsURLConnection.setRequestMethod("GET");
            httpsURLConnection.setRequestProperty("Cookie", "PHPSESSID=" + sessionId + "; pwhash=" + pwhash + "; userid=" + userid);
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
            receiver.onGalleryImageInfoReceived(null);
            return;
        }

        if (sdf == null) {
            sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.GERMANY);
        }

        Matcher matcher = Pattern.compile("<th>([^<]*):</th><td>([^<]*)</td>").matcher(string);
        while (matcher.find()) {
            switch (matcher.group(1)) {
                case "Besitzer":
                    image.owner = matcher.group(2);
                    break;
                case "Dateiformat":
                    image.format = matcher.group(2);
                    break;
                case "Hochgeladen am":
                    try {
                        image.uploadDate = sdf.parse(matcher.group(2));
                    } catch (ParseException ignored) {}
                    break;
            }
        }

        Matcher matcher2 = Pattern.compile("<div><b>([^<]*)</b></div>").matcher(string);
        if (matcher2.find()) {
            image.name = matcher2.group(1);
        }

        receiver.onGalleryImageInfoReceived(image);
    }
}
