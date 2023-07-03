package eu.jonahbauer.qed.model.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import eu.jonahbauer.qed.R;
import eu.jonahbauer.qed.model.Person;
import eu.jonahbauer.qed.networking.async.QEDPageReceiver;
import eu.jonahbauer.qed.networking.pages.QEDDBPages;
import io.reactivex.rxjava3.disposables.Disposable;

import java.time.Instant;

public class PersonViewModel extends DatabaseInfoViewModel<Person> {

    public PersonViewModel(@NonNull Application application) {
        super(application);
    }

    @Override
    protected Disposable load(@NonNull Person person, @NonNull QEDPageReceiver<Person> receiver) {
        return QEDDBPages.getPerson(person, receiver);
    }

    @Override
    public void onResult(@NonNull Person out) {
        out.setLoaded(Instant.now());
    }

    @NonNull
    @Override
    protected CharSequence getTitle(@NonNull Person person) {
        return person.getFullName();
    }

    @Override
    protected @StringRes Integer getDefaultTitle() {
        return R.string.title_fragment_person;
    }
}