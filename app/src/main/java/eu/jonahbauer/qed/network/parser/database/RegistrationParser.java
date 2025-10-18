package eu.jonahbauer.qed.network.parser.database;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import eu.jonahbauer.qed.model.Event;
import eu.jonahbauer.qed.model.Person;
import eu.jonahbauer.qed.model.Registration;
import eu.jonahbauer.qed.network.parser.HtmlParseException;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.Map;
import java.util.function.Consumer;

public final class RegistrationParser extends DatabaseParser<Registration> {
    private static final String GENERAL_KEY_EVENT = "Veranstaltung";
    private static final String GENERAL_KEY_PERSON = "Person";
    private static final String GENERAL_KEY_ORGANIZER = "Organisator";
    private static final String GENERAL_KEY_STATUS = "Anmeldestatus";
    private static final String GENERAL_KEY_BIRTHDAY = "Geburtstag";
    private static final String GENERAL_KEY_GENDER = "Geschlecht";
    private static final String GENERAL_KEY_MAIL = "Emailadresse";
    private static final String GENERAL_KEY_ADDRESS = "Adresse";
    private static final String GENERAL_KEY_PHONE = "Handynummer";

    private static final String TRANSPORT_KEY_TIME_OF_ARRIVAL = "Ankunftszeit";
    private static final String TRANSPORT_KEY_TIME_OF_DEPARTURE = "Abfahrtszeit";
    private static final String TRANSPORT_KEY_SOURCE_STATION = "Anreisebahnhof";
    private static final String TRANSPORT_KEY_TARGET_STATION = "Abfahrtsbahnhof";
    private static final String TRANSPORT_KEY_RAILCARD = "Bahncard";
    private static final String TRANSPORT_KEY_OVERNIGHT_STAYS = "Übernachtungen";

    private static final String PAYMENT_KEY_FULL = "Vollständig Bezahlt";
    private static final String PAYMENT_KEY_MEMBERSHIP_ABATEMENT = "Mitgliedsermäßigung";
    private static final String PAYMENT_KEY_OTHER_ABATEMENT = "Sonstige Ermäßigungen";

    private static final String PAYMENT_TYPE_TRANSFER = "Überweisung";
    private static final String PAYMENT_TYPE_EXPENSE = "Auslage";

    private static final String PAYMENT_CATEGORY_SHOPPING = "Einkauf";
    private static final String PAYMENT_CATEGORY_TALK_GIFT = "Vortragsgeschenk";
    private static final String PAYMENT_CATEGORY_DEPOSIT = "Pfand";
    private static final String PAYMENT_CATEGORY_ORGANIZER_GIFT = "Orgageschenk";
    private static final String PAYMENT_CATEGORY_TRANSPORTATION = "Transport";
    private static final String PAYMENT_CATEGORY_TALK_MATERIALS = "Vortragsmaterial";

    private static final String ADDITIONAL_KEY_FOOD = "Essenswünsche";
    private static final String ADDITIONAL_KEY_TALKS = "Vorträge";
    private static final String ADDITIONAL_KEY_NOTES = "Anmerkungen";

    public static final RegistrationParser INSTANCE = new RegistrationParser();

    private RegistrationParser() {}

    @NonNull
    @Override
    protected Registration parse(@NonNull Registration registration, @NonNull Document document) {
        parseGeneral(registration, document.selectFirst("#registration_general_information"));
        parseStatus(registration, document.selectFirst("#registration_status"));
        parseTransport(registration, document.selectFirst("#registration_transport"));
        parseAdditional(registration, document.selectFirst("#registration_additional"));
        return registration;
    }

    private void parseGeneral(@NonNull Registration registration, @Nullable Element element) {
        parseDefinitionList(element, Map.ofEntries(
                Map.entry(GENERAL_KEY_EVENT, (dt, dd) -> {
                    var eventElement = dd.selectFirst("a");
                    if (eventElement == null) throw new HtmlParseException("Registration " + registration.getId() + " does not seem to contain an event.");

                    var id = parseIdFromHref(eventElement, Event.NO_ID);
                    registration.setEventId(id);
                    registration.setEventTitle(eventElement.text());
                }),
                Map.entry(GENERAL_KEY_PERSON, (dt, dd) -> {
                    var personElement = dd.selectFirst("a");
                    if (personElement == null) throw new HtmlParseException("Registration " + registration.getId() + " does not seem to contain a person.");

                    var id = parseIdFromHref(personElement, Person.NO_ID);
                    registration.setPersonId(id);
                    registration.setPersonName(personElement.text());
                }),
                Map.entry(GENERAL_KEY_ORGANIZER, (dt, dd) -> parseBoolean(dd, registration::setOrganizer)),
                Map.entry(GENERAL_KEY_STATUS, (dt, dd) -> parseRegistrationStatus(dd, (status, orga) -> registration.setStatus(status))),
                Map.entry(GENERAL_KEY_BIRTHDAY, (dt, dd) -> parseLocalDate(dd, registration::setPersonBirthday)),
                Map.entry(GENERAL_KEY_GENDER, (dt, dd) -> parsePersonGender(dd, registration::setPersonGender)),
                Map.entry(GENERAL_KEY_MAIL, (dt, dd) -> parseString(dd.selectFirst("a"), registration::setPersonMail)),
                Map.entry(GENERAL_KEY_ADDRESS, (dt, dd) -> parseMultilineString(dd.selectFirst(".address"), registration::setPersonAddress)),
                Map.entry(GENERAL_KEY_PHONE, (dt, dd) -> parseString(dd.selectFirst("a"), registration::setPersonPhone))
        ));
    }

