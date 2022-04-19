package com.jonahbauer.qed.model.parser;

import static com.jonahbauer.qed.model.parser.PersonParser.parseAddress;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jonahbauer.qed.model.Registration;
import com.jonahbauer.qed.networking.parser.HtmlParseException;
import com.jonahbauer.qed.networking.parser.HtmlParser;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class RegistrationParser extends HtmlParser<Registration> {
    private static final String LOG_TAG = RegistrationParser.class.getName();

    private static final Pattern COST_PATTERN = Pattern.compile("(\\d+(?:,\\d+)?)\\s*€");

    private static final String GENERAL_KEY_EVENT = "Veranstaltung:";
    private static final String GENERAL_KEY_PERSON = "Person:";
    private static final String GENERAL_KEY_ORGANIZER = "Organisator:";
    private static final String GENERAL_KEY_STATUS = "Anmeldestatus:";
    private static final String GENERAL_KEY_BIRTHDAY = "Geburtstag:";
    private static final String GENERAL_KEY_GENDER = "Geschlecht:";
    private static final String GENERAL_KEY_MAIL = "Emailadresse:";
    private static final String GENERAL_KEY_ADDRESS = "Adresse:";
    private static final String GENERAL_KEY_PHONE = "Handynummer:";

    private static final String TRANSPORT_KEY_TIME_OF_ARRIVAL = "Ankunftszeit:";
    private static final String TRANSPORT_KEY_TIME_OF_DEPARTURE = "Abfahrtszeit:";
    private static final String TRANSPORT_KEY_SOURCE_STATION = "Anreisebahnhof:";
    private static final String TRANSPORT_KEY_TARGET_STATION = "Abfahrtsbahnhof:";
    private static final String TRANSPORT_KEY_RAILCARD = "Bahncard:";
    private static final String TRANSPORT_KEY_OVERNIGHT_STAYS = "Übernachtungen:";

    private static final String PAYMENT_KEY_AMOUNT = "Gezahlter Betrag:";
    private static final String PAYMENT_KEY_FULL = "Vollständig Bezahlt:";
    private static final String PAYMENT_KEY_TIMESTAMP = "Geldeingang:";
    private static final String PAYMENT_KEY_MEMBERSHIP_ABATEMENT = "Mitgliedsermäßigung:";
    private static final String PAYMENT_KEY_OTHER_ABATEMENT = "Sonstige Ermäßigungen:";

    private static final String ADDITIONAL_KEY_FOOD = "Essenswünsche:";
    private static final String ADDITIONAL_KEY_TALKS = "Vorträge:";
    private static final String ADDITIONAL_KEY_NOTES = "Anmerkungen:";

    private static final String STATUS_CANCELLED = "abgesagt";
    private static final String STATUS_OPEN = "offen";
    private static final String STATUS_CONFIRMED = "bestätigt";

    public static final RegistrationParser INSTANCE = new RegistrationParser();

    private RegistrationParser() {}

    @NonNull
    @Override
    protected Registration parse(@NonNull Registration registration, Document document) {
        parseGeneral(registration, document.selectFirst("#registration_general_information"));
        parseStatus(registration, document.selectFirst("#registration_status"));
        parseTransport(registration, document.selectFirst("#registration_transport"));
        parseAdditional(registration, document.selectFirst("#registration_additional"));

        return registration;
    }

    private void parseGeneral(Registration registration, Element element) {
        if (element == null) return;
        element.select("dl dt")
                .forEach(dt -> {
                    try {
                        String key = dt.text();
                        Element value = dt.nextElementSibling();
                        //noinspection ConstantConditions
                        if (value.selectFirst("i") != null) return;

                        switch (key) {
                            case GENERAL_KEY_EVENT: {
                                var eventElement = value.selectFirst("a");
                                if (eventElement == null) throw new HtmlParseException("Registration " + registration.getId() + " does not seem to contain an event.");
                                var id = Long.parseLong(eventElement.attr("href").substring("/events/".length()));
                                registration.setEventId(id);
                                registration.setEventTitle(eventElement.text());
                                break;
                            }
                            case GENERAL_KEY_PERSON: {
                                var personElement = value.selectFirst("a");
                                if (personElement == null) throw new HtmlParseException("Registration " + registration.getId() + " does not seem to contain a person.");
                                var id = Long.parseLong(personElement.attr("href").substring("/people/".length()));
                                registration.setPersonId(id);
                                registration.setPersonName(personElement.text());
                                break;
                            }
                            case GENERAL_KEY_ORGANIZER: {
                                registration.setOrganizer(parseBoolean(value));
                                break;
                            }
                            case GENERAL_KEY_STATUS: {
                                registration.setStatus(parseRegistrationStatus(value.text()));
                                break;
                            }
                            case GENERAL_KEY_BIRTHDAY: {
                                var birthdayString = value.text();
                                registration.setPersonBirthdayString(birthdayString);
                                registration.setPersonBirthday(parseLocalDate(birthdayString));
                                break;
                            }
                            case GENERAL_KEY_GENDER: {
                                var gender = value.text();
                                registration.setPersonGender(gender);
                                break;
                            }
                            case GENERAL_KEY_MAIL: {
                                var mailElement = value.selectFirst("a");
                                if (mailElement == null) break;
                                registration.setPersonMail(mailElement.text());
                                break;
                            }
                            case GENERAL_KEY_ADDRESS: {
                                var addressElement = value.selectFirst(".address");
                                if (addressElement == null) break;
                                registration.setPersonAddress(parseAddress(addressElement));
                                break;
                            }
                            case GENERAL_KEY_PHONE: {
                                var phoneElement = value.selectFirst("a");
                                if (phoneElement == null) break;
                                registration.setPersonPhone(phoneElement.text());
                                break;
                            }
                        }
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "Error parsing registration " + registration.getId() + ".", e);
                        if (e instanceof HtmlParseException) throw e;
                    }
                });
    }

    private void parseStatus(Registration registration, Element element) {
        if (element == null) return;
        element.select("dl dt")
                .forEach(dt -> {
                    try {
                        String key = dt.text();
                        Element value = dt.nextElementSibling();
                        //noinspection ConstantConditions
                        if (value.selectFirst("i") != null) return;

                        switch (key) {
                            case PAYMENT_KEY_AMOUNT: {
                                String cost = value.text();
                                Matcher matcher = COST_PATTERN.matcher(cost);
                                if (matcher.find()) {
                                    //noinspection ConstantConditions
                                    registration.setPaymentAmount(Double.parseDouble(matcher.group(1).replace(',', '.')));
                                }
                                break;
                            }
                            case PAYMENT_KEY_FULL: {
                                registration.setPaymentDone(parseBoolean(value));
                                break;
                            }
                            case PAYMENT_KEY_TIMESTAMP: {
                                var time = value.child(0);
                                registration.setPaymentTimeString(time.text());
                                registration.setPaymentTime(parseLocalDate(time));
                                break;
                            }
                            case PAYMENT_KEY_MEMBERSHIP_ABATEMENT: {
                                registration.setMemberAbatement(parseBoolean(value));
                                break;
                            }
                            case PAYMENT_KEY_OTHER_ABATEMENT: {
                                registration.setOtherAbatement(value.text());
                                break;
                            }
                        }
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "Error parsing registration " + registration.getId() + ".", e);
                        if (e instanceof HtmlParseException) throw e;
                    }
                });
    }

    private void parseTransport(Registration registration, Element element) {
        if (element == null) return;
        element.select("dl dt")
                .forEach(dt -> {
                    try {
                        String key = dt.text();
                        Element value = dt.nextElementSibling();
                        //noinspection ConstantConditions
                        if (value.selectFirst("i") != null) return;

                        switch (key) {
                            case TRANSPORT_KEY_TIME_OF_ARRIVAL: {
                                var time = value.child(0);
                                registration.setTimeOfArrivalString(time.text());
                                registration.setTimeOfArrival(parseInstant(time));
                                break;
                            }
                            case TRANSPORT_KEY_TIME_OF_DEPARTURE: {
                                var time = value.child(0);
                                registration.setTimeOfDepartureString(time.text());
                                registration.setTimeOfDeparture(parseInstant(time));
                                break;
                            }
                            case TRANSPORT_KEY_SOURCE_STATION: {
                                registration.setSourceStation(value.text());
                                break;
                            }
                            case TRANSPORT_KEY_TARGET_STATION: {
                                registration.setTargetStation(value.text());
                                break;
                            }
                            case TRANSPORT_KEY_RAILCARD: {
                                registration.setRailcard(value.text());
                                break;
                            }
                            case TRANSPORT_KEY_OVERNIGHT_STAYS: {
                                registration.setOvernightStays(parseInteger(value));
                            }
                        }
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "Error parsing registration " + registration.getId() + ".", e);
                    }
                });
    }

    private void parseAdditional(Registration registration, Element element) {
        if (element == null) return;
        element.select("dl dt")
                .forEach(dt -> {
                    try {
                        String key = dt.text();
                        Element value = dt.nextElementSibling();
                        //noinspection ConstantConditions
                        if (value.selectFirst("i") != null) return;

                        switch (key) {
                            case ADDITIONAL_KEY_FOOD: {
                                registration.setFood(value.text());
                                break;
                            }
                            case ADDITIONAL_KEY_TALKS: {
                                registration.setTalks(value.text());
                                break;
                            }
                            case ADDITIONAL_KEY_NOTES: {
                                registration.setNotes(value.text());
                                break;
                            }
                        }
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "Error parsing registration " + registration.getId() + ".", e);
                    }
                });
    }

    @Nullable
    public static Registration.Status parseRegistrationStatus(String status) {
        switch (status) {
            case STATUS_OPEN:
                return Registration.Status.OPEN;
            case STATUS_CONFIRMED:
                return Registration.Status.CONFIRMED;
            case STATUS_CANCELLED:
                return Registration.Status.CANCELLED;
        }
        return null;
    }
}