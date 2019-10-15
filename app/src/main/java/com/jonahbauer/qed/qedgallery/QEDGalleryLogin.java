package com.jonahbauer.qed.qedgallery;

import android.os.AsyncTask;
import android.util.Log;

import com.jonahbauer.qed.Application;
import com.jonahbauer.qed.R;
import com.jonahbauer.qed.networking.login.InvalidCredentialsException;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

@Deprecated
public class QEDGalleryLogin extends AsyncTask<QEDGalleryLoginReceiver, Void, Void> {
    @Override
    protected Void doInBackground(QEDGalleryLoginReceiver... receivers) {
        QEDGalleryLoginReceiver receiver;
        if (receivers.length < 1) return null;
        receiver = receivers[0];

        if (tryLogin()) {
            receiver.onLoginFinish(true);
            return null;
        }

        if (receiver != null) try {
            receiver.onLoginFinish(login());
        } catch (InvalidCredentialsException e) {
            receiver.onLoginError();
        }

        return null;
    }

    private boolean tryLogin() {
        Application application = Application.getContext();

        String phpsessid = application.loadData(Application.KEY_GALLERY_PHPSESSID, true);
        String pwhash = application.loadData(Application.KEY_GALLERY_PWHASH, true);
        String userId = application.loadData(Application.KEY_USERID, false);

        if (phpsessid == null || pwhash == null || userId == null) return false;

        try {
            HttpsURLConnection httpsURLConnection = (HttpsURLConnection) new URL(application.getString(R.string.gallery_server_main)).openConnection();
            httpsURLConnection.setRequestMethod("POST");
            httpsURLConnection.setDoInput(true);
            httpsURLConnection.setInstanceFollowRedirects(false);
            httpsURLConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            httpsURLConnection.setRequestProperty("charset", "utf-8");
            httpsURLConnection.setRequestProperty("Cookie", "userid=" + userId + "; pwhash=" + pwhash + "; PHPSESSID=" + phpsessid);
            httpsURLConnection.setUseCaches(false);

            httpsURLConnection.connect();

            String location = httpsURLConnection.getHeaderField("Location");

            if (location == null)
                return false;
            else if (location.equals("album_list.php"))
                return true;
            else if (location.equals("account.php"))
                return false;
            else
                return false;
        } catch (IOException e) {
            Log.e(Application.LOG_TAG_ERROR, e.getMessage(), e);
        }

        return false;
    }

    public static boolean login() throws InvalidCredentialsException {
        try {
            Application application = Application.getContext();
            String username = application.loadData(Application.KEY_USERNAME, false);
            String password = application.loadData(Application.KEY_PASSWORD, true);

            byte[] data = ("username=" + username + "&password=" + password + "&login=Einloggen").getBytes(StandardCharsets.UTF_8);

            String phpsessid = null;
            String userid = null;
            String pwhash = null;

            HttpsURLConnection httpsURLConnection = (HttpsURLConnection) new URL(application.getString(R.string.gallery_server_login)).openConnection();
            httpsURLConnection.setRequestMethod("POST");
            httpsURLConnection.setDoOutput(true);
            httpsURLConnection.setDoInput(true);
            httpsURLConnection.setInstanceFollowRedirects(false);
            httpsURLConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            httpsURLConnection.setRequestProperty("charset", "utf-8");
            httpsURLConnection.setRequestProperty("Content-Length", Integer.toString(data.length));
            httpsURLConnection.setUseCaches(false);

            DataOutputStream dataOutputStream = new DataOutputStream(httpsURLConnection.getOutputStream());
            dataOutputStream.write(data);

            httpsURLConnection.connect();

            List<String> cookies = httpsURLConnection.getHeaderFields().get("Set-Cookie");

            httpsURLConnection.disconnect();


            if (cookies == null) {
                return false;
            }

            for (String str : cookies) {
                if (str.startsWith("PHPSESSID=")) phpsessid = str.split("=")[1].split(";")[0];
                else if (str.startsWith("userid=")) userid = str.split("=")[1].split(";")[0];
                else if (str.startsWith("pwhash=")) pwhash = str.split("=")[1].split(";")[0];
            }

            if (phpsessid == null || userid == null || pwhash == null) {
                throw new InvalidCredentialsException();
            }

            application.saveData(pwhash, Application.KEY_GALLERY_PWHASH, true);
            application.saveData(phpsessid, Application.KEY_GALLERY_PHPSESSID, true);
            application.saveData(userid, Application.KEY_USERID, false);

            return true;
        } catch (java.io.IOException e) {
            Log.e(Application.LOG_TAG_ERROR,e.getMessage(), e);
            return false;
        }
    }
}
