package com.jonahbauer.qed.util;

import androidx.core.util.Pair;

import com.jonahbauer.qed.model.Album;
import com.jonahbauer.qed.model.Event;
import com.jonahbauer.qed.model.Image;
import com.jonahbauer.qed.model.Message;
import com.jonahbauer.qed.model.Person;
import com.jonahbauer.qed.model.Registration;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Debug {
    public static Person dummyPerson() {
        Person person = new Person(-1);
        person.setFirstName("Max");
        person.setLastName("Mustermann");
        person.setUsername("MaxMustermann");
        person.setEmail("max.mustermann@example.org");
        person.setBirthday(LocalDate.of(2000, 1, 1));
        person.setBirthdayString("01.01.2000");
        person.setHomeStation("Musterstadt");
        person.setRailcard("Bahncard 42");
        person.setFood("Nur leckeres Essen");
        person.setNotes("Raum für Notizen");
        person.setMember(true);
        person.setActive(true);
        person.setDateOfJoining(LocalDate.of(2020, 3, 2));
        person.setDateOfJoiningString("03/14/2020");
        person.setLeavingDate(LocalDate.of(2050, 12, 31));
        person.setLeavingDateString("31-12-2050");
        person.getContacts().add(Pair.create("mobil", "0123456789"));
        person.getContacts().add(Pair.create("daheim", "987654321"));
        person.getContacts().add(Pair.create("skype", "max.mustermann"));
        person.getAddresses().add("Musterstraße 10\n12345 Musterstadt");
        person.getAddresses().add("Mustergasse 5\n54321 Musterdorf");

        Registration registration;

        registration = new Registration(-1);
        registration.setStatus(Registration.Status.OPEN);
        person.getEvents().put("Akademie 2021", registration);

        registration = new Registration(-1);
        registration.setStatus(Registration.Status.CONFIRMED);
        person.getEvents().put("Musterstadt 2020", registration);

        registration = new Registration(-1);
        registration.setStatus(Registration.Status.CANCELLED);
        person.getEvents().put("Musterstadt 2019", registration);

        registration = new Registration(-1);
        registration.setStatus(Registration.Status.CONFIRMED);
        registration.setOrganizer(true);
        person.getEvents().put("Testseminar", registration);

        return person;
    }

    public static Event dummyEvent() {
        Event event = new Event(-1);
        event.setTitle("Musterveranstaltung");
        event.setCost(200);
        event.setNotes("Raum für Notizen");
        event.setMaxParticipants(50);

        event.setStart(LocalDate.of(2021, 8, 1));
        event.setStartString("01.08.2021");

        event.setEnd(LocalDate.of(2021, 8, 10));
        event.setEndString("10.08.2021");

        event.setDeadline(LocalDate.of(2021, 7, 16));
        event.setDeadlineString("16.07.2021");

        event.setHotel("Musterunterkunft");
        event.setHotelAddress("Musterstraße 10\n12345 Musterstadt");

        event.setEmailAll("musterveranstaltung-teilnehmer@example.org");
        event.setEmailOrga("musterveranstaltung@example.org");

        Registration registration;
        registration = new Registration(-1);
        registration.setStatus(Registration.Status.CONFIRMED);
        registration.setOrganizer(true);
        event.getParticipants().put("Max Mustermann", registration);

        registration = new Registration(-1);
        registration.setStatus(Registration.Status.OPEN);
        registration.setOrganizer(true);
        event.getParticipants().put("Erika Musterfrau", registration);

        for (int i = 0; i < 10; i++) {
            registration = new Registration(-1);
            registration.setStatus(
                    Math.random() > 0.5 ? Registration.Status.OPEN
                            : Math.random() > 0 ? Registration.Status.CONFIRMED
                            : Registration.Status.CANCELLED
            );
            event.getParticipants().put("Testteilnehmer " + i, registration);
        }

        return event;
    }

    public static Album dummyAlbum() {
        Album album = new Album(-1);
        album.setName("Album");
        album.setOwner("Max Mustermann");
        album.setCreationDate("01.01.2000");
        album.setPrivate_(true);
        album.setImageListDownloaded(Instant.now());
        album.getCategories().add("Kategorie 1");
        album.getCategories().add("Kategorie 2");
        album.getCategories().add("Kategorie 3");
        album.getCategories().add("Sonstige");
        album.getPersons().add(dummyPerson());
        for (int i = 1; i < 6; i++) {
            album.getDates().add(LocalDate.of(2000, 1, i));
        }
        for (int i = 0; i < 10; i++) {
            album.getImages().add(new Image(-1));
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
                0,
                ""
        );
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
