package eu.jonahbauer.qed.network.parser.database;

import android.util.Log;

import androidx.annotation.NonNull;

import eu.jonahbauer.qed.model.Person;

import eu.jonahbauer.qed.network.parser.HtmlParser;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.util.List;
import java.util.Objects;

public final class PersonListParser extends DatabaseParser<List<Person>> {
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

                        long id = Long.parseLong(columns.get(8).text());
                        Person person = new Person(id);

                        firstName: try {
                            var element = columns.get(1).selectFirst("a");
                            if (element == null) break firstName;
                            person.setFirstName(element.text());
                        } catch (Exception ignored) {}

                        lastName: try {
                            var element = columns.get(2).selectFirst("a");
                            if (element == null) break lastName;
                            person.setLastName(element.text());
                        } catch (Exception ignored) {}

                        gender: try {
                            var element = columns.get(3);
                            if (element == null) break gender;
                            person.setGender(PersonParser.parseGender(element.text()));
                        } catch (Exception ignored) {}

                        birthday: try {
                            var element = columns.get(4).selectFirst("time");
                            if (element == null) break birthday;
                            person.setBirthday(HtmlParser.parseLocalDate(element));
                        } catch (Exception ignored) {}

                        email: try {
                            var element = columns.get(5).selectFirst("a");
                            if (element == null) break email;
                            person.setEmail(element.text());
                        } catch (Exception ignored) {}

                        username: try {
                            var element = columns.get(9);
                            if (element == null) break username;
                            person.setUsername(element.text());
                        } catch (Exception ignored) {}

                        active: try {
                            var element = columns.get(10);
                            if (element == null) break active;
                            person.setActive(HtmlParser.parseBoolean(element));
                        } catch (Exception ignored) {}

                        member: try {
                            var element = columns.get(11);
                            if (element == null) break member;
                            person.setMember(HtmlParser.parseBoolean(element));
                        } catch (Exception ignored) {}

                        dateOfJoining: try {
                            var element = columns.get(12).selectFirst("time");
                            if (element == null) break dateOfJoining;
                            person.setDateOfJoining(HtmlParser.parseLocalDate(element));
                        } catch (Exception ignored) {}


                        dateOfQuitting: try {
                            var element = columns.get(13).selectFirst("time");
                            if (element == null) break dateOfQuitting;
                            person.setDateOfQuitting(HtmlParser.parseLocalDate(element));
                        } catch (Exception ignored) {}

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
