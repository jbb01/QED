package eu.jonahbauer.qed.network.parser.database;

import android.util.Log;

import androidx.annotation.NonNull;

import androidx.annotation.Nullable;
import eu.jonahbauer.qed.model.Person;
import eu.jonahbauer.qed.model.Registration;

import eu.jonahbauer.qed.model.contact.ContactDetail;
import eu.jonahbauer.qed.util.TextUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.EnumSet;
import java.util.Map;
import java.util.function.Consumer;

public final class PersonParser extends DatabaseParser<Person> {
    private static final String LOG_TAG = PersonParser.class.getName();
    
    public static final PersonParser INSTANCE = new PersonParser();

    private static final String GENERAL_KEY_FIRST_NAME = "Vorname";
    private static final String GENERAL_KEY_LAST_NAME = "Nachname";
    private static final String GENERAL_KEY_EMAIL = "Emailadresse";
    private static final String GENERAL_KEY_BIRTH_DAY = "Geburtstag";
    private static final String GENERAL_KEY_GENDER = "Geschlecht";
    private static final String GENERAL_KEY_DATE_OF_JOINING = "Eingetreten";
    private static final String GENERAL_KEY_DATE_OF_QUITTING = "Ausgetreten";
    private static final String GENERAL_KEY_MEMBER_UNTIL = "Mitglied bis";
    private static final String GENERAL_KEY_PAID_UNTIL = "Bezahlt bis";
    private static final String GENERAL_KEY_MEMBER = "Aktuell Mitglied";
    private static final String GENERAL_KEY_ACTIVE = "Aktiviertes Profil";

    private static final String ADDITIONAL_KEY_HOME_STATION = "Bahnhof";
    private static final String ADDITIONAL_KEY_RAILCARD = "Bahnermäßigung";
    private static final String ADDITIONAL_KEY_NOTES = "Anmerkungen";
    private static final String ADDITIONAL_KEY_FOOD = "Essenswünsche";

    private static final String PAYMENT_AMOUNT = "Betrag";

    private static final String PAYMENT_PURPOSE_REGULAR_MEMBER = "Mitgliedsbeitrag";
    private static final String PAYMENT_PURPOSE_SPONSOR_MEMBER = "Förderbeitrag";
    private static final String PAYMENT_PURPOSE_DONATION = "Spende an den Verein";
    private static final String PAYMENT_PURPOSE_OTHER = "Anderer Verwendungszweck";
    private static final String PAYMENT_PURPOSE_FREE_MEMBER = "Freimitgliedschaft";
    private static final String PAYMENT_PURPOSE_SPONSOR_AND_MEMBER = "Fördermitglied";

    private static final String PRIVACY_NEWSLETTER = "Newsletter abonnieren";
    private static final String PRIVACY_PHOTOS = "Fotoeinwilligung vorhanden";
    private static final String PRIVACY_PUBLIC_BIRTHDAY = "Geburtstag einsehbar";
    private static final String PRIVACY_PUBLIC_EMAIL = "Emailadresse einsehbar";
    private static final String PRIVACY_PUBLIC_ADDRESS = "Adressen und Kontakte einsehbar";
    private static final String PRIVACY_PUBLIC_PROFILE = "Profil veröffentlichen";

    private PersonParser() {}