    private void parseStatus(@NonNull Registration registration, @Nullable Element element) {
        if (element == null) return;

        parseDefinitionList(element.selectFirst("h3:contains(Zahlungsinformationen) + dl"), Map.ofEntries(
                Map.entry(PAYMENT_KEY_FULL, (dt, dd) -> parseBoolean(dd, registration::setPaymentDone)),
                Map.entry(PAYMENT_KEY_MEMBERSHIP_ABATEMENT, (dt, dd) -> parseBoolean(dd, registration::setMemberAbatement)),
                Map.entry(PAYMENT_KEY_OTHER_ABATEMENT, (dt, dd) -> parseString(dd, registration::setOtherAbatement))
        ));

        registration.getPayments().clear();
        parseDefinitionList(element.selectFirst("h3:contains(Zahlungen) + dl"), (dt, dd) -> {
            Registration.Payment payment = new Registration.Payment();

            parseLocalDate(dt, payment::setDate);
            parseCurrency(dd, payment::setAmount);
            parsePaymentType(dd, payment::setType);
            parsePaymentCategory(dd, payment::setCategory);

            if (payment.getType() != null && payment.getAmount() != null) {
                registration.getPayments().add(payment);
            }
        });
    }

    private void parseTransport(@NonNull Registration registration, @Nullable Element element) {
        parseDefinitionList(element, Map.ofEntries(
                Map.entry(TRANSPORT_KEY_TIME_OF_ARRIVAL, (dt, dd) -> parseInstant(dd, registration::setTimeOfArrival)),
                Map.entry(TRANSPORT_KEY_TIME_OF_DEPARTURE, (dt, dd) -> parseInstant(dd, registration::setTimeOfDeparture)),
                Map.entry(TRANSPORT_KEY_SOURCE_STATION, (dt, dd) -> parseString(dd, registration::setSourceStation)),
                Map.entry(TRANSPORT_KEY_TARGET_STATION, (dt, dd) -> parseString(dd, registration::setTargetStation)),
                Map.entry(TRANSPORT_KEY_RAILCARD, (dt, dd) -> parseString(dd, registration::setRailcard)),
                Map.entry(TRANSPORT_KEY_OVERNIGHT_STAYS, (dt, dd) -> parseInteger(dd, registration::setOvernightStays))
        ));
    }

    private void parseAdditional(@NonNull Registration registration, @Nullable Element element) {
        parseDefinitionList(element, Map.ofEntries(
                Map.entry(ADDITIONAL_KEY_FOOD, (dt, dd) -> parseString(dd, registration::setFood)),
                Map.entry(ADDITIONAL_KEY_TALKS, (dt, dd) -> parseString(dd, registration::setTalks)),
                Map.entry(ADDITIONAL_KEY_NOTES, (dt, dd) -> parseString(dd, registration::setNotes))
        ));
    }

    private static void parsePaymentType(@Nullable Element element, @NonNull Consumer<Registration.Payment.Type> action) {
        if (element == null) return;

        try {
            String text = element.text();

            if (text.contains(PAYMENT_TYPE_EXPENSE)) {
                action.accept(Registration.Payment.Type.EXPENSE);
            } else if (text.contains(PAYMENT_TYPE_TRANSFER)) {
                action.accept(Registration.Payment.Type.TRANSFER);
            }
        } catch (Exception ignored) {
            // ignored
        }
    }

    private static void parsePaymentCategory(@Nullable Element element, @NonNull Consumer<Registration.Payment.Category> action) {
        if (element == null) return;

        try {
            String text = element.text();

            if (text.contains(PAYMENT_CATEGORY_SHOPPING)) {
                action.accept(Registration.Payment.Category.SHOPPING);
            } else if (text.contains(PAYMENT_CATEGORY_TALK_GIFT)) {
                action.accept(Registration.Payment.Category.TALK_GIFT);
            } else if (text.contains(PAYMENT_CATEGORY_DEPOSIT)) {
                action.accept(Registration.Payment.Category.DEPOSIT);
            } else if (text.contains(PAYMENT_CATEGORY_ORGANIZER_GIFT)) {
                action.accept(Registration.Payment.Category.ORGANIZER_GIFT);
            } else if (text.contains(PAYMENT_CATEGORY_TRANSPORTATION)) {
                action.accept(Registration.Payment.Category.TRANSPORTATION);
            } else if (text.contains(PAYMENT_CATEGORY_TALK_MATERIALS)) {
                action.accept(Registration.Payment.Category.TALK_MATERIALS);
            }
        } catch (Exception ignored) {
            // ignored
        }
    }
}
