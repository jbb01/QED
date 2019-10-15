package com.jonahbauer.qed.qeddb;

import android.os.AsyncTask;

import com.jonahbauer.qed.Application;
import com.jonahbauer.qed.R;
import com.jonahbauer.qed.qeddb.event.Event;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;

/**
 * @deprecated
 * use {@link com.jonahbauer.qed.networking.AsyncLoadQEDPage} instead
 */
@Deprecated
public class QEDDBEventsList extends AsyncTask<QEDDBEventsListReceiver, Void, String> {
    private QEDDBEventsListReceiver receiver;

    private final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.GERMANY);

    @Override
    protected String doInBackground(QEDDBEventsListReceiver...qeddbEventsListReceivers) {
        if (qeddbEventsListReceivers.length == 0) return null;
        receiver = qeddbEventsListReceivers[0];
        if (receiver == null) return null;

        Application application = Application.getContext();
        String sessionId = application.loadData(Application.KEY_DATABASE_SESSIONID, true);
        String sessionId2 = application.loadData(Application.KEY_DATABASE_SESSIONID2, true);
        if (sessionId == null || sessionId2 == null) return null;

        try {
            HttpsURLConnection httpsURLConnection = (HttpsURLConnection) new URL(String.format(Application.getContext().getString(R.string.database_server_events), sessionId2)).openConnection();
            httpsURLConnection.setRequestMethod("POST");
            httpsURLConnection.setDoOutput(true);
            httpsURLConnection.setInstanceFollowRedirects(false);
            httpsURLConnection.setRequestProperty("Cookie", sessionId);
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
            receiver.onEventsListReceived(null);
            return;
        }
        List<Event> events = new ArrayList<>();

        for (String str : string.split("</tr>")) {
            Matcher matcher = Pattern.compile("<tr class=\"data\">.*").matcher(str);
            while (matcher.find()) {
                String data = matcher.group();
                if (data.contains("veranstaltung")) {
                    Event event = new Event();
                    Matcher matcher2 = Pattern.compile("veranstaltungen_(\\d*)").matcher(data);
                    if (matcher2.find())
                        event.id = matcher2.group(1);
                    Matcher matcher3 = Pattern.compile("<div class=\"[^\"]*\" title=\"([^\"]*)\">([^&]*)&nbsp;</div>").matcher(data);
                    while (matcher3.find()) {
                        String title = matcher3.group(1);
                        String value = matcher3.group(2);
                        switch (title) {
                            case "Titel":
                                event.name = value;
                                break;
                            case "Start":
                                try {
                                    event.start = simpleDateFormat.parse(value);
                                } catch (ParseException ignored) {}
                                event.startString = value;
                                break;
                            case "Ende":
                                try {
                                    event.end = simpleDateFormat.parse(value);
                                } catch (ParseException ignored) {}
                                event.endString = value;
                                break;
                            case "Kosten":
                                event.cost = value;
                                break;
                            case "Anmeldeschluss":
                                event.deadlineString = value;
                                break;
                            case "Max. Teilnehmerzahl":
                                event.maxMember = value;
                                break;
                            case "&Uuml;bernachtung":
                                if (!value.trim().equals("(keine)")) event.hotel = value;
                                break;
                        }
                    }
                    events.add(event);
                }
            }
        }
        receiver.onEventsListReceived(events);
    }
}
