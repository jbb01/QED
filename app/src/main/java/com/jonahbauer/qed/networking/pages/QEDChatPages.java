package com.jonahbauer.qed.networking.pages;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;

import com.jonahbauer.qed.model.Message;
import com.jonahbauer.qed.model.parser.ChatLogParser;
import com.jonahbauer.qed.model.viewmodel.LogViewModel;
import com.jonahbauer.qed.model.viewmodel.LogViewModel.FileLogRequest;
import com.jonahbauer.qed.networking.Feature;
import com.jonahbauer.qed.networking.NetworkConstants;
import com.jonahbauer.qed.networking.Reason;
import com.jonahbauer.qed.networking.async.AsyncLoadQEDPageToStream;
import com.jonahbauer.qed.networking.async.QEDPageStreamReceiver;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.annotations.CheckReturnValue;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.Supplier;
import io.reactivex.rxjava3.schedulers.Schedulers;
import lombok.experimental.UtilityClass;

@UtilityClass
public class QEDChatPages extends QEDPages {

    @NonNull
    @CheckReturnValue
    public static Disposable getChatLog(@NonNull Context context, @NonNull LogViewModel.LogRequest logRequest, @NonNull Uri file, QEDPageStreamReceiver<Uri> listener) {
        if (logRequest instanceof FileLogRequest) throw new IllegalStateException();

        OutputStream out;
        try {
            out = context.getContentResolver().openOutputStream(file);
            if (out == null) throw new IOException("Could not open output stream for uri " + file + ".");
        } catch (IOException e) {
            listener.onError(file, Reason.guess(e), e);
            return Disposable.empty();
        }

        AsyncLoadQEDPageToStream network = new AsyncLoadQEDPageToStream(
                Feature.CHAT,
                NetworkConstants.CHAT_SERVER_HISTORY + logRequest.getQueryString(),
                out
        );

        return run(
                network,
                listener,
                file
        );
    }

    @NonNull
    @CheckReturnValue
    public static Disposable parseChatLog(@NonNull Context context, @NonNull Uri file, QEDPageStreamReceiver<List<Message>> listener) {
        ArrayList<Message> out = new ArrayList<>();

        Supplier<InputStream> in = () -> context.getContentResolver().openInputStream(file);
        Consumer<Message> consumer = out::add;

        return Observable.create(new ChatLogParser(in, consumer))
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        progress -> {
                            if (progress.firstLong() == 0) {
                                out.ensureCapacity((int) progress.secondLong());
                            }
                            listener.onProgressUpdate(Collections.emptyList(), progress.firstLong(), progress.secondLong());
                        },
                        err -> listener.onError(Collections.emptyList(), err),
                        () -> listener.onPageReceived(out)
                );
    }
}
