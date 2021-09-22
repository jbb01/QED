package com.jonahbauer.qed.networking;


import androidx.annotation.StringRes;
import androidx.room.rxjava3.EmptyResultSetException;

import com.jonahbauer.qed.R;
import com.jonahbauer.qed.networking.exceptions.InvalidCredentialsException;
import com.jonahbauer.qed.networking.exceptions.LowMemoryException;

import java.io.FileNotFoundException;
import java.io.IOException;

public enum Reason {
    UNKNOWN(R.string.error_unknown),
    NETWORK(R.string.error_network),
    UNABLE_TO_LOG_IN(R.string.error_login),
    EMPTY(R.string.error_empty),
    NOT_FOUND(R.string.error_404),
    OUT_OF_MEMORY(R.string.error_out_of_memory),
    USER(R.string.error_user);

    @StringRes
    private final int stringRes;

    Reason(int res) {
        this.stringRes = res;
    }

    public static Reason guess(Throwable throwable) {
        if (throwable instanceof InvalidCredentialsException) {
            return UNABLE_TO_LOG_IN;
        } else if (throwable instanceof FileNotFoundException) {
            return NOT_FOUND;
        } else if (throwable instanceof IOException) {
            return NETWORK;
        } else if (throwable instanceof NullPointerException || throwable instanceof EmptyResultSetException) {
            return EMPTY;
        } else if (throwable instanceof OutOfMemoryError || throwable instanceof LowMemoryException) {
            return OUT_OF_MEMORY;
        } else {
            return UNKNOWN;
        }
    }

    @StringRes
    public int getStringRes() {
        return stringRes;
    }
}
