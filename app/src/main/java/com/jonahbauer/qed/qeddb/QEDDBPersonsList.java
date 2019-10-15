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

public class QEDDBPersonsList extends AsyncTask<Object, Void, String> {
    private QEDDBPersonsListReceiver receiver;

    @Override
    protected String doInBackground(Object...objects) {
        char[] sessionId;
        char[] cookie;
        if (objects.length < 3) return null;
        if (objects[0] instanceof QEDDBPersonsListReceiver) receiver = (QEDDBPersonsListReceiver) objects[0];
        else return null;

        if (objects[1] instanceof char[]) sessionId = (char[]) objects[1];
        else return null;

        if (objects[2] instanceof char[]) cookie = (char[]) objects[2];
        else return null;

        try {
            HttpsURLConnection httpsURLConnection = (HttpsURLConnection) new URL(String.format(Application.getContext().getString(R.string.database_server_persons), String.valueOf(sessionId))).openConnection();
            httpsURLConnection.setRequestMethod("POST");
            httpsURLConnection.setDoOutput(true);
            httpsURLConnection.setInstanceFollowRedirects(false);
            httpsURLConnection.setRequestProperty("Cookie", String.valueOf(cookie));
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
            receiver.onPersonsListReceived(null);
            return;
        }
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

        receiver.onPersonsListReceived(persons);
    }
}
