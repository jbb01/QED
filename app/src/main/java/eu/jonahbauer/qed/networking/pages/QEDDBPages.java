package eu.jonahbauer.qed.networking.pages;

import androidx.annotation.NonNull;

import eu.jonahbauer.qed.model.Event;
import eu.jonahbauer.qed.model.Person;
import eu.jonahbauer.qed.model.Registration;
import eu.jonahbauer.qed.model.parser.EventListParser;
import eu.jonahbauer.qed.model.parser.EventParser;
import eu.jonahbauer.qed.model.parser.PersonListParser;
import eu.jonahbauer.qed.model.parser.PersonParser;
import eu.jonahbauer.qed.model.parser.RegistrationParser;
import eu.jonahbauer.qed.networking.Feature;
import eu.jonahbauer.qed.networking.NetworkConstants;
import eu.jonahbauer.qed.networking.Reason;
import eu.jonahbauer.qed.networking.async.AsyncLoadQEDPage;
import eu.jonahbauer.qed.networking.async.QEDPageReceiver;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import io.reactivex.rxjava3.annotations.CheckReturnValue;
import io.reactivex.rxjava3.disposables.Disposable;
import lombok.experimental.UtilityClass;

@UtilityClass
public class QEDDBPages extends QEDPages {
    //<editor-fold desc="Event">
    @NonNull
    @CheckReturnValue
    public static Disposable getEvent(@NonNull Event event, QEDPageReceiver<Event> eventReceiver) {
        if (event.getId() == Event.NO_ID) {
            eventReceiver.onError(event, Reason.NOT_FOUND, null);
            return Disposable.disposed();
        }

        AsyncLoadQEDPage network = new AsyncLoadQEDPage(
                Feature.DATABASE,
                String.format(Locale.ROOT, NetworkConstants.DATABASE_SERVER_EVENT, event.getId())
        );

        return run(
                network,
                EventParser.INSTANCE,
                eventReceiver,
                event
        );
    }

    @NonNull
    @CheckReturnValue
    public static Disposable getEventList(QEDPageReceiver<List<Event>> eventListReceiver) {
        AsyncLoadQEDPage network = new AsyncLoadQEDPage(
                Feature.DATABASE,
                NetworkConstants.DATABASE_SERVER_EVENTS
        );

        return run(
                network,
                EventListParser.INSTANCE,
                eventListReceiver,
                new ArrayList<>()
        );
    }
    //</editor-fold>

    //<editor-fold desc="Person">
    @NonNull
    @CheckReturnValue
    public static Disposable getPerson(@NonNull Person person, QEDPageReceiver<Person> personReceiver) {
        if (person.getId() == Person.NO_ID) {
            personReceiver.onError(person, Reason.NOT_FOUND, null);
            return Disposable.disposed();
        }

        AsyncLoadQEDPage network = new AsyncLoadQEDPage(
                Feature.DATABASE,
                String.format(Locale.ROOT, NetworkConstants.DATABASE_SERVER_PERSON, person.getId())
        );

        return run(
                network,
                PersonParser.INSTANCE,
                personReceiver,
                person
        );
    }

    @NonNull
    @CheckReturnValue
    public static Disposable getPersonList(QEDPageReceiver<List<Person>> personListReceiver) {
        AsyncLoadQEDPage network = new AsyncLoadQEDPage(
                Feature.DATABASE,
                NetworkConstants.DATABASE_SERVER_PERSONS
        );

        return run(
                network,
                PersonListParser.INSTANCE,
                personListReceiver,
                new ArrayList<>()
        );
    }
    //</editor-fold>

    @NonNull
    @CheckReturnValue
    public static Disposable getRegistration(@NonNull Registration registration, QEDPageReceiver<Registration> registrationReceiver) {
        if (registration.getId() == Registration.NO_ID) {
            registrationReceiver.onError(registration, Reason.NOT_FOUND, null);
            return Disposable.disposed();
        }

        AsyncLoadQEDPage network = new AsyncLoadQEDPage(
                Feature.DATABASE,
                String.format(Locale.ROOT, NetworkConstants.DATABASE_SERVER_REGISTRATION, registration.getId())
        );

        return run(
                network,
                RegistrationParser.INSTANCE,
                registrationReceiver,
                registration
        );
    }
}
