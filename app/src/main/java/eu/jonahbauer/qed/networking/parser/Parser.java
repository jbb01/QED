package eu.jonahbauer.qed.networking.parser;

import java.util.function.BiFunction;

public interface Parser<T> extends BiFunction<T, String, T> {
}
