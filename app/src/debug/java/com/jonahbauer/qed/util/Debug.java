package com.jonahbauer.qed.util;

import androidx.core.util.Pair;

import com.jonahbauer.qed.model.Album;
import com.jonahbauer.qed.model.Event;
import com.jonahbauer.qed.model.Image;
import com.jonahbauer.qed.model.Message;
import com.jonahbauer.qed.model.Person;
import com.jonahbauer.qed.model.Registration;

import java.time.*;

import com.jonahbauer.qed.model.Registration.Status;
import com.jonahbauer.qed.model.util.ParsedInstant;
import com.jonahbauer.qed.model.util.ParsedLocalDate;
import lombok.experimental.UtilityClass;

@UtilityClass
public class Debug {
    public static Person dummyPerson() {
        Person person = new Person(Person.NO_ID);
        person.setFirstName("Max");
        person.setLastName("Mustermann");
        person.setUsername("MaxMustermann");
        person.setEmail("max.mustermann@example.org");
        person.setBirthday(new ParsedLocalDate("01.01.2000", LocalDate.of(2000, 1, 1)));
        person.setHomeStation("Musterstadt");
        person.setRailcard("Bahncard 42");
        person.setFood("Nur leckeres Essen");
        person.setNotes("Raum für Notizen");
        person.setMember(true);
        person.setActive(true);
        person.setDateOfJoining(new ParsedLocalDate("03/14/2020", LocalDate.of(2020, 3, 2)));
        person.setDateOfQuitting(new ParsedLocalDate("31-12-2050", LocalDate.of(2050, 12, 31)));
        person.getContacts().add(Pair.create("mobil", "0123456789"));
        person.getContacts().add(Pair.create("daheim", "987654321"));
        person.getContacts().add(Pair.create("skype", "max.mustermann"));
        person.getAddresses().add("Musterstraße 10\n12345 Musterstadt");
        person.getAddresses().add("Mustergasse 5\n54321 Musterdorf");

        Registration registration;

        registration = new Registration(Registration.NO_ID);
        registration.setStatus(Status.PENDING);
        registration.setEventTitle("Akademie 2021");
        registration.setPerson(person);
        person.getEvents().add(registration);

        registration = new Registration(Registration.NO_ID);
        registration.setStatus(Status.CONFIRMED);
        registration.setEventTitle("Musterstadt 2020");
        registration.setPerson(person);
        person.getEvents().add(registration);

        registration = new Registration(Registration.NO_ID);
        registration.setStatus(Status.CANCELLED);
        registration.setEventTitle("Musterstadt 2019");
        registration.setPerson(person);
        person.getEvents().add(registration);

        registration = new Registration(Registration.NO_ID);
        registration.setStatus(Status.CONFIRMED);
        registration.setOrganizer(true);
        registration.setEventTitle("Testseminar");
        registration.setPerson(person);
        person.getEvents().add(registration);

        registration = new Registration(Registration.NO_ID);
        registration.setStatus(Status.REJECTED);
        registration.setEventTitle("Musterdorf 2017");
        registration.setPerson(person);
        person.getEvents().add(registration);

        return person;
    }

    public static Event dummyEvent() {
        Event event = new Event(Event.NO_ID);
        event.setTitle("Musterveranstaltung");
        event.setCost(200d);
        event.setNotes("Raum für Notizen");
        event.setMaxParticipants(50);
        event.setStart(new ParsedLocalDate("01.08.2021", LocalDate.of(2021, 8, 1)));
        event.setEnd(new ParsedLocalDate("10.08.2021", LocalDate.of(2021, 8, 10)));
        event.setDeadline(new ParsedLocalDate("16.07.2021", LocalDate.of(2021, 7, 16)));

        event.setHotel("Musterunterkunft");
        event.setHotelAddress("Musterstraße 10\n12345 Musterstadt");

        event.setEmailAll("musterveranstaltung-teilnehmer@example.org");
        event.setEmailOrga("musterveranstaltung@example.org");

        Registration registration;
        registration = new Registration(Registration.NO_ID);
        registration.setStatus(Status.CONFIRMED);
        registration.setOrganizer(true);
        registration.setPersonName("Max Mustermann");
        registration.setEvent(event);
        event.getParticipants().add(registration);

        registration = new Registration(Registration.NO_ID);
        registration.setStatus(Status.PENDING);
        registration.setOrganizer(true);
        registration.setPersonName("Erika Musterfrau");
        registration.setEvent(event);
        event.getParticipants().add(registration);

        for (int i = 0; i < 10; i++) {
            registration = new Registration(Registration.NO_ID);
            registration.setStatus(Status.values()[(int) (Math.random() * Status.values().length)]);
            registration.setPersonName("Testteilnehmer " + i);
            registration.setEvent(event);
            event.getParticipants().add(registration);
        }

        return event;
    }

    public static Album dummyAlbum() {
        Album album = new Album(Album.NO_ID);
        album.setName("Album");
        album.setOwner("Max Mustermann");
        album.setCreationDate("01.01.2000");
        album.setPrivate(true);
        album.setLoaded(Instant.now());
        album.getCategories().add("Kategorie 1");
        album.getCategories().add("Kategorie 2");
        album.getCategories().add("Kategorie 3");
        album.getCategories().add("Sonstige");
        album.getPersons().add(dummyPerson());
        for (int i = 1; i < 6; i++) {
            album.getDates().add(LocalDate.of(2000, 1, i));
        }
        for (int i = 0; i < 10; i++) {
            album.getImages().add(new Image(Image.NO_ID));
        }
        return album;
    }

