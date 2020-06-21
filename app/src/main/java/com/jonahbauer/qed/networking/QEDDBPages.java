package com.jonahbauer.qed.networking;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;

import com.jonahbauer.qed.Application;
import com.jonahbauer.qed.R;
import com.jonahbauer.qed.qeddb.event.Event;
import com.jonahbauer.qed.qeddb.person.Person;
import com.jonahbauer.qed.util.Triple;

import java.io.IOException;
import java.io.StringReader;
import java.lang.ref.SoftReference;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public abstract class QEDDBPages {
    @Nullable
    public static AsyncTask<?,?,?>[] getEvent(String tag, long eventId, QEDPageReceiver<Event> eventReceiver) {
        SoftReference<Application> applicationReference = Application.getApplicationReference();
        Application application = applicationReference.get();

        if (application == null) {
            Log.e(Application.LOG_TAG_ERROR, "", new Exception("Application is null!"));
            return null;
        }

        QEDPageReceiver<Event> receiver = new QEDPageReceiver<Event>() {
            private final Event mEvent = new Event();
            private final AtomicInteger mDone = new AtomicInteger();

            @Override
            public void onPageReceived(String tag2, Event out) {
                if (tag2.equals("page1")) {
                    mEvent.title = out.title;
                    mEvent.start = out.start;
                    mEvent.startString = out.startString;
                    mEvent.end = out.end;
                    mEvent.endString = out.endString;
                    mEvent.cost = out.cost;
                    mEvent.deadline = out.deadline;
                    mEvent.deadlineString = out.deadlineString;
                    mEvent.maxParticipants = out.maxParticipants;
                    mEvent.organizers = out.organizers;
                } else if (tag2.equals("page2")) {
                    mEvent.participants = out.participants;
                }

                if (mDone.incrementAndGet() == 2) {
                    mEvent.id = eventId;
                    mDone.set(0);
                    eventReceiver.onPageReceived(tag, mEvent);
                }
            }

            @SuppressLint("MissingSuperCall")
            @Override
            public void onError(String tag2, String reason, Throwable cause) {
                eventReceiver.onError(tag, reason, cause);
            }
        };

        @SuppressLint("StringFormatMatches")
        AsyncLoadQEDPage<Event> page1 = new AsyncLoadQEDPage<>(
                Feature.DATABASE,
                String.format(application.getString(R.string.database_server_event), eventId, "1"),
                receiver,
                "page1",
                (String overview) -> {
                    StringReader reader = new StringReader(overview);
                    StringBuilder builder = new StringBuilder();

                    Event event;

                    try {
                        String name = readEventPage1Value(reader, builder, "titel");
                        String start = readEventPage1Value(reader, builder, "start");
                        String end = readEventPage1Value(reader, builder, "ende");
                        String cost = readEventPage1Value(reader, builder, "kosten");
                        String deadline = readEventPage1Value(reader, builder, "anmeldeschluss");
                        String maxMember = readEventPage1Value(reader, builder, "max_anzahl_teilnehmer");
                        String hotel = readEventPage1Value(reader, builder, "uebernachtung_id");

                        event = new Event(name, start, end, cost, deadline, maxMember, hotel);
                    } catch (IOException e) {
                        event = new Event();
                    }

                    try {
                        while (NetworkUtils.readUntilAfter(reader, "<tr class=\"data\"><td id='rollen_")) {
                            try {
                                Person person = new Person();
                                person.firstName = readTableColumn(reader, builder, "Vorname");
                                person.lastName = readTableColumn(reader, builder, "Nachname");
                                event.organizers.add(person);
                            } catch (IOException ignored) {}
                        }
                    } catch (IOException ignored) {}

                    return event;
                },
                new HashMap<>()
        );

        @SuppressLint("StringFormatMatches")
        AsyncLoadQEDPage<Event> page2 = new AsyncLoadQEDPage<>(
                Feature.DATABASE,
                String.format(application.getString(R.string.database_server_event), eventId, "2"),
                receiver,
                "page2",
                (String participants) -> {
                    Event event = new Event();

                    StringReader reader = new StringReader(participants);
                    StringBuilder builder = new StringBuilder();

                    try {
                        while (NetworkUtils.readUntilAfter(reader, "<div class=\"teilnehmerlink teilnehmer_")) {
                            try {
                                Person person = new Person();
                                person.id = NetworkUtils.readInt(reader);
                                person.firstName = readTableColumn(reader, builder, "Vorname");
                                person.lastName = readTableColumn(reader, builder, "Nachname");
                                person.email = readTableColumn(reader, builder, "E-Mail");

                                String status = readTableColumn(reader, builder, "Status");
                                switch (status) {
                                    case "offen":
                                        person.participationStatus = Person.ParticipationStatus.MEMBER_OPEN;
                                        break;
                                    case "bestaetigt":
                                        person.participationStatus = Person.ParticipationStatus.MEMBER_CONFIRMED;
                                        break;
                                    case "abgemeldet":
                                        person.participationStatus = Person.ParticipationStatus.MEMBER_OPT_OUT;
                                        break;
                                    case "teilgenommen":
                                        person.participationStatus = Person.ParticipationStatus.MEMBER_PARTICIPATED;
                                        break;
                                }

                                event.participants.add(person);
                            } catch (IOException ignored) {}
                        }
                    } catch (IOException ignored) {}

                    return event;
                },
                new HashMap<>()
        );

        page1.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void) null);
        page2.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void) null);

        return new AsyncTask[] {page1, page2};
    }

    public static AsyncTask<?,?,?>[] getEvent(String tag, @NonNull Event event, QEDPageReceiver<Event> eventReceiver) {
        return getEvent(tag, event.id, eventReceiver);
    }

    @Nullable
    public static AsyncTask<?,?,?>[] getEventList(String tag, QEDPageReceiver<List<Event>> eventListReceiver) {
        SoftReference<Application> applicationReference = Application.getApplicationReference();
        Application application = applicationReference.get();

        if (application == null) {
            Log.e(Application.LOG_TAG_ERROR, "", new Exception("Application is null!"));
            return null;
        }

        AsyncLoadQEDPage<List<Event>> list = new AsyncLoadQEDPage<>(
                Feature.DATABASE,
                application.getString(R.string.database_server_events),
                eventListReceiver,
                tag,
                (String string) -> {
                    List<Event> events = new LinkedList<>();

                    StringReader reader = new StringReader(string);
                    StringBuilder builder = new StringBuilder();

                    try {
                        while (NetworkUtils.readUntilAfter(reader, "<td class='veranstaltungen_")) {
                            try {
                                // Id
                                String id = String.valueOf(NetworkUtils.readInt(reader));
                                String name = readTableColumn(reader, builder, "Titel");
                                String start = readTableColumn(reader, builder, "Start");
                                String end = readTableColumn(reader, builder, "Ende");
                                String cost = readTableColumn(reader, builder, "Kosten");
                                String deadline = readTableColumn(reader, builder, "Anmeldeschluss");
                                String maxMember = readTableColumn(reader, builder, "Max. Teilnehmerzahl");
                                String hotel = readTableColumn(reader, builder, "&Uuml;bernachtung");

                                Event event = new Event(id, name, start, end, cost, deadline, maxMember, hotel);
                                events.add(event);
                            } catch (IOException ignored) {}
                        }
                    } catch (IOException e) {
                        return Collections.emptyList();
                    }

                    return events;
                },
                new HashMap<>()
        );

        list.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void) null);

        return new AsyncTask[] {list};
    }

    @Nullable
    public static AsyncTask<?,?,?>[] getPerson(String tag, long personId, QEDPageReceiver<Person> personReceiver) {
        SoftReference<Application> applicationReference = Application.getApplicationReference();
        Application application = applicationReference.get();

        if (application == null) {
            Log.e(Application.LOG_TAG_ERROR, "", new Exception("Application is null!"));
            return null;
        }

        QEDPageReceiver<Person> receiver = new QEDPageReceiver<Person>() {
            private final Person mPerson = new Person();
            private final AtomicInteger mDone = new AtomicInteger();

            @Override
            public void onPageReceived(String tag2, Person out) {
                switch (tag2) {
                    case "page1":
                        mPerson.firstName = out.firstName;
                        mPerson.lastName = out.lastName;
                        mPerson.email = out.email;
                        mPerson.birthday = out.birthday;
                        mPerson.homeStation = out.homeStation;
                        mPerson.railcard = out.railcard;
                        mPerson.memberSince = out.memberSince;
                        mPerson.active = out.active;
                        mPerson.member = out.member;
                        mPerson.id = personId;
                        break;
                    case "page2":
                        mPerson.addresses = out.addresses;
                        mPerson.phoneNumbers = out.phoneNumbers;
                        break;
                    case "page3":
                        mPerson.events = out.events;
                        mPerson.management = out.management;
                        break;
                }

                if (mDone.incrementAndGet() == 3) {
                    mDone.set(0);
                    personReceiver.onPageReceived(tag, mPerson);
                }
            }

            @SuppressLint("MissingSuperCall")
            @Override
            public void onError(String tag2, String reason, Throwable cause) {
                personReceiver.onError(tag, reason, cause);
            }
        };

        final boolean isSelf = application.loadData(Application.KEY_USERID, false).equals(String.valueOf(personId));

        @SuppressLint("StringFormatMatches") AsyncLoadQEDPage<Person> page1 = new AsyncLoadQEDPage<>(
                Feature.DATABASE,
                String.format(application.getString(R.string.database_server_person), personId, "1"),
                receiver,
                "page1",
                (String overview) -> {
                    Person person = new Person();
                    StringReader reader = new StringReader(overview);
                    StringBuilder builder = new StringBuilder();
                    try {
                        person.lastName = readPersonPage1Value(reader, builder, "nachname");
                        person.firstName = readPersonPage1Value(reader, builder, "vorname");
                        person.email = readPersonPage1Value(reader, builder, "sichtbare_email");
                        person.birthday = readPersonPage1Value(reader, builder, "sichtbarer_geburtstag");
                        if (isSelf) {
                            person.homeStation = readPersonPage1SelfValue(reader,builder,"heimatbahnhof");
                            person.railcard = readPersonPage1SelfValue(reader,builder,"bahncard");
                        } else {
                            person.homeStation = readPersonPage1Value(reader, builder, "heimatbahnhof");
                            person.railcard = readPersonPage1Value(reader, builder, "bahncard");
                        }
                        person.memberSince = readPersonPage1Value(reader, builder, "mitglied_seit");
                        person.member = "Ja".equals(readPersonPage1Value(reader, builder, "mitglied"));
                        person.active = "1".equals(readPersonPage1Value(reader, builder, "aktiv"));
                    } catch (IOException ignored) {}
                    return person;
                },
                new HashMap<>()
        );


        @SuppressLint("StringFormatMatches") AsyncLoadQEDPage<Person> page2 = new AsyncLoadQEDPage<>(
                Feature.DATABASE,
                String.format(application.getString(R.string.database_server_person), personId, "2"),
                receiver,
                "page2",
                (String details) -> {
                    Person person = new Person();

                    StringReader reader = new StringReader(details);
                    StringBuilder builder = new StringBuilder();

                    try {
                        while (NetworkUtils.readUntilAfter(reader, "<td class='person_adresse")) {
                            try {
                                String street = readTableColumn(reader, builder, "Strasse");
                                String number = readTableColumn(reader, builder, "Nummer");
                                String additions = readTableColumn(reader, builder, "Adresszusatz");
                                String zip = readTableColumn(reader, builder, "PLZ");
                                String city = readTableColumn(reader, builder, "Ort");
                                String country = readTableColumn(reader, builder, "Land");

                                if (isSelf) { // remove "<div class="noedit">"
                                    street = street.substring(20);
                                    number = number.substring(20);
                                    additions = additions.substring(20);
                                    zip = zip.substring(20);
                                    city = city.substring(20);
                                    country = country.substring(20);
                                }

                                person.addresses.add(toAddress(street, number, additions, zip, city, country));
                            } catch (IOException ignored) {}
                        }
                    } catch (IOException ignored) {}

                    reader = new StringReader(details);

                    try {
                        while (NetworkUtils.readUntilAfter(reader, "<td class='person_telefon")) {
                            try {
                                String type = readTableColumn(reader, builder, "Art");
                                String number = readTableColumn(reader, builder, "Nummer");

                                if (isSelf) { // remove "<div class="noedit">"
                                    type = type.substring(20);
                                    number = number.substring(20);
                                }

                                person.phoneNumbers.add(new Pair<>(type, number));
                            } catch (IOException ignored) {}
                        }
                    } catch (IOException ignored) {}

                    return person;
                },
                new HashMap<>()
        );


        @SuppressLint("StringFormatMatches") AsyncLoadQEDPage<Person> page3 = new AsyncLoadQEDPage<>(
                Feature.DATABASE,
                String.format(application.getString(R.string.database_server_person), personId, "3"),
                receiver,
                "page3",
                (String events) -> {
                    Person person = new Person();

                    StringReader reader = new StringReader(events);
                    StringBuilder builder = new StringBuilder();

                    try {
                        while (NetworkUtils.readUntilAfter(reader, "<tr class=\"data\"><td id='rollen")) {
                            try {
                                String role = readTableColumn(reader, builder, "Rolle");
                                String eventTitle = readTableColumn(reader, builder, "Titel");
                                String start = readTableColumn(reader, builder, "Start");
                                String end = readTableColumn(reader, builder, "Ende");

                                person.events.add(new Pair<>(new Event(eventTitle, start, end), role));
                            } catch (IOException ignored) {}
                        }
                    } catch (IOException ignored) {}

                    reader = new StringReader(events);

                    try {
                        while (NetworkUtils.readUntilAfter(reader, "<tr class=\"data\"><td id='funktionen")) {
                            try {
                                String role = readTableColumn(reader, builder, "Rolle");
                                String start = readTableColumn(reader, builder, "Start");
                                String end = readTableColumn(reader, builder, "Ende");

                                person.management.add(new Triple<>(role, start, end));
                            } catch (IOException ignored) {}
                        }
                    } catch (IOException ignored) {}
                    return person;
                },
                new HashMap<>()
        );


        page1.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void) null);
        page2.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void) null);
        page3.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void) null);

        return new AsyncTask[] {page1, page2, page3};
    }

    @Nullable
    public static AsyncTask<?,?,?>[] getPersonList(String tag, QEDPageReceiver<List<Person>> personListReceiver) {
        SoftReference<Application> applicationReference = Application.getApplicationReference();
        Application application = applicationReference.get();

        if (application == null) {
            Log.e(Application.LOG_TAG_ERROR, "", new Exception("Application is null!"));
            return null;
        }

        AsyncLoadQEDPage<List<Person>> list = new AsyncLoadQEDPage<>(
                Feature.DATABASE,
                application.getString(R.string.database_server_persons),
                personListReceiver,
                tag,
                (String string) -> {
                    List<Person> persons = new LinkedList<>();

                    StringReader reader = new StringReader(string);
                    StringBuilder builder = new StringBuilder();

                    try {
                        while (NetworkUtils.readUntilAfter(reader, "<td class='personen_")) {
                            try {
                                // Id
                                Person person = new Person();
                                person.id = NetworkUtils.readInt(reader);

                                person.lastName = readTableColumn(reader, builder, "Nachname");
                                person.firstName = readTableColumn(reader, builder, "Vorname");
                                person.email = readTableColumn(reader, builder, "Emailadresse");
                                person.birthday = readTableColumn(reader, builder, "Geburtstag");
                                person.homeStation = readTableColumn(reader, builder, "Heimatbahnhof");
                                person.railcard = readTableColumn(reader, builder, "Bahncard");
                                person.memberSince = readTableColumn(reader, builder, "Mitglied seit");
                                person.member = "Ja".equals(readTableColumn(reader, builder, "Aktuell Mitglied"));
                                person.active = "aktiv".equals(readTableColumn(reader, builder, "Aktiv"));
                                persons.add(person);
                            } catch (IOException ignored) {}
                        }
                    } catch (IOException e) {
                        return Collections.emptyList();
                    }

                    return persons;
                },
                new HashMap<>()
        );

        list.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void) null);

        return new AsyncTask[] {list};
    }

    private static String readTableColumn(StringReader reader, StringBuilder builder, String key) throws IOException {
        builder.setLength(0);
        NetworkUtils.readUntilAfter(reader,"title=\"" + key + "\">");
        NetworkUtils.readUntilAfter(reader, builder,"&nbsp;");
        return builder.toString();
    }

    private static String readEventPage1Value(StringReader reader, StringBuilder builder, String key) throws IOException {
        builder.setLength(0);
        NetworkUtils.readUntilAfter(reader,"veranstaltung_" + key + " cell' title=''>");
        NetworkUtils.readUntilAfter(reader, builder,"&nbsp;");
        return builder.toString();
    }

    private static String readPersonPage1Value(StringReader reader, StringBuilder builder, String key) throws IOException {
        builder.setLength(0);
        NetworkUtils.readUntilAfter(reader,"person_" + key + " cell' title=''>");
        NetworkUtils.readUntilAfter(reader, builder,"&nbsp;");
        return builder.toString();
    }

    private static String readPersonPage1SelfValue(StringReader reader, StringBuilder builder, String key) throws IOException {
        builder.setLength(0);
        NetworkUtils.readUntilAfter(reader,"person[" + key + "]\" value=\"");
        NetworkUtils.readUntilAfter(reader, builder,"\"");
        return builder.toString();
    }

    private static String toAddress(String street, String number, String additions, String zip, String city, String country) {
        String address = street + " " + number;
        if (!additions.isEmpty()) address += '\n' + additions;
        if (!zip.isEmpty() || !city.isEmpty()) address += '\n' + zip + " " + city;
        if (!country.isEmpty()) address += '\n' + country;

        return address;
    }
}
