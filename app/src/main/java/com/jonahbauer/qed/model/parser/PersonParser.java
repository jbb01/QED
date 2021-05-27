package com.jonahbauer.qed.model.parser;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;

import com.jonahbauer.qed.Application;
import com.jonahbauer.qed.model.Person;
import com.jonahbauer.qed.model.Registration;
import com.jonahbauer.qed.networking.parser.HtmlParser;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public final class PersonParser extends HtmlParser<Person> {
    public static final PersonParser INSTANCE = new PersonParser();

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
        element.select("dl dt")
               .forEach(dt -> {
                   try {
                       String key = dt.text();
                       Element value = dt.nextElementSibling();
                       if (value.selectFirst("i") != null) return;

                       switch (key) {
                           case "Vorname:": {
                               person.setFirstName(value.text());
                               break;
                           }
                           case "Nachname:": {
                               person.setLastName(value.text());
                               break;
                           }
                           case "Emailadresse:": {
                               person.setEmail(value.child(0).text());
                               break;
                           }
                           case "Geburtstag:": {
                               Element time = value.child(0);
                               person.setBirthdayString(time.text());
                               person.setBirthday(parseDate(time.attr("datetime")));
                               break;
                           }
                           case "Eingetreten:": {
                               Element time = value.child(0);
                               person.setMemberSinceString(time.text());
                               person.setMemberSince(parseDate(time.attr("datetime")));
                               break;
                           }
                           case "Ausgetreten:": {
                               break;
                           }
                           case "Aktuell Mitglied:": {
                               person.setMember("Ja".equals(value.text()));
                               break;
                           }
                           case "Aktiviertes Profil:": {
                               person.setActive("Ja".equals(value.text()));
                               break;
                           }
                       }
                   } catch (Exception e) {
                       Log.e(Application.LOG_TAG_ERROR, "Error parsing person " + person.getId() + ".", e);
                   }
               });
    }

    private void parseAdditional(Person person, Element element) {
        element.select("dl dt")
               .forEach(dt -> {
                   try {
                       String key = dt.text();
                       Element value = dt.nextElementSibling();
                       if (value.selectFirst("i") != null) return;

                       switch (key) {
                           case "Bahnhof:": {
                               person.setHomeStation(value.text());
                               break;
                           }
                           case "Bahnermäßigung:": {
                               person.setRailcard(value.text());
                               break;
                           }
                           case "Anmerkungen:":
                           case "Essenswünsche:": {
                               break;
                           }
                       }
                   } catch (Exception e) {
                       Log.e(Application.LOG_TAG_ERROR, "Error parsing person " + person.getId() + ".", e);
                   }
               });
    }

    private void parseAddresses(Person person, Element element) {
        if (element.selectFirst("i") != null) return;

        element.select(".address").forEach(address -> person.getAddresses().add(address.text()));
    }

    private void parseContacts(Person person, Element element) {
        if (element.selectFirst("i") != null) return;

        element.select("dl dt")
               .forEach(dt -> {
                   try {
                       String key = dt.text();
                       Element value = dt.nextElementSibling();
                       if (value.selectFirst("i") != null) return;

                       person.getContacts().add(Pair.create(
                               key.substring(0, key.length() - 1),
                               value.text()
                       ));
                   } catch (Exception e) {
                       Log.e(Application.LOG_TAG_ERROR, "Error parsing person " + person.getId() + ".", e);
                   }
               });
    }

    private void parseRegistrations(Person person, Element element) {
        if (element.selectFirst("p > i") != null) return;

        element.select("ul li").forEach(li -> {
            try {
                long id = Long.parseLong(li.child(0).attr("href").substring("/registrations/".length()));
                String event = li.child(0).text();

                Element statusElement = li.selectFirst("i");
                String statusString = statusElement != null ? statusElement.text() : "";

                Registration.Status status = Registration.Status.CONFIRMED;
                if (statusString.contains("Orga")) {
                    status = Registration.Status.ORGA;
                } else if (statusString.contains("abgesagt")) {
                    status = Registration.Status.CANCELLED;
                } else if (statusString.contains("offen")) {
                    status = Registration.Status.OPEN;
                }

                Registration registration = new Registration(id);
                registration.setStatus(status);

                person.getEvents().put(event, registration);
            } catch (Exception e) {
                Log.e(Application.LOG_TAG_ERROR, "Error parsing person " + person.getId() + ".", e);
            }
        });
    }
}
