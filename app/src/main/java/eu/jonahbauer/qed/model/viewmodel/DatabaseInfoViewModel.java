package eu.jonahbauer.qed.model.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import eu.jonahbauer.qed.networking.Reason;
import eu.jonahbauer.qed.networking.async.QEDPageReceiver;
import eu.jonahbauer.qed.util.StatusWrapper;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;

public abstract class DatabaseInfoViewModel<T> extends InfoViewModel<T> {
    private final CompositeDisposable mDisposable = new CompositeDisposable();
    private final QEDPageReceiver<T> mReceiver = new QEDPageReceiver<>() {
        @Override
        public void onResult(@NonNull T out) {
            DatabaseInfoViewModel.this.onResult(out);
            submit(StatusWrapper.loaded(out));
        }

        @Override
        public void onError(T out, @NonNull Reason reason, @Nullable Throwable cause) {
            QEDPageReceiver.super.onError(out, reason, cause);
            submit(StatusWrapper.error(out, reason));
        }
    };

    public DatabaseInfoViewModel(@NonNull Application application) {
        super(application);
    }

    public final void load(@NonNull T value) {
        submit(StatusWrapper.preloaded(value));
        mDisposable.add(load(value, mReceiver));
    }

    protected abstract Disposable load(@NonNull T value, @NonNull QEDPageReceiver<T> receiver);

    protected abstract void onResult(@NonNull T out);

    @Override
    protected final void onCleared() {
        super.onCleared();
        mDisposable.clear();
    }
}
