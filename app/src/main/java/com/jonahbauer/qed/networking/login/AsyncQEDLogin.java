package com.jonahbauer.qed.networking.login;

import android.os.AsyncTask;
import android.util.Log;

import com.jonahbauer.qed.Application;
import com.jonahbauer.qed.networking.NoNetworkException;

public class AsyncQEDLogin extends AsyncTask<Void, Void, Void> {
    private final Feature feature;
    private final QEDLoginReceiver receiver;

    public AsyncQEDLogin(QEDLoginReceiver receiver, Feature feature) {
        this.receiver = receiver;
        this.feature = feature;
    }

    @Override
    protected Void doInBackground(Void...voids) {
        try {
            switch (feature) {
                case CHAT:
                    QEDLogin.loginChat();
                    break;
                case GALLERY:
                    QEDLogin.loginGallery();
                    break;
                case DATABASE:
                    QEDLogin.loginDatabase();
                    break;
            }
            if (receiver != null) receiver.onLoginSuccess();
        } catch (NoNetworkException e) {
            Log.e(Application.LOG_TAG_ERROR, e.getMessage(), e);
            if (receiver != null) receiver.onLoginNetworkError();
        } catch (InvalidCredentialsException e) {
            Log.e(Application.LOG_TAG_ERROR, e.getMessage(), e);
            if (receiver != null) receiver.onLoginError();
        }
        return null;
    }

    public enum Feature {
        CHAT, DATABASE, GALLERY
    }
}
