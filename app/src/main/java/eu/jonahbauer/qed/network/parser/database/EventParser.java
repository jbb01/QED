package eu.jonahbauer.qed.network.parser.database;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import eu.jonahbauer.qed.model.Event;
import eu.jonahbauer.qed.model.Registration;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.Map;

public final class EventParser extends DatabaseParser<Event> {
    private static final String LOG_TAG = EventParser.class.getName();

    private static final String GENERAL_KEY_START = "Beginn";
    private static final String GENERAL_KEY_END = "Ende";
    private static final String GENERAL_KEY_DEADLINE = "Anmeldeschluss";
    private static final String GENERAL_KEY_COST = "Kosten";
    private static final String GENERAL_KEY_MAX_PARTICIPANTS = "Teilnehmerlimit";
    private static final String GENERAL_KEY_HOTEL = "Unterkunft";
    private static final String GENERAL_KEY_HOTEL_ADDRESS = "Adresse der Unterkunft";
    private static final String GENERAL_KEY_EMAIL_ORGA = "Email an Orgas";
    private static final String GENERAL_KEY_EMAIL_ALL = "Email an Teilnehmer";
    private static final String GENERAL_KEY_NOTES = "Anmerkungen";
    private static final String GENERAL_KEY_PAYMENT_REFERENCE = "Verwendungszweck";

    private static final String REGISTRATIONS_KEY_PARTICIPANTS = "Teilnehmerliste";

    public static final EventParser INSTANCE = new EventParser();

    private EventParser() {}

    @NonNull
    @Override
    protected Event parse(@NonNull Event event, @NonNull Document document) {
        event.setTitle(document.selectFirst("#event h2").text());

        parseGeneral(event, document.selectFirst("#event_general_information"));
        parseRegistrations(event, document.selectFirst("#event_registrations"));

        return event;
    }

    private void parseGeneral(@NonNull Event event, @Nullable Element element) {
        parseDefinitionList(element, Map.ofEntries(
                Map.entry(GENERAL_KEY_START, (dt, dd) -> parseLocalDate(dd, event::setStart)),
                Map.entry(GENERAL_KEY_END, (dt, dd) -> parseLocalDate(dd, event::setEnd)),
                Map.entry(GENERAL_KEY_DEADLINE, (dt, dd) -> parseLocalDate(dd, event::setDeadline)),
                Map.entry(GENERAL_KEY_COST, (dt, dd) -> parseCurrency(dd, event::setCost)),
                Map.entry(GENERAL_KEY_PAYMENT_REFERENCE, (dt, dd) -> parseString(dd, event::setPaymentReference)),
                Map.entry(GENERAL_KEY_MAX_PARTICIPANTS, (dt, dd) -> parseInteger(dd, event::setMaxParticipants)),
                Map.entry(GENERAL_KEY_HOTEL, (dt, dd) -> parseString(dd, event::setHotel)),
                Map.entry(GENERAL_KEY_HOTEL_ADDRESS, (dt, dd) -> parseString(dd, event::setHotelAddress)),
                Map.entry(GENERAL_KEY_EMAIL_ORGA, (dt, dd) -> parseString(dd, event::setEmailOrga)),
                Map.entry(GENERAL_KEY_EMAIL_ALL, (dt, dd) -> parseString(dd, event::setEmailAll)),
                Map.entry(GENERAL_KEY_NOTES, (dt, dd) -> parseString(dd, event::setNotes))
        ));
    }

    private void parseRegistrations(@NonNull Event event, @Nullable Element element) {
        parseDefinitionList(element, Map.ofEntries(
                Map.entry(REGISTRATIONS_KEY_PARTICIPANTS, (dt, dd) -> {
                    for (var li : dd.select("li")) {
                        try {
                            Element a = li.selectFirst("a");
                            if (a == null) continue;

                            long id = parseIdFromHref(a, Registration.NO_ID);
                            Registration registration = new Registration(id);
                            registration.setEvent(event);

                            parseString(a, registration::setPersonName);
                            parseRegistrationStatus(li.selectFirst("i"), (status, orga) -> {
                                registration.setStatus(status);
                                registration.setOrganizer(orga);
                            });

                            event.getParticipants().remove(registration);
                            event.getParticipants().add(registration);
                        } catch (Exception e) {
                            Log.e(LOG_TAG, "Error parsing event " + event.getId() + ".", e);
                        }
                    }
                })
        ));
    }
}
