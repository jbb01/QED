package com.jonahbauer.qed.qeddb;

import android.os.AsyncTask;

import androidx.core.util.Pair;

import com.jonahbauer.qed.Application;
import com.jonahbauer.qed.R;
import com.jonahbauer.qed.qeddb.event.Event;
import com.jonahbauer.qed.qeddb.person.Person;

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

/**
 * @deprecated
 * use {@link com.jonahbauer.qed.networking.AsyncLoadQEDPage} instead
 */
@Deprecated
public class QEDDBPerson extends AsyncTask<Object, Void, String[]> {
    private QEDDBPersonReceiver receiver;

    private boolean isSelf = false;

    private final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY);

    @Override
    protected String[] doInBackground(Object...objects) {
        String userId;

        if (objects.length < 2) return null;
        if (objects[0] instanceof QEDDBPersonReceiver) receiver = (QEDDBPersonReceiver) objects[0];
        else return null;

        if (objects[1] instanceof String) userId = (String) objects[1];
        else return null;

        Application application = Application.getContext();
        if (userId.equals(application.loadData(Application.KEY_USERID,false))) isSelf = true;

        String sessionId = application.loadData(Application.KEY_DATABASE_SESSIONID, true);
        String sessionId2 = application.loadData(Application.KEY_DATABASE_SESSIONID2, true);
        if (sessionId == null || sessionId2 == null) return null;

        String[] result = new String[3];
        for (int i = 1 ; i <= 3; i++) {
            try {
                HttpsURLConnection httpsURLConnection = (HttpsURLConnection) new URL(String.format(Application.getContext().getString(R.string.database_server_person), sessionId2, userId, String.valueOf(i))).openConnection();
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

                result[i - 1] = builder.toString();
            } catch (IOException e) {
                return null;
            }
        }
        return result;
    }

    @Override
    protected void onPostExecute(String[] string) {
        if (string == null) {
            receiver.onPersonReceived(null);
            return;
        }

        Person person = new Person();

        String overview = string[0];

        String[] tableRows = overview.split("(?:</tr><tr>)|(?:<tr>)|(?:</tr>)");


        for (String row : tableRows) {
            String[] params = row.split("(?:<div class=\' person_)|(?: cell\' title=\'\'>)|(?:&nbsp;</div>)");
            if (params.length > 2 && params[2] != null && !params[2].trim().equals("")) switch (params[1]) {
                case "nachname":
                    person.lastName = params[2];
                    break;
                case "vorname":
                    person.firstName = params[2];
                    break;
                case "sichtbare_email":
                    person.email = params[2];
                    break;
                case "sichtbarer_geburtstag":
                    person.birthday = params[2];
                    break;
                case "heimatbahnhof":
                    if (!isSelf)
                        person.homeStation = params[2];
                    else {
                        Matcher matcher = Pattern.compile(".*value=\"(.*)\"").matcher(params[2]);
                        if (matcher.find())
                            person.homeStation = matcher.group(1);
                    }
                    break;
                case "bahncard":
                    person.railcard = params[2];
                    break;
                case "mitglied_seit":
                    person.memberSince = params[2];
                    break;
                case "mitglied":
                    person.member = params[2].equals("Ja");
                    break;
                case "aktiv":
                    person.active = params[2].equals("1");
                    break;
            }
        }

        for (String str : string[1].split("</tr>")) {
            if (isSelf) str = str.replaceAll("<div class=\"noedit\">", "");
            Matcher matcher = Pattern.compile("<tr class=\"data\">.*").matcher(str);
            while (matcher.find()) {
                String data = matcher.group();
                if (data.contains("adresse")) {
                    String street = null, number = null, additions = null, zip = null, city = null, country = null;
                    Matcher matcher2 = Pattern.compile("<div class=\"[^\"]*\" title=\"([^\"]*)\">([^&]*)&nbsp;</div>").matcher(data);
                    while (matcher2.find()) {
                        String title = matcher2.group(1);
                        String value = matcher2.group(2);
                        switch (title) {
                            case "Strasse":
                                street = value;
                                break;
                            case "Nummer":
                                number = value;
                                break;
                            case "Adresszusatz":
                                additions = value;
                                break;
                            case "PLZ":
                                zip = value;
                                break;
                            case "Ort":
                                city = value;
                                break;
                            case "Land":
                                country = value;
                                break;
                        }
                    }
                    String address = "";
                    if (street != null && !street.trim().equals("")) address += street;
                    if (number != null && !number.trim().equals("")) address += " " + number;
                    if (additions != null && !additions.trim().equals("")) address += "\n" + additions;
                    if (zip != null && !zip.trim().equals("")) address += "\n" + zip;
                    if (city != null && !city.trim().equals("")) address += ((zip != null && !zip.trim().equals("")) ? " " : "\n") + city;
                    if (country != null && !country.trim().equals("")) address += "\n" + country;

                    person.addresses.add(address);
                } else if (data.contains("telefon")) {
                    String type = null, number = null;
                    Matcher matcher2 = Pattern.compile("<div class=\"[^\"]*\" title=\"([^\"]*)\">([^&]*)&nbsp;</div>").matcher(data);
                    while (matcher2.find()) {
                        String title = matcher2.group(1);
                        String value = matcher2.group(2);
                        switch (title) {
                            case "Art":
                                type = value;
                                break;
                            case "Nummer":
                                number = value;
                                break;
                        }
                    }

                    person.phoneNumbers.add(new Pair<>(type, number));
                }
            }
        }


        for (String str : string[2].split("</tr>")) {
            Matcher matcher = Pattern.compile("<tr class=\"data\">.*").matcher(str);
            while (matcher.find()) {
                String data = matcher.group();
                if (data.contains("rollen")) {
                    String roll = null, eventTitle = null, start = null, end = null;
                    Matcher matcher2 = Pattern.compile("<div class=\"[^\"]*\" title=\"([^\"]*)\">([^&]*)&nbsp;</div>").matcher(data);
                    while (matcher2.find()) {
                        String title = matcher2.group(1);
                        String value = matcher2.group(2);
                        switch (title) {
                            case "Rolle":
                                roll = value;
                                break;
                            case "Titel":
                                eventTitle = value;
                                break;
                            case "Start":
                                start = value;
                                break;
                            case "Ende":
                                end = value;
                                break;
                        }
                    }
                    Event event = new Event();
                    event.name = eventTitle;
                    try {
                        event.start = simpleDateFormat.parse(start);
                    } catch (ParseException ignored) {}
                    event.startString = start;
                    try {
                        event.end = simpleDateFormat.parse(end);
                    } catch (ParseException ignored) {}
                    event.endString = end;

                    person.events.add(new Pair<>(event, roll));
                } else if (data.contains("funktion")) {
                    String roll = null, start = null, end = null;
                    Matcher matcher2 = Pattern.compile("<div class=\"[^\"]*\" title=\"([^\"]*)\">([^&]*)&nbsp;</div>").matcher(data);
                    while (matcher2.find()) {
                        String title = matcher2.group(1);
                        String value = matcher2.group(2);
                        switch (title) {
                            case "Rolle":
                                roll = value;
                                break;
                            case "Start":
                                start = value;
                                break;
                            case "Ende":
                                end = value;
                                break;
                        }
                    }

                    person.management.add(new Pair<>(roll, new Pair<>(start, end)));

                }
            }
        }

        receiver.onPersonReceived(person);
    }
}