    @NonNull
    @Override
    protected Person parse(@NonNull Person person, @NonNull Document document) {
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

    private void parseGeneral(@NonNull Person person, @Nullable Element element) {
        parseDefinitionList(element, Map.ofEntries(
                Map.entry(GENERAL_KEY_FIRST_NAME, (dt, dd) -> parseString(dd, person::setFirstName)),
                Map.entry(GENERAL_KEY_LAST_NAME, (dt, dd) -> parseString(dd, person::setLastName)),
                Map.entry(GENERAL_KEY_EMAIL, (dt, dd) -> parseString(dd.child(0), person::setEmail)),
                Map.entry(GENERAL_KEY_BIRTH_DAY, (dt, dd) -> parseLocalDate(dd, person::setBirthday)),
                Map.entry(GENERAL_KEY_GENDER, (dt, dd) -> parsePersonGender(dd, person::setGender)),
                Map.entry(GENERAL_KEY_DATE_OF_JOINING, (dt, dd) -> parseLocalDate(dd, person::setDateOfJoining)),
                Map.entry(GENERAL_KEY_DATE_OF_QUITTING, (dt, dd) -> parseLocalDate(dd, person::setDateOfQuitting)),
                Map.entry(GENERAL_KEY_MEMBER_UNTIL, (dt, dd) -> parseLocalDate(dd, person::setMemberUntil)),
                Map.entry(GENERAL_KEY_PAID_UNTIL, (dt, dd) -> parseLocalDate(dd, person::setPaidUntil)),
                Map.entry(GENERAL_KEY_MEMBER, (dt, dd) -> parseBoolean(dd, person::setMember)),
                Map.entry(GENERAL_KEY_ACTIVE, (dt, dd) -> parseBoolean(dd, person::setActive))
        ));
    }

    private void parseAdditional(@NonNull Person person, @Nullable Element element) {
        parseDefinitionList(element, Map.ofEntries(
                Map.entry(ADDITIONAL_KEY_HOME_STATION, (dt, dd) -> parseString(dd, person::setHomeStation)),
                Map.entry(ADDITIONAL_KEY_RAILCARD, (dt, dd) -> parseString(dd, person::setRailcard)),
                Map.entry(ADDITIONAL_KEY_NOTES, (dt, dd) -> parseString(dd, person::setNotes)),
                Map.entry(ADDITIONAL_KEY_FOOD, (dt, dd) -> parseString(dd, person::setFood))
        ));
    }

    private void parseAddresses(@NonNull Person person, @Nullable Element element) {
        if (element == null) return;
        if (element.selectFirst("> i") != null) return;

        for (var address : element.select(".address")) {
            parseMultilineString(address, person.getAddresses()::add);
        }
    }

    private void parseContacts(@NonNull Person person, @Nullable Element element) {
        parseDefinitionList(element, (dt, dd) -> {
            var contact = dd.text();
            if (TextUtils.isNullOrBlank(contact)) return;

            var key = dt.text();
            person.getContacts().add(new ContactDetail(
                    key.substring(0, key.length() - 1),
                    contact
            ));
        });
    }

    private void parseGroups(@NonNull Person person, @Nullable Element element) {
        if (element == null) return;
        if (element.selectFirst("> i") != null) return;

        for (var node : element.select(":is(div, span)")) {
            person.getGroups().add(node.text());
        }
    }

    private void parsePrivacy(@NonNull Person person, @Nullable Element element) {
        if (element == null) return;

        var text = element.text();
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

    private void parsePayments(@NonNull Person person, @Nullable Element element) {
        parseDefinitionList(element, false, (dt, dd) -> {
            var payment = new Person.Payment();

            parseLocalDateRange(dt, (range) -> {
                payment.setStart(range.first);
                payment.setEnd(range.second);
            });
            parseCurrency(dd, payment::setAmount);
            parsePaymentPurpose(dd, payment::setPurpose);

            // TODO parse comment

            person.getPayments().add(payment);
        });
    }

    private void parseRegistrations(@NonNull Person person, @Nullable Element element) {
        if (element == null) return;

        for (var li : element.select("ul li")) {
            try {
                Element a = li.selectFirst("a");
                if (a == null) continue;

                var id = parseIdFromHref(li.child(0), Registration.NO_ID);
                Registration registration = new Registration(id);
                registration.setPerson(person);

                parseString(a, registration::setEventTitle);
                parseRegistrationStatus(li.selectFirst("i"), (status, orga) -> {
                    registration.setStatus(status);
                    registration.setOrganizer(orga);
                });

                person.getEvents().remove(registration);
                person.getEvents().add(registration);
            } catch (Exception e) {
                Log.e(LOG_TAG, "Error parsing person " + person.getId() + ".", e);
            }
        }
    }

    private static void parsePaymentPurpose(@Nullable Element element, @NonNull Consumer<Person.Payment.Purpose> action) {
        if (element == null) return;

        try {
            String text = element.text();

            if (text.contains(PAYMENT_PURPOSE_REGULAR_MEMBER)) {
                action.accept(Person.Payment.Purpose.REGULAR_MEMBER);
            } else if (text.contains(PAYMENT_PURPOSE_SPONSOR_MEMBER)) {
                action.accept(Person.Payment.Purpose.SPONSOR_MEMBER);
            } else if (text.contains(PAYMENT_PURPOSE_DONATION)) {
                action.accept(Person.Payment.Purpose.DONATION);
            } else if (text.contains(PAYMENT_PURPOSE_OTHER)) {
                action.accept(Person.Payment.Purpose.OTHER);
            } else if (text.contains(PAYMENT_PURPOSE_FREE_MEMBER)) {
                action.accept(Person.Payment.Purpose.FREE_MEMBER);
            } else if (text.contains(PAYMENT_PURPOSE_SPONSOR_AND_MEMBER)) {
                action.accept(Person.Payment.Purpose.SPONSOR_AND_MEMBER);
            }
        } catch (Exception ignored) {
            // ignored
        }
    }
}
