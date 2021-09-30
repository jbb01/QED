package com.jonahbauer.qed.model.parser;

import android.util.Log;

import androidx.annotation.NonNull;

import com.jonahbauer.qed.model.Person;
import com.jonahbauer.qed.networking.parser.HtmlParser;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.List;
import java.util.Objects;

public final class PersonListParser extends HtmlParser<List<Person>> {
    private static final String LOG_TAG = PersonListParser.class.getName();
    
    public static final PersonListParser INSTANCE = new PersonListParser();

    private PersonListParser() {}
    @NonNull
    @Override
    protected List<Person> parse(@NonNull List<Person> list, Document document) {
        list.clear();

        document.select("#people_table tbody tr")
                .stream()
                .map(tr -> {
                    try {
                        Elements columns = tr.select("td");

                        String firstName = columns.get(1).select("a").text();
                        String lastName = columns.get(2).select("a").text();
                        Element emailElement = columns.get(5).select("a").first();
                        String email = emailElement != null ? emailElement.text() : null;
                        long id = Long.parseLong(columns.get(8).text());
                        String username = columns.get(9).text();
                        boolean active = "Ja".equals(columns.get(10).text());
                        boolean member = "Ja".equals(columns.get(11).text());

                        Person person = new Person(id);
                        person.setUsername(username);
                        person.setFirstName(firstName);
                        person.setLastName(lastName);
                        person.setEmail(email);
                        person.setActive(active);
                        person.setMember(member);

                        return person;
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "Error parsing person list.", e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .forEach(list::add);

        return list;
    }
}
