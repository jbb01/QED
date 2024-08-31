package eu.jonahbauer.qed.model.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.lifecycle.*;
import eu.jonahbauer.qed.R;
import eu.jonahbauer.qed.model.Person;
import eu.jonahbauer.qed.networking.async.QEDPageReceiver;
import eu.jonahbauer.qed.networking.pages.QEDDBPages;
import eu.jonahbauer.qed.util.Preferences;
import eu.jonahbauer.qed.util.Preferences$Config;
import eu.jonahbauer.qed.util.preferences.SharedPreferenceLiveData;
import io.reactivex.rxjava3.disposables.Disposable;

import java.time.Instant;

public class PersonViewModel extends DatabaseInfoViewModel<Person> {
    private final @NonNull LiveData<Boolean> mFavorite;

    public PersonViewModel(@NonNull Application application) {
        super(application);

        var liveFavorites = SharedPreferenceLiveData.forString(
                Preferences.getSharedPreferences(),
                new Preferences$Config.LongSetSerializer(),
                Preferences.getDatabase().getKeys().getFavorites(),
                null
        );
        var livePerson = getValue();

        var favorite = new MediatorLiveData<Boolean>();
        favorite.addSource(liveFavorites, favorites -> {
            var person = livePerson.getValue();
            favorite.setValue(person != null &&  favorites != null && favorites.contains(person.getId()));
        });
        favorite.addSource(livePerson, person -> {
            var favorites = liveFavorites.getValue();
            favorite.setValue(person != null && favorites != null && favorites.contains(person.getId()));
        });
        this.mFavorite = favorite;
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

    @NonNull
    public LiveData<Boolean> getFavorite() {
        return mFavorite;
    }
}