package com.jonahbauer.qed.qeddb;

import android.os.AsyncTask;
import android.util.Log;

import com.jonahbauer.qed.Application;
import com.jonahbauer.qed.R;

import java.io.DataOutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import javax.net.ssl.HttpsURLConnection;

/**
 * @deprecated
 *
 * a manual login is no longer required. {@link com.jonahbauer.qed.networking.AsyncLoadQEDPage} handles logging in.
 */
@Deprecated
public class QEDDBLogin extends AsyncTask<QEDDBLoginReceiver, Void, Void> {


    @Override
    protected Void doInBackground(QEDDBLoginReceiver... receivers) {
        QEDDBLoginReceiver receiver;
        if (receivers.length < 1) return null;
        receiver = receivers[0];
        try {
            Application application = Application.getContext();
            String username = application.loadData(Application.KEY_USERNAME, false);
            String password = application.loadData(Application.KEY_PASSWORD, true);

            byte[] data = ("username=" + username + "&password=" + password + "&anmelden=anmelden").getBytes(StandardCharsets.UTF_8);

            HttpsURLConnection httpsURLConnection = (HttpsURLConnection) new URL(application.getString(R.string.database_server_login)).openConnection();
            httpsURLConnection.setRequestMethod("POST");
            httpsURLConnection.setDoOutput(true);
            httpsURLConnection.setInstanceFollowRedirects(false);
            httpsURLConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            httpsURLConnection.setRequestProperty("charset", "utf-8");
            httpsURLConnection.setRequestProperty("Content-Length", Integer.toString(data.length));
            httpsURLConnection.setUseCaches(false);

            DataOutputStream dataOutputStream = new DataOutputStream(httpsURLConnection.getOutputStream());
            dataOutputStream.write(data);

            httpsURLConnection.connect();

            String location = httpsURLConnection.getHeaderField("Location");
            String cookie = httpsURLConnection.getHeaderField("Set-Cookie");
            if (location == null)
                receiver.onLoginFinish(false);
            else {
                application.saveData(cookie, Application.KEY_DATABASE_SESSIONID, true);
                application.saveData(location.split("session_id2=")[1], Application.KEY_DATABASE_SESSIONID2, true);
                receiver.onLoginFinish(true);
            }
        } catch (java.io.IOException e) {
            Log.e(Application.LOG_TAG_ERROR,e.getMessage(), e);
            receiver.onLoginFinish(false);
        }
        return null;
    }
}
