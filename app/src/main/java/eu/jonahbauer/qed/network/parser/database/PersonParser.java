package eu.jonahbauer.qed.network.parser.database;

import android.util.Log;

import androidx.annotation.NonNull;

import eu.jonahbauer.qed.model.Person;
import eu.jonahbauer.qed.model.Registration;

import eu.jonahbauer.qed.model.contact.ContactDetail;
import eu.jonahbauer.qed.util.TextUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.EnumSet;
import java.util.regex.Pattern;

public final class PersonParser extends DatabaseParser<Person> {
    private static final String LOG_TAG = PersonParser.class.getName();
    
    public static final PersonParser INSTANCE = new PersonParser();

    private static final Pattern COST_PATTERN = Pattern.compile("(\\d+(?:,\\d+)?)\\s*€");

    private static final String GENERAL_KEY_FIRST_NAME = "Vorname:";
    private static final String GENERAL_KEY_LAST_NAME = "Nachname:";
    private static final String GENERAL_KEY_EMAIL = "Emailadresse:";
    private static final String GENERAL_KEY_BIRTH_DAY = "Geburtstag:";
    private static final String GENERAL_KEY_GENDER = "Geschlecht:";
    private static final String GENERAL_KEY_DATE_OF_JOINING = "Eingetreten:";
    private static final String GENERAL_KEY_DATE_OF_QUITTING = "Ausgetreten:";
    private static final String GENERAL_KEY_MEMBER_UNTIL = "Mitglied bis:";
    private static final String GENERAL_KEY_PAID_UNTIL = "Bezahlt bis:";
    private static final String GENERAL_KEY_MEMBER = "Aktuell Mitglied:";
    private static final String GENERAL_KEY_ACTIVE = "Aktiviertes Profil:";

    private static final String ADDITIONAL_KEY_HOME_STATION = "Bahnhof:";
    private static final String ADDITIONAL_KEY_RAILCARD = "Bahnermäßigung:";
    private static final String ADDITIONAL_KEY_NOTES = "Anmerkungen:";
    private static final String ADDITIONAL_KEY_FOOD = "Essenswünsche:";

    private static final String REGISTRATIONS_KEY_ORGA = "Orga";

    private static final String PAYMENT_AMOUNT = "Betrag";

    private static final String PAYMENT_TYPE_REGULAR_MEMBER = "Mitgliedsbeitrag";
    private static final String PAYMENT_TYPE_SPONSOR_MEMBER = "Förderbeitrag";
    private static final String PAYMENT_TYPE_DONATION = "Spende an den Verein";
    private static final String PAYMENT_TYPE_OTHER = "Anderer Verwendungszweck";
    private static final String PAYMENT_TYPE_FREE_MEMBER = "Freimitgliedschaft";
    private static final String PAYMENT_TYPE_SPONSOR_AND_MEMBER = "Fördermitglied";

    private static final String GENDER_MALE = "männlich";
    private static final String GENDER_FEMALE = "weiblich";
    private static final String GENDER_OTHER = "andere";


    private static final String PRIVACY_NEWSLETTER = "Newsletter abonnieren";
    private static final String PRIVACY_PHOTOS = "Fotoeinwilligung vorhanden";
    private static final String PRIVACY_PUBLIC_BIRTHDAY = "Geburtstag einsehbar";
    private static final String PRIVACY_PUBLIC_EMAIL = "Emailadresse einsehbar";
    private static final String PRIVACY_PUBLIC_ADDRESS = "Adressen und Kontakte einsehbar";
    private static final String PRIVACY_PUBLIC_PROFILE = "Profil veröffentlichen";

    private PersonParser() {}

    @NonNull
    @Override
    protected Person parse(@NonNull Person person, Document document) {
        parseGeneral(person, document.selectFirst("#person_general_information"));
        parseAdditional(person, document.selectFirst("#person_additional_information"));
        parseAddresses(person, document.selectFirst("#person_addresses"));
        parseContacts(person, document.selectFirst("#person_contacts"));
        parseGroups(person, document.selectFirst("#person_groups_automatic"));
        parseGroups(person, document.selectFirst("#person_groups_explicit"));
        parsePayments(person, document.selectFirst("#person_payments"));
        parseRegistrations(person, document.selectFirst("#person_registrations"));
        parsePrivacy(person, document.selectFirst("#person_privacy"));

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
                               person.setBirthday(parseLocalDate(time));
                               break;
                           }
                           case GENERAL_KEY_GENDER: {
                               person.setGender(parseGender(value.text()));
                               break;
                           }
                           case GENERAL_KEY_DATE_OF_JOINING: {
                               Element time = value.child(0);
                               person.setDateOfJoining(parseLocalDate(time));
                               break;
                           }
                           case GENERAL_KEY_DATE_OF_QUITTING: {
                               Element time = value.child(0);
                               person.setDateOfQuitting(parseLocalDate(time));
                               break;
                           }
                           case GENERAL_KEY_MEMBER_UNTIL: {
                               Element time = value.child(0);
                               person.setMemberUntil(parseLocalDate(time));
                               break;
                           }
                           case GENERAL_KEY_PAID_UNTIL: {
                               Element time = value.child(0);
                               person.setPaidUntil(parseLocalDate(time));
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
        if (element.selectFirst("> i") != null) return;

        element.select(".address").forEach(e -> {
            var address = parseAddress(e);
            if (TextUtils.isNullOrBlank(address)) return;
            person.getAddresses().add(address);
        });
    }

