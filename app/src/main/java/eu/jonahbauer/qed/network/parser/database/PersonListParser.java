package eu.jonahbauer.qed.network.parser.database;

import android.util.Log;
import androidx.annotation.NonNull;
import eu.jonahbauer.qed.model.Person;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.util.List;

public final class PersonListParser extends DatabaseParser<List<Person>> {
    private static final String LOG_TAG = PersonListParser.class.getName();
    
    public static final PersonListParser INSTANCE = new PersonListParser();

    private PersonListParser() {}

    @NonNull
    @Override
    protected List<Person> parse(@NonNull List<Person> list, @NonNull Document document) {
        list.clear();

        for (var tr : document.select("#people_table tbody tr")) {
            try {
                Elements columns = tr.select("td");

                long id = Long.parseLong(columns.get(8).text());
                Person person = new Person(id);

                parseString(columns.get(1), person::setFirstName);
                parseString(columns.get(2), person::setLastName);
                parsePersonGender(columns.get(3), person::setGender);
                parseLocalDate(columns.get(4), person::setBirthday);
                parseString(columns.get(5), person::setEmail);
                parseString(columns.get(9), person::setUsername);
                parseBoolean(columns.get(10), person::setActive);
                parseBoolean(columns.get(11), person::setMember);
                parseLocalDate(columns.get(12), person::setDateOfJoining);
                parseLocalDate(columns.get(13), person::setDateOfQuitting);

                list.add(person);
            } catch (Exception e) {
                Log.e(LOG_TAG, "Error parsing person list.", e);
            }
        }

        return list;
    }
}
