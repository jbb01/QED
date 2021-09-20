package com.jonahbauer.qed.networking.pages;

import androidx.annotation.NonNull;

import com.jonahbauer.qed.networking.async.AsyncLoadQEDPage;
import com.jonahbauer.qed.networking.async.AsyncLoadQEDPageToStream;
import com.jonahbauer.qed.networking.async.QEDPageReceiver;
import com.jonahbauer.qed.networking.async.QEDPageStreamReceiver;
import com.jonahbauer.qed.networking.parser.Parser;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.annotations.CheckReturnValue;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

abstract class QEDPages {

    @NonNull
    @CheckReturnValue
    static <T> Disposable run(@NonNull AsyncLoadQEDPage network,
                              @NonNull Parser<T> parser,
                              @NonNull QEDPageReceiver<T> listener,
                              @NonNull T object) {
        return Single.fromCallable(network)
                     .subscribeOn(Schedulers.io())
                     .observeOn(Schedulers.computation())
                     .map(str -> parser.apply(object, str))
                     .observeOn(AndroidSchedulers.mainThread())
                     .subscribe(
                             listener::onPageReceived,
                             err -> listener.onError(object, err)
                     );
    }

    @NonNull
    @CheckReturnValue
    static <T> Disposable run(@NonNull AsyncLoadQEDPageToStream network,
                              @NonNull QEDPageStreamReceiver<T> listener,
                              @NonNull T object) {
        return Observable.create(network)
                         .subscribeOn(Schedulers.io())
                         .observeOn(AndroidSchedulers.mainThread())
                         .subscribe(
                                 progress -> listener.onProgressUpdate(object, progress.firstLong(), progress.secondLong()),
                                 err -> listener.onError(object, err),
                                 () -> listener.onPageReceived(object)
                         );
    }
}
