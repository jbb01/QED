package eu.jonahbauer.qed.model.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import eu.jonahbauer.qed.R;
import eu.jonahbauer.qed.model.Event;
import eu.jonahbauer.qed.networking.async.QEDPageReceiver;
import eu.jonahbauer.qed.networking.pages.QEDDBPages;
import io.reactivex.rxjava3.disposables.Disposable;

import java.time.Instant;

public class EventViewModel extends DatabaseInfoViewModel<Event> {
    public EventViewModel(@NonNull Application application) {
        super(application);
    }

    @Override
    protected Disposable load(@NonNull Event event, @NonNull QEDPageReceiver<Event> receiver) {
        return QEDDBPages.getEvent(event, receiver);
    }

    @Override
    public void onResult(@NonNull Event out) {
        out.setLoaded(Instant.now());
    }

    @Override
    protected @NonNull CharSequence getTitle(@NonNull Event event) {
        return event.getTitle();
    }

    @Override
    protected @StringRes Integer getDefaultTitle() {
        return R.string.title_fragment_event;
    }
}