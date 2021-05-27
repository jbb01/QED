package com.jonahbauer.qed.model.parser;

import androidx.annotation.NonNull;

import com.jonahbauer.qed.model.Person;
import com.jonahbauer.qed.networking.parser.HtmlParser;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.List;

public final class PersonListParser extends HtmlParser<List<Person>> {
    public static final PersonListParser INSTANCE = new PersonListParser();

    private PersonListParser() {}
    @NonNull
    @Override
    protected List<Person> parse(@NonNull List<Person> list, Document document) {
        list.clear();

        document.select("#people_table tbody tr")
                .stream()
                .map(tr -> {
                    Elements columns = tr.select("td");

                    String firstName = columns.get(1).select("a").text();
                    String lastName = columns.get(2).select("a").text();
                    Element emailElement = columns.get(5).select("a").first();
                    String email = emailElement != null ? emailElement.text() : null;
                    long id = Long.parseLong(columns.get(8).text());
                    boolean active = "Ja".equals(columns.get(10).text());
                    boolean member = "Ja".equals(columns.get(11).text());

                    Person person = new Person(id);
                    person.setFirstName(firstName);
                    person.setLastName(lastName);
                    person.setEmail(email);
                    person.setActive(active);
                    person.setMember(member);

                    return person;
                }).forEach(list::add);

        return list;
    }
}
