package com.jonahbauer.qed.model.parser;

import android.util.Log;

import com.jonahbauer.qed.model.Message;
import com.jonahbauer.qed.networking.exceptions.LowMemoryException;

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

/**
 * Parses a list of \n-separated JSON-formatted messages.
 */
public final class ChatLogParser implements ObservableOnSubscribe<LongLongPair> {
    private static final String LOG_TAG = ChatLogParser.class.getName();
    private static final double MAX_MEMORY_FACTOR = 0.75;

    private final Supplier<InputStream> mIn;
    private final Consumer<Message> mOut;

    public ChatLogParser(Supplier<InputStream> in, Consumer<Message> out) {
        this.mIn = in;
        this.mOut = out;
    }

    @Override
    public void subscribe(@NonNull ObservableEmitter<LongLongPair> emitter) throws Throwable {
        long lineCount;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(mIn.get()))) {
            lineCount = reader.lines().count() - 2;
        }

        if (lineCount > Integer.MAX_VALUE) {
            throw new LowMemoryException("Cannot process more than " + Integer.MAX_VALUE + " messages at once.");
        }
        emitter.onNext(LongLongPair.of(0, lineCount));

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(mIn.get()))) {
            long index = 0;
            long lastUpdate = System.currentTimeMillis();

            Message.Type lastType = null;

            Iterator<String> lines = reader.lines().iterator();
            while (lines.hasNext() && !emitter.isDisposed()) {
                String line = lines.next();
                Message msg = Message.interpretJSONMessage(line);

                if (msg != null && (lastType = msg.getType()) == Message.Type.POST) {
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

                if (index % 1000 == 0) {
                    Runtime runtime = Runtime.getRuntime();
                    if (runtime.totalMemory() > runtime.maxMemory() * MAX_MEMORY_FACTOR) {
                        throw new LowMemoryException("Exceeded maximum allowed memory.");
                    }
                }
            }

            if (lastType != Message.Type.OK) {
                Log.w(LOG_TAG, "Last message was of type " + lastType + " (expected OK).");
            }
        }
        emitter.onNext(LongLongPair.of(lineCount, lineCount));
        emitter.onComplete();
    }
}
