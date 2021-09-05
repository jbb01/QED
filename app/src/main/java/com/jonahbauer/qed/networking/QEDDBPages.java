package com.jonahbauer.qed.networking;

import android.annotation.SuppressLint;
import android.os.AsyncTask;

import androidx.annotation.NonNull;

import com.jonahbauer.qed.model.Event;
import com.jonahbauer.qed.model.Person;
import com.jonahbauer.qed.model.parser.EventListParser;
import com.jonahbauer.qed.model.parser.EventParser;
import com.jonahbauer.qed.model.parser.PersonListParser;
import com.jonahbauer.qed.model.parser.PersonParser;
import com.jonahbauer.qed.networking.async.AsyncLoadQEDPage;
import com.jonahbauer.qed.networking.async.QEDPageReceiver;

import java.util.ArrayList;
import java.util.List;

import lombok.experimental.UtilityClass;

@UtilityClass
@SuppressWarnings("UnusedReturnValue")
public class QEDDBPages {
    //<editor-fold desc="Event">
    public static AsyncTask<?,?,?> getEvent(@NonNull Event event, QEDPageReceiver<Event> eventReceiver) {
        @SuppressLint("StringFormatMatches")
        AsyncLoadQEDPage<Event> async = new AsyncLoadQEDPage<>(
                Feature.DATABASE,
                NetworkConstants.DATABASE_SERVER_EVENT + event.getId(),
                event,
                EventParser.INSTANCE,
                eventReceiver
        );

        async.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        return async;
    }

    public static AsyncTask<?,?,?> getEventList(QEDPageReceiver<List<Event>> eventListReceiver) {
        AsyncLoadQEDPage<List<Event>> async = new AsyncLoadQEDPage<>(
                Feature.DATABASE,
                NetworkConstants.DATABASE_SERVER_EVENTS,
                new ArrayList<>(),
                EventListParser.INSTANCE,
                eventListReceiver
        );

        async.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        return async;
    }
    //</editor-fold>

    //<editor-fold desc="Person">
    public static AsyncTask<?,?,?> getPerson(@NonNull Person person, QEDPageReceiver<Person> personReceiver) {
        @SuppressLint("StringFormatMatches")
        AsyncLoadQEDPage<Person> async = new AsyncLoadQEDPage<>(
                Feature.DATABASE,
                NetworkConstants.DATABASE_SERVER_PERSON + person.getId(),
                person,
                PersonParser.INSTANCE,
                personReceiver
        );


        async.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        return async;
    }

    public static AsyncTask<?,?,?> getPersonList(QEDPageReceiver<List<Person>> personListReceiver) {
        AsyncLoadQEDPage<List<Person>> async = new AsyncLoadQEDPage<>(
                Feature.DATABASE,
                NetworkConstants.DATABASE_SERVER_PERSONS,
                new ArrayList<>(),
                PersonListParser.INSTANCE,
                personListReceiver
        );

        async.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        return async;
    }
    //</editor-fold>
}
