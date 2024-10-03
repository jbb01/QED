package eu.jonahbauer.qed.model.parser;

import android.util.Log;

import androidx.annotation.NonNull;

import androidx.annotation.Nullable;
import eu.jonahbauer.qed.model.Event;
import eu.jonahbauer.qed.model.Registration;

import eu.jonahbauer.qed.networking.parser.HtmlParser;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class EventParser extends DatabaseParser<Event> {
    private static final String LOG_TAG = EventParser.class.getName();

    private static final Pattern COST_PATTERN = Pattern.compile("((?:\\d+\\.)*\\d+(?:,\\d+)?)\\s*€");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\d+");

    private static final String GENERAL_KEY_START = "Beginn:";
    private static final String GENERAL_KEY_END = "Ende:";
    private static final String GENERAL_KEY_DEADLINE = "Anmeldeschluss:";
    private static final String GENERAL_KEY_COST = "Kosten:";
    private static final String GENERAL_KEY_MAX_PARTICIPANTS = "Teilnehmerlimit:";
    private static final String GENERAL_KEY_HOTEL = "Unterkunft:";
    private static final String GENERAL_KEY_HOTEL_ADDRESS = "Adresse der Unterkunft:";
    private static final String GENERAL_KEY_EMAIL_ORGA = "Email an Orgas:";
    private static final String GENERAL_KEY_EMAIL_ALL = "Email an Teilnehmer:";
    private static final String GENERAL_KEY_NOTES = "Anmerkungen:";
    private static final String GENERAL_KEY_PAYMENT_REFERENCE = "Verwendungszweck";

    private static final String REGISTRATIONS_KEY_ORGA = "Orga";

    public static final EventParser INSTANCE = new EventParser();

    private EventParser() {}

    @NonNull
    @Override
    protected Event parse(@NonNull Event event, Document document) {
        event.setTitle(document.selectFirst("#event h2").text());

        parseGeneral(event, document.selectFirst("#event_general_information"));
        parseRegistrations(event, document.selectFirst("#event_registrations"));

        return event;
    }

    private void parseGeneral(Event event, Element element) {
        if (element == null) return;
        element.select("dl dt")
               .forEach(dt -> {
                   try {
                       String key = dt.text();
                       Element value = dt.nextElementSibling();
                       //noinspection ConstantConditions
                       if (value.selectFirst("i") != null) return;

                       switch (key) {
                           case GENERAL_KEY_START: {
                               Element time = value.child(0);
                               event.setStart(HtmlParser.parseLocalDate(time));
                               break;
                           }
                           case GENERAL_KEY_END: {
                               Element time = value.child(0);
                               event.setEnd(HtmlParser.parseLocalDate(time));
                               break;
                           }
                           case GENERAL_KEY_DEADLINE: {
                               Element time = value.child(0);
                               event.setDeadline(HtmlParser.parseLocalDate(time));
                               break;
                           }
                           case GENERAL_KEY_COST: {
                               event.setCost(parseCost(value));
                               break;
                           }
                           case GENERAL_KEY_PAYMENT_REFERENCE: {
                               String paymentReference = value.text();
                               event.setPaymentReference(paymentReference);
                               break;
                           }
                           case GENERAL_KEY_MAX_PARTICIPANTS: {
                               String limit = value.text();
                               Matcher matcher = NUMBER_PATTERN.matcher(limit);
                               if (matcher.find()) {
                                   event.setMaxParticipants(HtmlParser.parseInteger(matcher.group()));
                               }
                               break;
                           }
                           case GENERAL_KEY_HOTEL: {
                               String hotel = value.child(0).text();
                               event.setHotel(hotel);
                               break;
                           }
                           case GENERAL_KEY_HOTEL_ADDRESS: {
                               String hotelAddress = value.text();
                               event.setHotelAddress(hotelAddress);
                               break;
                           }
                           case GENERAL_KEY_EMAIL_ORGA: {
                               String mail = value.child(0).text();
                               event.setEmailOrga(mail);
                               break;
                           }
                           case GENERAL_KEY_EMAIL_ALL: {
                               String mail = value.child(0).text();
                               event.setEmailAll(mail);
                               break;
                           }
                           case GENERAL_KEY_NOTES: {
                               String notes = value.text();
                               event.setNotes(notes);
                               break;
                           }
                           default: {
                               break;
                           }
                       }
                   } catch (Exception e) {
                       Log.e(LOG_TAG, "Error parsing event " + event.getId() + ".", e);
                   }
               });
    }

    private void parseRegistrations(Event event, Element element) {
        if (element == null) return;
        element.select("dl dt")
               .forEach(dt -> {
                   try {
                       String key = dt.text();
                       if ("Teilnehmerliste:".equals(key)) {
                           //noinspection ConstantConditions
                           Elements divs = dt.nextElementSibling().select("li div");
                           divs.forEach(div -> {
                               try {
                                   long id = parseIdFromHref(div.child(0), Registration.NO_ID);
                                   String participant = div.child(0).text();

                                   var statusElement = div.selectFirst("i");
                                   var statusString = statusElement != null ? statusElement.text() : "";
                                   var orga = statusString.contains(REGISTRATIONS_KEY_ORGA);
                                   var status = RegistrationStatusParser.INSTANCE.parse(statusString);

                                   Registration registration = new Registration(id);
                                   registration.setStatus(status);
                                   registration.setOrganizer(orga);
                                   registration.setEvent(event);
                                   registration.setPersonName(participant);

                                   event.getParticipants().remove(registration);
                                   event.getParticipants().add(registration);
                               } catch (Exception e) {
                                   Log.e(LOG_TAG, "Error parsing event " + event.getId() + ".", e);
                               }
                           });
                       }
                   } catch (Exception e) {
                       Log.e(LOG_TAG, "Error parsing event " + event.getId() + ".", e);
                   }
               });
    }

    @Nullable
    static Double parseCost(@NonNull Element element) {
        Matcher matcher = COST_PATTERN.matcher(element.text());
        if (matcher.find() && matcher.groupCount() > 0) {
            String match = matcher.group(1);
            assert match != null;

            match = match.replace(".","");
            match = match.replace(',', '.');
            return HtmlParser.parseDouble(match);
        }
        return null;
    }
}
