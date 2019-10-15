package com.jonahbauer.qed.qedgallery;

import android.os.AsyncTask;
import android.util.Log;

import com.jonahbauer.qed.Application;
import com.jonahbauer.qed.R;

import java.io.DataOutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

public class QEDGalleryLogin extends AsyncTask<QEDGalleryLoginReceiver, Void, Void> {


    @Override
    protected Void doInBackground(QEDGalleryLoginReceiver... receivers) {
        QEDGalleryLoginReceiver receiver;
        if (receivers.length < 1) return null;
        receiver = receivers[0];
        try {
            Application application = (Application) Application.getContext();
            String username = application.loadData(application.getString(R.string.data_username_key), false);
            String password = application.loadData(application.getString(R.string.data_password_key), true);

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


            if (cookies == null)
                receiver.onReceiveSessionId(null, null, null);

            for (String str : cookies) {
                if (str.startsWith("PHPSESSID=")) phpsessid = str.split("=")[1].split(";")[0];
                else if (str.startsWith("userid=")) userid = str.split("=")[1].split(";")[0];
                else if (str.startsWith("pwhash=")) pwhash = str.split("=")[1].split(";")[0];
            }

            if (phpsessid == null || userid == null || pwhash == null)
                receiver.onReceiveSessionId(null, null, null);

            receiver.onReceiveSessionId(phpsessid.toCharArray(), pwhash.toCharArray(), application.loadData(application.getString(R.string.data_userid_key), false).toCharArray());

        } catch (java.io.IOException e) {
            Log.e("","",e);
            receiver.onReceiveSessionId(null,null, null);
        }
        return null;
    }
}
