package com.jonahbauer.qed.model.parser;

import com.jonahbauer.qed.model.Message;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.function.Consumer;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.ObservableEmitter;
import io.reactivex.rxjava3.core.ObservableOnSubscribe;
import io.reactivex.rxjava3.functions.Supplier;
import it.unimi.dsi.fastutil.longs.LongLongPair;

public final class ChatLogParser implements ObservableOnSubscribe<LongLongPair> {
    private final Supplier<InputStream> mIn;
    private final Consumer<Message> mOut;

    public ChatLogParser(Supplier<InputStream> in, Consumer<Message> out) {
        this.mIn = in;
        this.mOut = out;
    }

    @Override
    public void subscribe(@NonNull ObservableEmitter<LongLongPair> emitter) {
        try {
            long lineCount;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(mIn.get()))) {
                lineCount = reader.lines().count();
            }

            if (lineCount > Integer.MAX_VALUE) {
                throw new IndexOutOfBoundsException("Cannot process more than " + Integer.MAX_VALUE + " messages at once.");
            }
            emitter.onNext(LongLongPair.of(0, lineCount));

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(mIn.get()))) {
                long index = 0;
                long lastUpdate = System.currentTimeMillis();

                Iterator<String> lines = reader.lines().iterator();
                while (lines.hasNext() && !emitter.isDisposed()) {
                    String line = lines.next();
                    Message msg = Message.interpretJSONMessage(line);

                    if (msg != null) {
                        mOut.accept(msg);
                    }

                    index++;

                    if (index % 19 == 0) {
                        long time = System.currentTimeMillis();
                        if (time - lastUpdate > 33) {
                            lastUpdate = time;
                            emitter.onNext(LongLongPair.of(index, lineCount));
                        }
                    }
                }
            }
            emitter.onNext(LongLongPair.of(lineCount, lineCount));
            emitter.onComplete();
        } catch (Throwable t) {
            emitter.tryOnError(t);
        }
    }
}