    public static Message dummyMessage() {
        return new Message(
                1_000_007,
                "Max Mustermann",
                loremIpsum(),
                ZonedDateTime.of(2000, 1, 1, 13, 37, 0, 0, ZoneId.of("Europe/Berlin")).toInstant(),
                1337,
                "Max Mustermann",
                "FF0000",
                "",
                0
        );
    }

    public static Registration dummyRegistration() {
        var registration = new Registration(Registration.NO_ID);
        registration.setStatus(Status.PENDING);
        registration.setOrganizer(true);

        registration.setEventId(Event.NO_ID);
        registration.setEventTitle("Musterveranstaltung");

        registration.setPersonId(Person.NO_ID);
        registration.setPersonName("Max Mustermann");
        registration.setPersonBirthday(new ParsedLocalDate("01.01.2000", LocalDate.now()));
        registration.setPersonGender(Person.Gender.MALE);
        registration.setPersonMail("max.mustermann@example.com");
        registration.setPersonAddress("Musterstraße 10\n12345 Musterstadt");
        registration.setPersonPhone("0123456789");

        registration.setTimeOfArrival(new ParsedInstant("01.01.2020 00:00", LocalDate.of(2020, 1, 1).atStartOfDay(ZoneId.of("Europe/Berlin")).toInstant()));
        registration.setTimeOfDeparture(new ParsedInstant("04.01.2020 00:00", LocalDate.of(2020, 1, 4).atStartOfDay(ZoneId.of("Europe/Berlin")).toInstant()));
        registration.setSourceStation("Musterstadt HBF");
        registration.setTargetStation("Musterstadt HBF");
        registration.setRailcard("50");
        registration.setOvernightStays(3);

        registration.setPaymentAmount(100d);
        registration.setPaymentDone(true);
        registration.setPaymentTime(new ParsedLocalDate("30.01.2020", LocalDate.of(2020, 1, 30)));
        registration.setMemberAbatement(true);
        registration.setOtherAbatement(null);

        registration.setLoaded(Instant.now());

        return registration;
    }

    public static String loremIpsum() {
        return "Lorem ipsum \\(1+1=2\\) dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. \n" +
                "\n" +
                "Duis autem vel eum iriure dolor in hendrerit in vulputate velit esse molestie consequat, vel illum dolore eu feugiat nulla facilisis at vero eros et accumsan et iusto odio dignissim qui blandit praesent luptatum zzril delenit augue duis dolore te feugait nulla facilisi. Lorem ipsum dolor sit amet, consectetuer adipiscing elit, sed diam nonummy nibh euismod tincidunt ut laoreet dolore magna aliquam erat volutpat. \n" +
                "\n" +
                "Ut wisi enim ad minim veniam, quis nostrud exerci tation ullamcorper suscipit lobortis nisl ut aliquip ex ea commodo consequat. Duis autem vel eum iriure dolor in hendrerit in vulputate velit esse molestie consequat, vel illum dolore eu feugiat nulla facilisis at vero eros et accumsan et iusto odio dignissim qui blandit praesent luptatum zzril delenit augue duis dolore te feugait nulla facilisi. \n" +
                "\n" +
                "Nam liber tempor cum soluta nobis eleifend option congue nihil imperdiet doming id quod mazim placerat facer possim assum. Lorem ipsum dolor sit amet, consectetuer adipiscing elit, sed diam nonummy nibh euismod tincidunt ut laoreet dolore magna aliquam erat volutpat. Ut wisi enim ad minim veniam, quis nostrud exerci tation ullamcorper suscipit lobortis nisl ut aliquip ex ea commodo consequat. \n" +
                "\n" +
                "Duis autem vel eum iriure dolor in hendrerit in vulputate velit esse molestie consequat, vel illum dolore eu feugiat nulla facilisis. \n" +
                "\n" +
                "At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, At accusam aliquyam diam diam dolore dolores duo eirmod eos erat, et nonumy sed tempor et et invidunt justo labore Stet clita ea et gubergren, kasd magna no rebum. sanctus sea sed takimata ut vero voluptua. est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat. \n" +
                "\n" +
                "Consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus. \n" +
                "\n" +
                "Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. \n" +
                "\n" +
                "Duis autem vel eum iriure dolor in hendrerit in vulputate velit esse molestie consequat, vel illum dolore eu feugiat nulla facilisis at vero eros et accumsan et iusto odio dignissim qui blandit praesent luptatum zzril delenit augue duis dolore te feugait nulla facilisi. Lorem ipsum dolor sit amet, consectetuer adipiscing elit, sed diam nonummy nibh euismod tincidunt ut laoreet dolore magna aliquam erat volutpat. \n" +
                "\n" +
                "Ut wisi enim ad minim veniam, quis nostrud exerci tation ullamcorper suscipit lobortis nisl ut aliquip ex ea commodo consequat. Duis autem vel eum iriure dolor in hendrerit in vulputate velit esse molestie consequat, vel illum dolore eu feugiat nulla facilisis at vero eros et accumsan et iusto odio dignissim qui blandit praesent luptatum zzril delenit augue duis dolore te feugait nulla facilisi. ";
    }
}
