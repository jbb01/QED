package com.jonahbauer.qed.model.parser;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;

import com.jonahbauer.qed.model.Person;
import com.jonahbauer.qed.model.Registration;
import com.jonahbauer.qed.networking.parser.HtmlParser;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public final class PersonParser extends HtmlParser<Person> {
    private static final String LOG_TAG = PersonParser.class.getName();
    
    public static final PersonParser INSTANCE = new PersonParser();

    private static final String GENERAL_KEY_FIRST_NAME = "Vorname:";
    private static final String GENERAL_KEY_LAST_NAME = "Nachname:";
    private static final String GENERAL_KEY_EMAIL = "Emailadresse:";
    private static final String GENERAL_KEY_BIRTH_DAY = "Geburtstag:";
    private static final String GENERAL_KEY_GENDER = "Geschlecht:";
    private static final String GENERAL_KEY_DATE_OF_JOINING = "Eingetreten:";
    private static final String GENERAL_KEY_LEAVING_DATE = "Ausgetreten:";
    private static final String GENERAL_KEY_MEMBER = "Aktuell Mitglied:";
    private static final String GENERAL_KEY_ACTIVE = "Aktiviertes Profil:";

    private static final String ADDITIONAL_KEY_HOME_STATION = "Bahnhof:";
    private static final String ADDITIONAL_KEY_RAILCARD = "Bahnermäßigung:";
    private static final String ADDITIONAL_KEY_NOTES = "Anmerkungen:";
    private static final String ADDITIONAL_KEY_FOOD = "Essenswünsche:";

    private static final String REGISTRATIONS_KEY_CANCELLED = "abgesagt";
    private static final String REGISTRATIONS_KEY_OPEN = "offen";
    private static final String REGISTRATIONS_KEY_ORGA = "Orga";

    private PersonParser() {}

    @NonNull
    @Override
    protected Person parse(@NonNull Person person, Document document) {
        parseGeneral(person, document.selectFirst("#person_general_information"));
        parseAdditional(person, document.selectFirst("#person_additional_information"));
        parseAddresses(person, document.selectFirst("#person_addresses"));
        parseContacts(person, document.selectFirst("#person_contacts"));
        parseRegistrations(person, document.selectFirst("#person_registrations"));

        return person;
    }

    private void parseGeneral(Person person, Element element) {
        if (element == null) return;
        element.select("dl dt")
               .forEach(dt -> {
                   try {
                       String key = dt.text();
                       Element value = dt.nextElementSibling();
                       //noinspection ConstantConditions
                       if (value.selectFirst("i") != null) return;

                       switch (key) {
                           case GENERAL_KEY_FIRST_NAME: {
                               person.setFirstName(value.text());
                               break;
                           }
                           case GENERAL_KEY_LAST_NAME: {
                               person.setLastName(value.text());
                               break;
                           }
                           case GENERAL_KEY_EMAIL: {
                               person.setEmail(value.child(0).text());
                               break;
                           }
                           case GENERAL_KEY_BIRTH_DAY: {
                               Element time = value.child(0);
                               person.setBirthdayString(time.text());
                               person.setBirthday(parseLocalDate(time));
                               break;
                           }
                           case GENERAL_KEY_GENDER: {
                               person.setGender(value.text());
                               break;
                           }
                           case GENERAL_KEY_DATE_OF_JOINING: {
                               Element time = value.child(0);
                               person.setDateOfJoiningString(time.text());
                               person.setDateOfJoining(parseLocalDate(time));
                               break;
                           }
                           case GENERAL_KEY_LEAVING_DATE: {
                               Element time = value.child(0);
                               person.setLeavingDateString(time.text());
                               person.setLeavingDate(parseLocalDate(time));
                               break;
                           }
                           case GENERAL_KEY_MEMBER: {
                               person.setMember(parseBoolean(value));
                               break;
                           }
                           case GENERAL_KEY_ACTIVE: {
                               person.setActive(parseBoolean(value));
                               break;
                           }
                       }
                   } catch (Exception e) {
                       Log.e(LOG_TAG, "Error parsing person " + person.getId() + ".", e);
                   }
               });
    }

    private void parseAdditional(Person person, Element element) {
        if (element == null) return;
        element.select("dl dt")
               .forEach(dt -> {
                   try {
                       String key = dt.text();
                       Element value = dt.nextElementSibling();
                       //noinspection ConstantConditions
                       if (value.selectFirst("i") != null) return;

                       switch (key) {
                           case ADDITIONAL_KEY_HOME_STATION: {
                               person.setHomeStation(value.text());
                               break;
                           }
                           case ADDITIONAL_KEY_RAILCARD: {
                               person.setRailcard(value.text());
                               break;
                           }
                           case ADDITIONAL_KEY_NOTES: {
                               person.setNotes(value.text());
                               break;
                           }
                           case ADDITIONAL_KEY_FOOD: {
                               person.setFood(value.text());
                               break;
                           }
                       }
                   } catch (Exception e) {
                       Log.e(LOG_TAG, "Error parsing person " + person.getId() + ".", e);
                   }
               });
    }

    private void parseAddresses(Person person, Element element) {
        if (element == null) return;
        if (element.selectFirst("i") != null) return;

        element.select(".address").forEach(address ->
                person.getAddresses().add(parseAddress(address))
        );
    }

    private void parseContacts(Person person, Element element) {
        if (element == null) return;
        if (element.selectFirst("i") != null) return;

        element.select("dl dt")
               .forEach(dt -> {
                   try {
                       String key = dt.text();
                       Element value = dt.nextElementSibling();
                       //noinspection ConstantConditions
                       if (value.selectFirst("i") != null) return;

                       person.getContacts().add(Pair.create(
                               key.substring(0, key.length() - 1),
                               value.text()
                       ));
                   } catch (Exception e) {
                       Log.e(LOG_TAG, "Error parsing person " + person.getId() + ".", e);
                   }
               });
    }

    private void parseRegistrations(Person person, Element element) {
        if (element == null) return;
        if (element.selectFirst("p > i") != null) return;

        element.select("ul li").forEach(li -> {
            try {
                long id = Long.parseLong(li.child(0).attr("href").substring("/registrations/".length()));
                String event = li.child(0).text();

                Element statusElement = li.selectFirst("i");
                String statusString = statusElement != null ? statusElement.text() : "";

                boolean orga = statusString.contains(REGISTRATIONS_KEY_ORGA);

                Registration.Status status = Registration.Status.CONFIRMED;
                if (statusString.contains(REGISTRATIONS_KEY_CANCELLED)) {
                    status = Registration.Status.CANCELLED;
                } else if (statusString.contains(REGISTRATIONS_KEY_OPEN)) {
                    status = Registration.Status.OPEN;
                }

                Registration registration = new Registration(id);
                registration.setStatus(status);
                registration.setOrganizer(orga);
                registration.setEventTitle(event);
                registration.setPerson(person);

                person.getEvents().remove(registration);
                person.getEvents().add(registration);
            } catch (Exception e) {
                Log.e(LOG_TAG, "Error parsing person " + person.getId() + ".", e);
            }
        });
    }

    public static String parseAddress(Element element) {
        return element.html().replaceAll("\\s*<br>\\s*", "\n").trim();
    }
}
