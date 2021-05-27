package com.jonahbauer.qed.util;

public final class Triple<A,B,C> {
    public final A first;
    public final B second;
    public final C third;

    public Triple(A a, B b, C c) {
        first = a;
        second = b;
        third = c;
    }
}
