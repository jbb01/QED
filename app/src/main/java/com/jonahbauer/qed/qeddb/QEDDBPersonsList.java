package com.jonahbauer.qed.qeddb;

import android.os.AsyncTask;

import com.jonahbauer.qed.Application;
import com.jonahbauer.qed.R;
import com.jonahbauer.qed.qeddb.person.Person;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;

/**
 * @deprecated
 * use {@link com.jonahbauer.qed.networking.AsyncLoadQEDPage} instead
 */
@Deprecated
public class QEDDBPersonsList extends AsyncTask<QEDDBPersonsListReceiver, Void, List<Person>> {
    private QEDDBPersonsListReceiver receiver;

    @Override
    protected List<Person> doInBackground(QEDDBPersonsListReceiver...qeddbPersonsListReceivers) {
        if (qeddbPersonsListReceivers.length == 0) return null;
        receiver = qeddbPersonsListReceivers[0];
        if (receiver == null) return null;

        Application application = Application.getContext();
        String sessionId = application.loadData(Application.KEY_DATABASE_SESSIONID, true);
        String sessionId2 = application.loadData(Application.KEY_DATABASE_SESSIONID2, true);
        if (sessionId == null || sessionId2 == null) return null;

        try {
            HttpsURLConnection httpsURLConnection = (HttpsURLConnection) new URL(String.format(Application.getContext().getString(R.string.database_server_persons), sessionId2)).openConnection();
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

            String string = builder.toString();

            List<Person> persons = new ArrayList<>();

            for (String str : string.split("</tr>")) {
                Matcher m = Pattern.compile("<tr class=\"data\">.*").matcher(str);
                while (m.find()) {
                    Person person = new Person();
                    String data = m.group();
                    Matcher m2 = Pattern.compile("<td id=\'personen_(\\d*)\'").matcher(data);
                    if (m2.find()) person.id = Integer.valueOf(m2.group(1));
                    for (String data2 : data.split("</div>")) {
                        Matcher m3 = Pattern.compile("<div class=\".*\" title=\"(.*)\">(.*)&nbsp;").matcher(data2);
                        while (m3.find()) {
                            switch (m3.group(1)) {
                                case "Vorname":
                                    person.firstName = m3.group(2);
                                    break;
                                case "Nachname":
                                    person.lastName = m3.group(2);
                                    break;
                                case "Emailadresse":
                                    person.email = m3.group(2);
                                    break;
                                case "Aktiv":
                                    person.active = m3.group(2).equals("aktiv");
                                    break;
                                case "Geburtstag":
                                    person.birthday = m3.group(2);
                                    break;
                                case "Mitglied seit":
                                    person.memberSince = m3.group(2);
                                    break;
                                case "Aktuell Mitglied":
                                    person.member = m3.group(2).equals("Ja");
                                    break;
                            }
                        }
                    }
                    persons.add(person);
                }
            }

            return persons;
        } catch (IOException ignored) {
            receiver.onPersonsListError();
            return null;
        }
    }

    @Override
    protected void onPostExecute(List<Person> persons) {
        receiver.onPersonsListReceived(persons);
    }
}
