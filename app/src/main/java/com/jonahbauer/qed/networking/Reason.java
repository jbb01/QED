package com.jonahbauer.qed.networking;

import androidx.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static com.jonahbauer.qed.networking.Reason.NETWORK;
import static com.jonahbauer.qed.networking.Reason.NOT_FOUND;
import static com.jonahbauer.qed.networking.Reason.UNABLE_TO_LOG_IN;
import static com.jonahbauer.qed.networking.Reason.UNKNOWN;
import static com.jonahbauer.qed.networking.Reason.USER;

@Retention(RetentionPolicy.SOURCE)
@StringDef({UNKNOWN, NETWORK, NOT_FOUND, UNABLE_TO_LOG_IN, USER})
public @interface Reason {
    String UNKNOWN = "unknown error";
    String NETWORK = "network error";
    String UNABLE_TO_LOG_IN = "unable to log in";
    String NOT_FOUND = "not found";
    String USER = "user";
}
