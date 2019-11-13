package com.jonahbauer.qed.networking;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;

import com.jonahbauer.qed.Application;
import com.jonahbauer.qed.R;
import com.jonahbauer.qed.qeddb.event.Event;
import com.jonahbauer.qed.qeddb.person.Person;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public abstract class QEDDBPages {
    private static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.GERMANY);
    private static final SimpleDateFormat simpleDateFormat2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.GERMANY);
    private static final SimpleDateFormat simpleDateFormat3 = new SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY);

    public static AsyncTask[] getEvent(String tag, String eventId, QEDPageReceiver<Event> eventReceiver) {
        Application application = Application.getContext();
        QEDPageReceiver<Event> receiver = new QEDPageReceiver<Event>() {
            private final Event event = new Event();
            private final AtomicInteger done = new AtomicInteger();

            @Override
            public void onPageReceived(String tag2, Event out) {
                if (tag2.equals("page1")) {
                    event.name = out.name;
                    event.start = out.start;
                    event.startString = out.startString;
                    event.end = out.end;
                    event.endString = out.endString;
                    event.cost = out.cost;
                    event.deadline = out.deadline;
                    event.deadlineString = out.deadlineString;
                    event.maxMember = out.maxMember;
                    event.organizer = out.organizer;
                } else if (tag2.equals("page2")) {
                    event.members = out.members;
                }

                if (done.incrementAndGet() == 2) {
                    done.set(0);
                    eventReceiver.onPageReceived(tag, event);
                }
            }

            @Override
            public void onNetworkError(String tag2) {
                eventReceiver.onNetworkError(tag);
            }
        };

        AsyncLoadQEDPage<Event> page1 = new AsyncLoadQEDPage<>(
                AsyncLoadQEDPage.Feature.DATABASE,
                String.format(application.getString(R.string.database_server_event), eventId, "1"),
                receiver,
                "page1",
                (String overview) -> {
                    Event event = new Event();
                    String[] tableRows = overview.split("(?:</tr><tr>)|(?:<tr>)|(?:</tr>)");
                    for (String row : tableRows) {
                        String[] params = row.split("(?:<div class=\' veranstaltung_)|(?: cell\' title=\'\'>)|(?:&nbsp;</div>)");
                        if (params.length > 2 && params[2] != null && !params[2].trim().equals("")) switch (params[1]) {
                            case "titel":
                                event.name = params[2];
                                break;
                            case "start":
                                try {
                                    event.start = simpleDateFormat.parse(params[2]);
                                } catch (ParseException ignored) {}
                                event.startString = params[2];
                                break;
                            case "ende":
                                try {
                                    event.end = simpleDateFormat.parse(params[2]);
                                } catch (ParseException ignored) {}
                                event.endString = params[2];
                                break;
                            case "kosten":
                                event.cost = params[2];
                                break;
                            case "anmeldeschluss":
                                try {
                                    event.deadline = simpleDateFormat2.parse(params[2]);
                                } catch (ParseException ignored) {}
                                event.deadlineString = params[2];
                                break;
                            case "max_anzahl_teilnehmer":
                                event.maxMember = params[2];
                                break;
                        }
                    }

                    event.organizer.clear();
                    for (String str : overview.split("</tr>")) {
                        Matcher matcher = Pattern.compile("<tr class=\"data\">.*").matcher(str);
                        while (matcher.find()) {
                            String data = matcher.group();
                            if (data.contains("rolle")) {
                                Person person = new Person();
                                Matcher matcher2 = Pattern.compile("<div class=\"[^\"]*\" title=\"([^\"]*)\">([^&]*)&nbsp;</div>").matcher(data);
                                while (matcher2.find()) {
                                    String title = matcher2.group(1);
                                    String value = matcher2.group(2);
                                    if (title != null) switch (title) {
                                        case "Vorname":
                                            person.firstName = value;
                                            break;
                                        case "Nachname":
                                            person.lastName = value;
                                            break;
                                    }
                                }
                                event.organizer.add(person);
                            }
                        }
                    }

                    return event;
                },
                new HashMap<>()
        );

        AsyncLoadQEDPage<Event> page2 = new AsyncLoadQEDPage<>(
                AsyncLoadQEDPage.Feature.DATABASE,
                String.format(application.getString(R.string.database_server_event), eventId, "2"),
                receiver,
                "page2",
                (String participants) -> {
                    Event event = new Event();
                    for (String str : participants.split("</tr>")) {
                        Matcher matcher = Pattern.compile("<tr class=\"data\">.*").matcher(str);
                        while (matcher.find()) {
                            String data = matcher.group();
                            if (data.contains("teilnehmer")) {
                                String type = null;

                                Person person = new Person();

                                Matcher matcher3 = Pattern.compile("person=(\\d*)").matcher(data);
                                if (matcher3.find()) {
                                    String id = matcher3.group(1);
                                    if (id != null) person.id = Integer.valueOf(id);
                                }

                                Matcher matcher2 = Pattern.compile("<div class=\"[^\"]*\" title=\"([^\"]*)\">([^&]*)&nbsp;</div>").matcher(data);
                                while (matcher2.find()) {
                                    String title = matcher2.group(1);
                                    String value = matcher2.group(2);
                                    if (title != null) switch (title) {
                                        case "Vorname":
                                            person.firstName = value;
                                            break;
                                        case "Nachname":
                                            person.lastName = value;
                                            break;
                                        case "Status":
                                            type = value;
                                            break;
                                        case "E-Mail":
                                            person.email = value;
                                            break;
                                    }
                                }

                                if (type != null) switch (type) {
                                    case "offen":
                                        person.type = Person.MemberType.MEMBER_OPEN;
                                        break;
                                    case "bestaetigt":
                                        person.type = Person.MemberType.MEMBER_CONFIRMED;
                                        break;
                                    case "abgemeldet":
                                        person.type = Person.MemberType.MEMBER_OPT_OUT;
                                        break;
                                    case "teilgenommen":
                                        person.type = Person.MemberType.MEMBER_PARTICIPATED;
                                        break;
                                }

                                for (Person organizer : event.organizer) {
                                    if ((organizer.firstName + organizer.lastName).equals(person.firstName + person.lastName)) {
                                        person.type = Person.MemberType.ORGA;
                                        break;
                                    }
                                }

                                event.members.add(person);
                            }
                        }
                    }
                    return event;
                },
                new HashMap<>()
        );

        page1.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void) null);
        page2.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void) null);

        return new AsyncTask[] {page1, page2};
    }

    public static AsyncTask[] getEvent(String tag, @NonNull Event event, QEDPageReceiver<Event> eventReceiver) {
        return getEvent(tag, event.id, eventReceiver);
    }

    public static AsyncTask[] getEventList(String tag, QEDPageReceiver<List<Event>> eventListReceiver) {
        Application application = Application.getContext();

        AsyncLoadQEDPage<List<Event>> list = new AsyncLoadQEDPage<>(
                AsyncLoadQEDPage.Feature.DATABASE,
                application.getString(R.string.database_server_events),
                eventListReceiver,
                tag,
                (String string) -> {
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
                                    if (title != null) switch (title) {
                                        case "Titel":
                                            event.name = value;
                                            break;
                                        case "Start":
                                            try {
                                                if (value != null) event.start = simpleDateFormat.parse(value);
                                            } catch (ParseException ignored) {}
                                            event.startString = value;
                                            break;
                                        case "Ende":
                                            try {
                                                if (value != null) event.end = simpleDateFormat.parse(value);
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
                                            if (value != null && !value.trim().equals("(keine)")) event.hotel = value;
                                            break;
                                    }
                                }
                                events.add(event);
                            }
                        }
                    }
                    return events;
                },
                new HashMap<>()
        );

        list.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void) null);

        return new AsyncTask[] {list};
    }

    public static AsyncTask[] getPerson(String tag, String personId, QEDPageReceiver<Person> personReceiver) {
        Application application = Application.getContext();
        QEDPageReceiver<Person> receiver = new QEDPageReceiver<Person>() {
            private final Person person = new Person();
            private final AtomicInteger done = new AtomicInteger();

            @Override
            public void onPageReceived(String tag2, Person out) {
                switch (tag2) {
                    case "page1":
                        person.firstName = out.firstName;
                        person.lastName = out.lastName;
                        person.email = out.email;
                        person.birthday = out.birthday;
                        person.homeStation = out.homeStation;
                        person.railcard = out.railcard;
                        person.memberSince = out.memberSince;
                        person.active = out.active;
                        person.member = out.member;
                        person.id = Integer.parseInt(personId);
                        break;
                    case "page2":
                        person.addresses = out.addresses;
                        person.phoneNumbers = out.phoneNumbers;
                        break;
                    case "page3":
                        person.events = out.events;
                        person.management = out.management;
                        break;
                }

                if (done.incrementAndGet() == 3) {
                    done.set(0);
                    personReceiver.onPageReceived(tag, person);
                }
            }

            @Override
            public void onNetworkError(String tag2) {
                personReceiver.onNetworkError(tag);
            }
        };

        final boolean isSelf = application.loadData(Application.KEY_USERID, false).equals(personId);

        AsyncLoadQEDPage<Person> page1 = new AsyncLoadQEDPage<>(
                AsyncLoadQEDPage.Feature.DATABASE,
                String.format(application.getString(R.string.database_server_person), personId, "1"),
                receiver,
                "page1",
                (String overview) -> {
                    Person person = new Person();
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
                    return person;
                },
                new HashMap<>()
        );


        AsyncLoadQEDPage<Person> page2 = new AsyncLoadQEDPage<>(
                AsyncLoadQEDPage.Feature.DATABASE,
                String.format(application.getString(R.string.database_server_person), personId, "2"),
                receiver,
                "page2",
                (String details) -> {
                    Person person = new Person();
                    for (String str : details.split("</tr>")) {
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
                                    if (title != null) switch (title) {
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
                                    if (title != null) switch (title) {
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
                    return person;
                },
                new HashMap<>()
        );


        AsyncLoadQEDPage<Person> page3 = new AsyncLoadQEDPage<>(
                AsyncLoadQEDPage.Feature.DATABASE,
                String.format(application.getString(R.string.database_server_person), personId, "3"),
                receiver,
                "page3",
                (String events) -> {
                    Person person = new Person();
                    for (String str : events.split("</tr>")) {
                        Matcher matcher = Pattern.compile("<tr class=\"data\">.*").matcher(str);
                        while (matcher.find()) {
                            String data = matcher.group();
                            if (data.contains("rollen")) {
                                String roll = null, eventTitle = null, start = null, end = null;
                                Matcher matcher2 = Pattern.compile("<div class=\"[^\"]*\" title=\"([^\"]*)\">([^&]*)&nbsp;</div>").matcher(data);
                                while (matcher2.find()) {
                                    String title = matcher2.group(1);
                                    String value = matcher2.group(2);
                                    if (title != null) switch (title) {
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
                                    if (start != null) event.start = simpleDateFormat3.parse(start);
                                } catch (ParseException ignored) {}
                                event.startString = start;
                                try {
                                    if (end != null) event.end = simpleDateFormat3.parse(end);
                                } catch (ParseException ignored) {}
                                event.endString = end;

                                person.events.add(new Pair<>(event, roll));
                            } else if (data.contains("funktion")) {
                                String roll = null, start = null, end = null;
                                Matcher matcher2 = Pattern.compile("<div class=\"[^\"]*\" title=\"([^\"]*)\">([^&]*)&nbsp;</div>").matcher(data);
                                while (matcher2.find()) {
                                    String title = matcher2.group(1);
                                    String value = matcher2.group(2);
                                    if(title != null) switch (title) {
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
                    return person;
                },
                new HashMap<>()
        );


        page1.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void) null);
        page2.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void) null);
        page3.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void) null);

        return new AsyncTask[] {page1, page2, page3};
    }

    public static AsyncTask[] getPersonList(String tag, QEDPageReceiver<List<Person>> personListReceiver) {
        Application application = Application.getContext();

        AsyncLoadQEDPage<List<Person>> list = new AsyncLoadQEDPage<>(
                AsyncLoadQEDPage.Feature.DATABASE,
                application.getString(R.string.database_server_persons),
                personListReceiver,
                tag,
                (String string) -> {
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
                                    String title = m3.group(1);
                                    String value = m2.group(2);
                                    if (title != null) switch (title) {
                                        case "Vorname":
                                            person.firstName = value;
                                            break;
                                        case "Nachname":
                                            person.lastName = value;
                                            break;
                                        case "Emailadresse":
                                            person.email = value;
                                            break;
                                        case "Aktiv":
                                            person.active = "aktiv".equals(value);
                                            break;
                                        case "Geburtstag":
                                            person.birthday = value;
                                            break;
                                        case "Mitglied seit":
                                            person.memberSince = value;
                                            break;
                                        case "Aktuell Mitglied":
                                            person.member = "Ja".equals(value);
                                            break;
                                    }
                                }
                            }
                            persons.add(person);
                        }
                    }

                    return persons;
                },
                new HashMap<>()
        );

        list.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void) null);

        return new AsyncTask[] {list};
    }
}
