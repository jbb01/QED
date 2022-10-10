package com.jonahbauer.qed.networking.pages;

import androidx.annotation.NonNull;

import com.jonahbauer.qed.model.Event;
import com.jonahbauer.qed.model.Person;
import com.jonahbauer.qed.model.Registration;
import com.jonahbauer.qed.model.parser.EventListParser;
import com.jonahbauer.qed.model.parser.EventParser;
import com.jonahbauer.qed.model.parser.PersonListParser;
import com.jonahbauer.qed.model.parser.PersonParser;
import com.jonahbauer.qed.model.parser.RegistrationParser;
import com.jonahbauer.qed.networking.Feature;
import com.jonahbauer.qed.networking.NetworkConstants;
import com.jonahbauer.qed.networking.Reason;
import com.jonahbauer.qed.networking.async.AsyncLoadQEDPage;
import com.jonahbauer.qed.networking.async.QEDPageReceiver;

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