    private void parseContacts(Person person, Element element) {
        if (element == null) return;
        if (element.selectFirst("> i") != null) return;

        element.select("dl dt")
               .forEach(dt -> {
                   try {
                       String key = dt.text();
                       Element value = dt.nextElementSibling();
                       //noinspection ConstantConditions
                       if (value.selectFirst("i") != null) return;

                       var contact = value.text();
                       if (TextUtils.isNullOrBlank(contact)) return;

                       person.getContacts().add(new ContactDetail(
                               key.substring(0, key.length() - 1),
                               contact
                       ));
                   } catch (Exception e) {
                       Log.e(LOG_TAG, "Error parsing person " + person.getId() + ".", e);
                   }
               });
    }

    private void parseGroups(Person person, Element element) {
        if (element == null) return;

        element.select("span").forEach(span -> {
            person.getGroups().add(span.text());
        });
    }

    private void parsePrivacy(Person person, Element element) {
        if (element == null) return;
        var p = element.selectFirst("p");
        if (p == null) return;

        var text = p.text();
        var privacy = EnumSet.noneOf(Person.Privacy.class);

        if (text.contains(PRIVACY_NEWSLETTER)) {
            privacy.add(Person.Privacy.NEWSLETTER);
        }
        if (text.contains(PRIVACY_PHOTOS)) {
            privacy.add(Person.Privacy.PHOTOS);
        }
        if (text.contains(PRIVACY_PUBLIC_BIRTHDAY)) {
            privacy.add(Person.Privacy.PUBLIC_BIRTHDAY);
        }
        if (text.contains(PRIVACY_PUBLIC_EMAIL)) {
            privacy.add(Person.Privacy.PUBLIC_EMAIL);
        }
        if (text.contains(PRIVACY_PUBLIC_ADDRESS)) {
            privacy.add(Person.Privacy.PUBLIC_ADDRESS);
        }
        if (text.contains(PRIVACY_PUBLIC_PROFILE)) {
            privacy.add(Person.Privacy.PUBLIC_PROFILE);
        }

        person.setPrivacy(privacy);
    }

    private void parsePayments(Person person, Element element) {
        if (element == null) return;
        if (element.selectFirst("> i") != null) return;

        element.select("dl dt")
                .forEach(dt -> {
                    try {
                        var value = dt.nextElementSibling();
                        if (value == null) return;

                        var payment = new Person.Payment();

                        var typeNode = value.selectFirst("i");
                        payment.setType(typeNode == null ? null : parsePaymentType(typeNode.text()));

                        value.textNodes().forEach(node -> {
                            var text = node.text();
                            if (text.contains(PAYMENT_AMOUNT)) {
                                // payment amount
                                var matcher = COST_PATTERN.matcher(text);
                                if (matcher.find()) {
                                    var string = matcher.group(1);
                                    if (string != null) try {
                                        payment.setAmount(Double.parseDouble(string));
                                    } catch (NumberFormatException ignored) {}
                                }
                            } else if (!TextUtils.isBlank(text)) {
                                // payment comment
                                payment.setComment(node.text());
                            }
                        });

                        // payment start and end
                        var times = dt.select("time");
                        if (times.size() == 1) {
                            var time = parseLocalDate(times.get(0));
                            payment.setStart(time);
                            payment.setEnd(time);
                        } else if (times.size() > 1) {
                            var start = parseLocalDate(times.get(0));
                            var end = parseLocalDate(times.get(1));
                            payment.setStart(start);
                            payment.setEnd(end);
                        }

                        person.getPayments().add(payment);
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
                var id = parseIdFromHref(li.child(0), Registration.NO_ID);
                String event = li.child(0).text();

                var statusElement = li.selectFirst("i");
                var statusString = statusElement != null ? statusElement.text() : "";
                var orga = statusString.contains(REGISTRATIONS_KEY_ORGA);
                var status = RegistrationStatusParser.INSTANCE.parse(statusString);

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

    private static Person.Payment.Type parsePaymentType(String type) {
        if (type == null) return null;
        if (type.startsWith("(")) type = type.substring(1);
        if (type.endsWith(")")) type = type.substring(0, type.length() - 1);

        switch (type) {
            case PAYMENT_TYPE_OTHER:
                return Person.Payment.Type.OTHER;
            case PAYMENT_TYPE_DONATION:
                return Person.Payment.Type.DONATION;
            case PAYMENT_TYPE_REGULAR_MEMBER:
                return Person.Payment.Type.REGULAR_MEMBER;
            case PAYMENT_TYPE_SPONSOR_MEMBER:
                return Person.Payment.Type.SPONSOR_MEMBER;
            case PAYMENT_TYPE_SPONSOR_AND_MEMBER:
                return Person.Payment.Type.SPONSOR_AND_MEMBER;
            case PAYMENT_TYPE_FREE_MEMBER:
                return Person.Payment.Type.FREE_MEMBER;
            default:
                return null;
        }
    }

    static Person.Gender parseGender(String gender) {
        if (gender == null) return null;
        switch (gender) {
            case GENDER_MALE:
                return Person.Gender.MALE;
            case GENDER_FEMALE:
                return Person.Gender.FEMALE;
            case GENDER_OTHER:
                return Person.Gender.OTHER;
            default:
                return null;
        }
    }
}
