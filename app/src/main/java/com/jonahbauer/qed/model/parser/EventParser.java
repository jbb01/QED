package com.jonahbauer.qed.model.parser;

import android.util.Log;

import androidx.annotation.NonNull;

import com.jonahbauer.qed.Application;
import com.jonahbauer.qed.model.Event;
import com.jonahbauer.qed.model.Registration;
import com.jonahbauer.qed.networking.parser.HtmlParser;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class EventParser extends HtmlParser<Event> {
    private static final Pattern COST_PATTERN = Pattern.compile("(\\d+(?:,\\d+))\\s*€");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\d+");

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
                       if (value.selectFirst("i") != null) return;

                       switch (key) {
                           case "Beginn:": {
                               Element time = value.child(0);
                               event.setStartString(time.text());
                               event.setStart(parseDate(time.attr("datetime")));
                               break;
                           }
                           case "Ende:": {
                               Element time = value.child(0);
                               event.setEndString(time.text());
                               event.setEnd(parseDate(time.attr("datetime")));
                               break;
                           }
                           case "Anmeldeschluss:": {
                               Element time = value.child(0);
                               event.setDeadlineString(time.text());
                               event.setDeadline(parseDate(time.attr("datetime")));
                               break;
                           }
                           case "Kosten:": {
                               String cost = value.text();
                               Matcher matcher = COST_PATTERN.matcher(cost);
                               if (matcher.find()) {
                                   event.setCost((int) Double.parseDouble(matcher.group(1).replaceAll(",", ".")));
                               }
                               break;
                           }
                           case "Teilnehmerlimit:": {
                               String limit = value.text();
                               Matcher matcher = NUMBER_PATTERN.matcher(limit);
                               if (matcher.find()) {
                                   event.setMaxParticipants(Integer.parseInt(matcher.group()));
                               }
                               break;
                           }
                           case "Unterkunft:": {
                               String hotel = value.child(0).text();
                               event.setHotel(hotel);
                               break;
                           }
                           case "Adresse der Unterkunft:": {
                               String hotelAddress = value.text();
                               event.setHotelAddress(hotelAddress);
                               break;
                           }
                           case "Email an Orgas:": {
                               String mail = value.child(0).text();
                               event.setEmailOrga(mail);
                               break;
                           }
                           case "Email an Teilnehmer:": {
                               String mail = value.child(0).text();
                               event.setEmailAll(mail);
                               break;
                           }
                           case "Anmerkungen:": {
                               String notes = value.text();
                               event.setNotes(notes);
                               break;
                           }
                           default: {
                               break;
                           }
                       }
                   } catch (Exception e) {
                       Log.e(Application.LOG_TAG_ERROR, "Error parsing event " + event.getId() + ".", e);
                   }
               });
    }

    private void parseRegistrations(Event event, Element element) {
        if (element == null) return;
        element.select("dl dt")
               .forEach(dt -> {
                   String key = dt.text();
                   if ("Teilnehmerliste:".equals(key)) {
                       Elements divs = dt.nextElementSibling().select("li div");
                       divs.forEach(div -> {
                           try {
                               long id = Long.parseLong(div.child(0).attr("href").substring("/registrations/".length()));
                               String participant = div.child(0).text();

                               Element statusElement = div.selectFirst("i");
                               String statusString = statusElement != null ? statusElement.text() : "";

                               boolean orga = statusString.contains("Orga");

                               Registration.Status status = Registration.Status.CONFIRMED;
                               if (statusString.contains("abgesagt")) {
                                   status = Registration.Status.CANCELLED;
                               } else if (statusString.contains("offen")) {
                                   status = Registration.Status.OPEN;
                               }

                               Registration registration = new Registration(id);
                               registration.setStatus(status);
                               registration.setOrganizer(orga);

                               event.getParticipants().put(participant, registration);
                           } catch (Exception e) {
                               Log.e(Application.LOG_TAG_ERROR, "Error parsing event " + event.getId() + ".", e);
                           }
                       });
                   }
               });
    }
}
