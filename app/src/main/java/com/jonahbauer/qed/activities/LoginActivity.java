package com.jonahbauer.qed.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputLayout;
import com.jonahbauer.qed.Application;
import com.jonahbauer.qed.Internet;
import com.jonahbauer.qed.R;
import com.jonahbauer.qed.networking.AsyncLoadQEDPage;
import com.jonahbauer.qed.networking.AsyncLoadQEDPageToStream;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLEncoder;

import javax.net.ssl.HttpsURLConnection;

public class LoginActivity extends AppCompatActivity implements Internet {
    public static final String ERROR_MESSAGE = "errorMessage";
    public static final String DONT_START_MAIN = "dontStartMain";

    private UserLoginTask authTask = null;

    private EditText usernameView;
    private EditText passwordView;
    private TextInputLayout usernameLayout;
    private TextInputLayout passwordLayout;
    private View progressView;
    private View loginFormView;

    private boolean doubleBackToExitPressedOnce = false;
    private boolean dontStartMain;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getSharedPreferences(getString(R.string.preferences_shared_preferences),MODE_PRIVATE).edit().putBoolean(getString(R.string.preferences_loggedIn_key),false).apply();

        setContentView(R.layout.activity_login);
        usernameView = findViewById(R.id.username);
        passwordView = findViewById(R.id.password);
        usernameLayout = findViewById(R.id.username_layout);
        passwordLayout = findViewById(R.id.password_layout);
        progressView = findViewById(R.id.login_progress);
        loginFormView = findViewById(R.id.login_form);

        passwordView.setOnEditorActionListener((textView, id, keyEvent) -> {
            if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                attemptLogin();
                return true;
            }
            return false;
        });
        usernameLayout.setErrorEnabled(true);
        passwordLayout.setErrorEnabled(true);

        Button mUsernameSignInButton = findViewById(R.id.username_sign_in_button);
        mUsernameSignInButton.setOnClickListener(view -> attemptLogin());

        dontStartMain = getIntent().getBooleanExtra(DONT_START_MAIN, false);
        String errorMessage = getIntent().getStringExtra(ERROR_MESSAGE);
        if (errorMessage != null) {
            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
        }
    }

    private void attemptLogin() {
        if (authTask != null) {
            return;
        }

        usernameLayout.setError(null);
        passwordLayout.setError(null);
        usernameView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
        passwordView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);

        String username = usernameView.getText().toString();
        String password = passwordView.getText().toString();

        showProgress(true);
        authTask = new UserLoginTask(username, password);
        authTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void showProgress(final boolean show) {
        int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

        loginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        loginFormView.animate().setDuration(shortAnimTime).alpha(
                show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                loginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            }
        });

        progressView.setVisibility(show ? View.VISIBLE : View.GONE);
        progressView.animate().setDuration(shortAnimTime).alpha(
                show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                progressView.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            ((Application)getApplication()).finish();
            return;
        }

        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, R.string.double_back_to_exit_toast, Toast.LENGTH_SHORT).show();

        new Handler().postDelayed(() -> doubleBackToExitPressedOnce = false, 2000);
    }

    @Override
    public void onConnectionFail() {
        passwordLayout.post(() -> {
            passwordLayout.setError(getString(R.string.cant_connect));
            passwordView.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_error_red, 0);
            passwordView.requestFocus();
        });
    }

    @Override
    public void onConnectionRegain() {
        passwordLayout.post(() -> {
            passwordLayout.setError(null);
            passwordView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
        });
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    private class UserLoginTask extends AsyncTask<Void, Void, Boolean> {

        private final String mUsername;
        private final String mPassword;

        private boolean networkError;

        UserLoginTask(String username, String password) {
            mUsername = username;
            mPassword = password;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
//            if (!Application.online) return false;
            try {
                URL url = new URL(getString(R.string.chat_server_login));
                HttpsURLConnection logInConnection = (HttpsURLConnection) url.openConnection();

                logInConnection.setDoInput(true);
                logInConnection.setDoOutput(true);
                logInConnection.setReadTimeout(5 * 1000);
                logInConnection.setConnectTimeout(5 * 1000);
                logInConnection.setRequestMethod("POST");
                logInConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                logInConnection.setRequestProperty("charset", "utf-8");

                logInConnection.connect();

                String data = URLEncoder.encode("username", "UTF-8") + "=" + URLEncoder.encode(mUsername, "UTF-8");
                data += "&" + URLEncoder.encode("password", "UTF-8") + "=" + URLEncoder.encode(mPassword, "UTF-8");
                data += "&" + URLEncoder.encode("version", "UTF-8") + "="
                        + URLEncoder.encode(getString(R.string.chat_version), "UTF-8");


                PrintWriter pw = new PrintWriter(logInConnection.getOutputStream());

                pw.write(data);
                pw.flush();
                pw.close();

                for (int i = 0; i < logInConnection.getHeaderFields().keySet().size(); i++) {
                    if ("Set-Cookie".equalsIgnoreCase(logInConnection.getHeaderFieldKey(i))) {
                        String header = logInConnection.getHeaderField(i);
                        if (header.startsWith("userid"))
                            ((Application)getApplication()).saveData(String.valueOf(Integer.valueOf(header.split("=")[1].split(";")[0])), Application.KEY_USERID, false);
                        else if (header.startsWith("pwhash"))
                            ((Application)getApplication()).saveData(header.split("=")[1].split(";")[0], Application.KEY_CHAT_PWHASH, true);
                    }
                }

                BufferedInputStream in = new BufferedInputStream(logInConnection.getInputStream());
                StringBuffer sb;
                String returnString;

                int x;

                sb = new StringBuffer();

                while ((x = in.read()) != -1) {
                    sb.append((char) x);
                }

                in.close();

                logInConnection.disconnect();

                returnString = sb.toString();
                String[] splitString = returnString.split("\"");

                if (logInConnection.getResponseCode() == 200 && splitString[3].equals("success")) {
                    return true;
                }

            } catch (IOException e) {
                Log.e(Application.LOG_TAG_ERROR, e.getMessage(), e);
                networkError = true;
            }
            return false;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            authTask = null;
            showProgress(false);

            if (success) {
                AsyncLoadQEDPage.forcedLogin = false;
                AsyncLoadQEDPageToStream.forcedLogin = false;
                ((Application)getApplication()).saveData(mUsername, Application.KEY_USERNAME, false);
                ((Application)getApplication()).saveData(mPassword, Application.KEY_PASSWORD, true);

                getSharedPreferences(getString(R.string.preferences_shared_preferences),MODE_PRIVATE).edit().putBoolean(getString(R.string.preferences_loggedIn_key),true).apply();

                if (!dontStartMain) {
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    startActivity(intent);
                }
                LoginActivity.this.finish();
                finish();
            } else {
                if (!networkError) {
                    passwordLayout.setError(getString(R.string.login_error_incorrect_data));
                    passwordView.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_error_red, 0);
                    passwordView.requestFocus();
                } else onConnectionFail();
            }
        }


        @Override
        protected void onCancelled() {
            authTask = null;
            showProgress(false);
        }
    }
}

