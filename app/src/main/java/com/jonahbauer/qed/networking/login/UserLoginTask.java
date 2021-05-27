package com.jonahbauer.qed.networking.login;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jonahbauer.qed.crypt.PasswordStorage;
import com.jonahbauer.qed.crypt.PasswordUtils;
import com.jonahbauer.qed.networking.Feature;
import com.jonahbauer.qed.networking.exceptions.InvalidCredentialsException;
import com.jonahbauer.qed.networking.exceptions.NetworkException;
import com.jonahbauer.qed.util.Preferences;

/**
 * Represents an asynchronous login/registration task used to authenticate
 * the user.
 */
public class UserLoginTask extends AsyncTask<Void, Void, Boolean> {
    private final String mUsername;
    private final char[] mPassword;

    private final LoginCallback mCallback;
    private final Feature mFeature;

    public UserLoginTask(String username, char[] password, Feature feature, @NonNull LoginCallback callback) {
        mUsername = username;
        mPassword = password;
        mFeature = feature;

        mCallback = callback;
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        try {
            switch (mFeature) {
                case CHAT:
                default:
                    QEDLogin.loginChat(mUsername, mPassword);
                    break;
                case GALLERY:
                    QEDLogin.loginGallery(mUsername, mPassword);
                    break;
                case DATABASE:
                    QEDLogin.loginDatabase(mUsername, mPassword);
                    break;
            }

            // Store username and password
            if (Preferences.general().isRememberMe()) {
                PasswordStorage.saveUsernameAndPassword(mUsername, mPassword);
            }

            return true;
        } catch (NetworkException e) {
            return null;
        } catch (InvalidCredentialsException e) {
            return false;
        } finally {
            PasswordUtils.wipe(mPassword);
        }
    }

    @Override
    protected void onPostExecute(@Nullable final Boolean success) {
        mCallback.onResult(success);
    }

    @Override
    protected void onCancelled() {
        mCallback.onCancelled();
    }

    public interface LoginCallback {
        /**
         * Called after login has finished.
         *
         * @param success true if login was successful, false if not, null if an error occurred
         */
        void onResult(Boolean success);
        void onCancelled();
    }
}
