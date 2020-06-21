package com.jonahbauer.qed.networking.login;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import com.jonahbauer.qed.Application;
import com.jonahbauer.qed.Pref;
import com.jonahbauer.qed.R;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.ref.SoftReference;
import java.net.URL;
import java.net.URLEncoder;

import javax.net.ssl.HttpsURLConnection;

/**
 * Represents an asynchronous login/registration task used to authenticate
 * the user.
 */
public class UserLoginTask extends AsyncTask<Void, Void, Boolean> {
    private final String mUsername;
    private final String mPassword;

    private final String mUrl;
    private final String mVersion;

    private final LoginCallback mCallback;

    public UserLoginTask(String username, String password, @NonNull Context context, @NonNull LoginCallback callback) {
        mUsername = username;
        mPassword = password;

        mUrl = context.getString(R.string.chat_server_login);
        mVersion = context.getString(R.string.chat_version);

        mCallback = callback;
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        try {
            URL url = new URL(mUrl);
            HttpsURLConnection logInConnection = (HttpsURLConnection) url.openConnection();

            logInConnection.setDoInput(true);
            logInConnection.setDoOutput(true);
            logInConnection.setReadTimeout(5 * 1000);
            logInConnection.setConnectTimeout(5 * 1000);
            logInConnection.setRequestMethod("POST");
            logInConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            logInConnection.setRequestProperty("charset", "utf-8");

            logInConnection.connect();

            String data = URLEncoder.encode("username", "UTF-8") + "=" + URLEncoder.encode(mUsername, "UTF-8");
            data += "&" + URLEncoder.encode("password", "UTF-8") + "=" + URLEncoder.encode(mPassword, "UTF-8");
            data += "&" + URLEncoder.encode("version", "UTF-8") + "=" + URLEncoder.encode(mVersion, "UTF-8");


            PrintWriter pw = new PrintWriter(logInConnection.getOutputStream());

            pw.write(data);
            pw.flush();
            pw.close();

            for (int i = 0; i < logInConnection.getHeaderFields().keySet().size(); i++) {
                if ("Set-Cookie".equalsIgnoreCase(logInConnection.getHeaderFieldKey(i))) {
                    String header = logInConnection.getHeaderField(i);

                    SoftReference<Application> applicationRef = Application.getApplicationReference();
                    Application application = applicationRef.get();

                    if (application != null) {
                        if (header.startsWith("userid"))
                            application.saveData(String.valueOf(Integer.valueOf(header.split("=")[1].split(";")[0])), Application.KEY_USERID, false);
                        else if (header.startsWith("pwhash"))
                            application.saveData(header.split("=")[1].split(";")[0], Application.KEY_CHAT_PWHASH, true);
                    }
                }
            }

            BufferedInputStream in = new BufferedInputStream(logInConnection.getInputStream());
            StringBuffer sb;
            String returnString;

            int x;

            sb = new StringBuffer();

            while ((x = in.read()) != -1) {
                sb.append((char) x);
            }

            in.close();

            logInConnection.disconnect();

            returnString = sb.toString();
            String[] splitString = returnString.split("\"");

            if (logInConnection.getResponseCode() == 200 && splitString[3].equals("success")) {
                return true;
            }

        } catch (IOException e) {
            Log.e(Application.LOG_TAG_ERROR, e.getMessage(), e);
            return null;
        }

        return false;
    }

    @Override
    protected void onPostExecute(@Nullable final Boolean success) {
        if (success != null && success) {
            SoftReference<Application> applicationRef = Application.getApplicationReference();
            Application application = applicationRef.get();

            if (application != null) {
                application.saveData(mUsername, Application.KEY_USERNAME, false);
                application.saveData(mPassword, Application.KEY_PASSWORD, true);

                PreferenceManager.getDefaultSharedPreferences(application).edit().putBoolean(Pref.General.LOGGED_IN, true).apply();
            }
        }

        mCallback.onResult(success);
    }

    @Override
    protected void onCancelled() {
        mCallback.onCancelled();
    }

    public interface LoginCallback {
        /**
         * Called after login has finished.
         *
         * @param success true if login was successful, false if not, null if an error occurred
         */
        void onResult(Boolean success);
        void onCancelled();
    }
}
