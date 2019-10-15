package com.jonahbauer.qed.networking.login;

import com.jonahbauer.qed.Application;
import com.jonahbauer.qed.R;
import com.jonahbauer.qed.networking.NoNetworkException;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

public abstract class QEDLogin {
    public static void loginChat() throws InvalidCredentialsException, NoNetworkException {
        try {
            Application application = Application.getContext();
            String username = application.loadData(Application.KEY_USERNAME, false);
            String password = application.loadData(Application.KEY_PASSWORD, true);
            String version = application.getString(R.string.chat_version);

            byte[] data = ("username=" + username + "&password=" + password + "&version=" + version).getBytes(StandardCharsets.UTF_8);

            String userid = null;
            String pwhash = null;

            HttpsURLConnection httpsURLConnection = (HttpsURLConnection) new URL(application.getString(R.string.chat_server_login)).openConnection();
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
            dataOutputStream.close();

            if (cookies == null) {
                throw new InvalidCredentialsException();
            }

            for (String str : cookies) {
                if (str.startsWith("userid=")) userid = str.split("=")[1].split(";")[0];
                else if (str.startsWith("pwhash=")) pwhash = str.split("=")[1].split(";")[0];
            }

            if (userid == null || pwhash == null)
                throw new InvalidCredentialsException();

            application.saveData(userid, Application.KEY_USERID, false);
            application.saveData(pwhash, Application.KEY_CHAT_PWHASH, true);
        } catch (IOException e) {
            throw new NoNetworkException(e);
        }
    }

    public static void loginDatabase() throws InvalidCredentialsException, NoNetworkException {
        try {
            Application application = Application.getContext();
            String username = application.loadData(Application.KEY_USERNAME, false);
            String password = application.loadData(Application.KEY_PASSWORD, true);

            byte[] data = ("username=" + username + "&password=" + password + "&anmelden=anmelden").getBytes(StandardCharsets.UTF_8);

            String sessionId;
            String sessionId2;
            String sessionCookie;

            HttpsURLConnection httpsURLConnection = (HttpsURLConnection) new URL(application.getString(R.string.database_server_login)).openConnection();
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

            String location = httpsURLConnection.getHeaderField("Location");
            String cookie = httpsURLConnection.getHeaderField("Set-Cookie");

            httpsURLConnection.disconnect();
            dataOutputStream.close();

            if (location == null || cookie == null) {
                throw new InvalidCredentialsException();
            }

            try {
                String[] tmp = cookie.split("=");
                sessionCookie = tmp[0];
                sessionId = tmp[1];
                sessionId2 = location.split("session_id2=")[1];
            } catch (ArrayIndexOutOfBoundsException e) {
                throw new InvalidCredentialsException();
            }

            if (sessionCookie == null || sessionId == null || sessionId2 == null)
                throw new InvalidCredentialsException();

            application.saveData(sessionCookie, Application.KEY_DATABASE_SESSION_COOKIE, true);
            application.saveData(sessionId, Application.KEY_DATABASE_SESSIONID, true);
            application.saveData(sessionId2, Application.KEY_DATABASE_SESSIONID2, true);
        } catch (IOException e) {
            throw new NoNetworkException(e);
        }
    }

    public static void loginGallery() throws InvalidCredentialsException, NoNetworkException {
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
            dataOutputStream.close();

            if (cookies == null)
                throw new InvalidCredentialsException();

            for (String str : cookies) {
                if (str.startsWith("PHPSESSID=")) phpsessid = str.split("=")[1].split(";")[0];
                else if (str.startsWith("userid=")) userid = str.split("=")[1].split(";")[0];
                else if (str.startsWith("pwhash=")) pwhash = str.split("=")[1].split(";")[0];
            }

            if (phpsessid == null || userid == null || pwhash == null)
                throw new InvalidCredentialsException();


            application.saveData(pwhash, Application.KEY_GALLERY_PWHASH, true);
            application.saveData(phpsessid, Application.KEY_GALLERY_PHPSESSID, true);
            application.saveData(userid, Application.KEY_USERID, false);
        } catch (java.io.IOException e) {
            throw new NoNetworkException(e);
        }
    }
}
