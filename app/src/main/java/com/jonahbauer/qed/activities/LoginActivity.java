package com.jonahbauer.qed.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.google.android.material.textfield.TextInputLayout;
import com.jonahbauer.qed.Application;
import com.jonahbauer.qed.NetworkListener;
import com.jonahbauer.qed.Pref;
import com.jonahbauer.qed.R;
import com.jonahbauer.qed.networking.login.UserLoginTask;

public class LoginActivity extends AppCompatActivity implements NetworkListener, UserLoginTask.LoginCallback {
    public static final String ERROR_MESSAGE = "errorMessage";
    public static final String DONT_START_MAIN = "dontStartMain";

    private UserLoginTask mAuthTask = null;

    private EditText mUsernameView;
    private EditText mPasswordView;
    private TextInputLayout mUsernameLayout;
    private TextInputLayout mPasswordLayout;
    private View mProgressView;
    private View mLoginFormView;

    private boolean mDoubleBackToExitPressedOnce = false;
    private boolean mDontStartMain;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean(Pref.General.LOGGED_IN,false).apply();

        setContentView(R.layout.activity_login);
        mUsernameView = findViewById(R.id.username);
        mPasswordView = findViewById(R.id.password);
        mUsernameLayout = findViewById(R.id.username_layout);
        mPasswordLayout = findViewById(R.id.password_layout);
        mProgressView = findViewById(R.id.login_progress);
        mLoginFormView = findViewById(R.id.login_form);

        mPasswordView.setOnEditorActionListener((textView, id, keyEvent) -> {
            if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                attemptLogin();
                return true;
            }
            return false;
        });
        mUsernameLayout.setErrorEnabled(true);
        mPasswordLayout.setErrorEnabled(true);

        Button mUsernameSignInButton = findViewById(R.id.username_sign_in_button);
        mUsernameSignInButton.setOnClickListener(view -> attemptLogin());

        mDontStartMain = getIntent().getBooleanExtra(DONT_START_MAIN, false);
        String errorMessage = getIntent().getStringExtra(ERROR_MESSAGE);
        if (errorMessage != null) {
            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
        }
    }

    private void attemptLogin() {
        if (mAuthTask != null) {
            return;
        }

        mUsernameLayout.setError(null);
        mPasswordLayout.setError(null);
        mUsernameView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
        mPasswordView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);

        String username = mUsernameView.getText().toString();
        String password = mPasswordView.getText().toString();

        showProgress(true);
        mAuthTask = new UserLoginTask(username, password, this, this);
        mAuthTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void showProgress(final boolean show) {
        int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

        mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            }
        });

        mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
        mProgressView.animate().setDuration(shortAnimTime).alpha(
                show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (mDoubleBackToExitPressedOnce) {
            ((Application)getApplication()).finish();
            return;
        }

        this.mDoubleBackToExitPressedOnce = true;
        Toast.makeText(this, R.string.double_back_to_exit_toast, Toast.LENGTH_SHORT).show();

        new Handler().postDelayed(() -> mDoubleBackToExitPressedOnce = false, 2000);
    }

    @Override
    public void onConnectionFail() {
        mPasswordLayout.post(() -> {
            mPasswordLayout.setError(getString(R.string.cant_connect));
            mPasswordView.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_error_red, 0);
            mPasswordView.requestFocus();
        });
    }

    @Override
    public void onConnectionRegain() {
        mPasswordLayout.post(() -> {
            mPasswordLayout.setError(null);
            mPasswordView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
        });
    }

    @Override
    public void onResult(Boolean success) {
        mAuthTask = null;
        showProgress(false);

        if (success == null) {
            onConnectionFail();
        } else if (success) {
            if (!mDontStartMain) {
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                startActivity(intent);
            }
            LoginActivity.this.finish();
            finish();
        } else {
            mPasswordLayout.setError(getString(R.string.login_error_incorrect_data));
            mPasswordView.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_error_red, 0);
            mPasswordView.requestFocus();
        }
    }

    @Override
    public void onCancelled() {
        mAuthTask = null;
        showProgress(false);
    }
}

