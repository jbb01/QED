package com.jonahbauer.qed.logs;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.jonahbauer.qed.Application;
import com.jonahbauer.qed.R;
import com.jonahbauer.qed.chat.Message;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.net.ssl.HttpsURLConnection;

public class LogGetter extends AsyncTask<Object, String, Boolean> {
    private final String KEY_USERID = Application.getContext().getString(R.string.data_userid_key);
    private final String KEY_PWHASH = Application.getContext().getString(R.string.data_pwhash_key);
    private String log;
    private LogReceiver receiver;
    private boolean oom;

    @Override
    protected Boolean doInBackground(Object... args) {
        String options;
        Context context;
        if (args.length<=2) return false;
        else {
            if (args[0] instanceof String) options = (String) args[0];
            else return false;

            if (args[1] instanceof LogReceiver) receiver = (LogReceiver) args[1];
            else return false;

            if (args[2] instanceof Context) context = (Context) args[2];
            else return false;
        }
        BufferedInputStream inLog = null;
        HttpsURLConnection logConnection = null;
        StringBuilder sbLog = null;
        try {
            URL url = new URL(context.getString(R.string.chat_server_history)+"?"+options);
            logConnection = (HttpsURLConnection) url.openConnection();

            logConnection.setDoInput(true);
            logConnection.setReadTimeout(0);
            logConnection.setConnectTimeout(2 * 1000);
            logConnection.setRequestMethod("GET");
            logConnection.setRequestProperty("Cookie", "pwhash=" + ((Application) Application.getContext()).loadData(KEY_PWHASH, true) + ";userid=" + ((Application) Application.getContext()).loadData(KEY_USERID, false));
            logConnection.connect();

            inLog = new BufferedInputStream(logConnection.getInputStream());

            sbLog = new StringBuilder();

            int x;

            while ((x = inLog.read()) != -1) {
                sbLog.append((char) x);
            }

            inLog.close();

            logConnection.disconnect();

            log = sbLog.toString();

            return true;
        } catch (IOException e) {
            Log.e("com.jonahbauer.qed", e.getMessage(), e);
        } catch (OutOfMemoryError e) {
            Log.e("com.jonahbauer.qed", e.getMessage(), e);
            oom = true;

            if (sbLog != null)
                log = sbLog.toString();

            return false;
        } finally {
            if (inLog != null) {
                try {
                    inLog.close();
                } catch (IOException ignored) {}
            }

            if (logConnection != null) {
                logConnection.disconnect();
            }
        }
        return false;
    }

    @Override
    protected void onPostExecute(final Boolean success) {
        if (oom) {
            receiver.onOutOfMemory();
        }

        if (log == null) {
            return;
        }

        List<Message> messages = Arrays.stream(log.split("\n")).parallel()
                .filter(str -> !str.contains("\"type\":\"ok\""))
                .map(Message::interpretJSONMessage)
                .filter(Objects::nonNull)
                .sorted((message, other) -> message != null ? message.compareTo(other) : 0)
                .collect(Collectors.toList());

        receiver.onReceiveLogs(messages);
    }

    @Override
    protected void onProgressUpdate(String... values) {
    }

    @Override
    protected void onCancelled() {
        receiver.onLogError();
    }
}