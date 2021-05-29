package com.jonahbauer.qed.activities;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.jonahbauer.qed.R;
import com.jonahbauer.qed.databinding.ActivityLoginBinding;
import com.jonahbauer.qed.networking.Feature;
import com.jonahbauer.qed.networking.NetworkListener;
import com.jonahbauer.qed.networking.login.UserLoginTask;
import com.jonahbauer.qed.util.Preferences;

import java.io.Serializable;

public class LoginActivity extends AppCompatActivity implements NetworkListener, UserLoginTask.LoginCallback {
    public static final String EXTRA_ERROR_MESSAGE = "errorMessage";
    public static final String EXTRA_DONT_START_MAIN = "dontStartMain";
    public static final String EXTRA_FEATURE = "feature";

    private UserLoginTask authTask = null;

    private ActivityLoginBinding binding;

    private boolean mDoubleBackToExitPressedOnce = false;
    private boolean mDontStartMain;
    private Feature mFeature;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Serializable extraFeature = getIntent().getSerializableExtra(EXTRA_FEATURE);
        if (extraFeature instanceof Feature) {
            mFeature = (Feature) extraFeature;
        } else {
            mFeature = Feature.CHAT;
        }

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.rememberMeCheckbox.setChecked(Preferences.general().isRememberMe());
        binding.rememberMeCheckbox.setOnCheckedChangeListener(
                (buttonView, isChecked) -> Preferences.general().edit().setRememberMe(isChecked).apply()
        );

        binding.password.setOnEditorActionListener((textView, id, keyEvent) -> {
            if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                attemptLogin();
                return true;
            }
            return false;
        });

        binding.usernameLayout.setErrorEnabled(true);
        binding.passwordLayout.setErrorEnabled(true);

        binding.signInButton.setOnClickListener(view -> attemptLogin());

        mDontStartMain = getIntent().getBooleanExtra(EXTRA_DONT_START_MAIN, false);
        String errorMessage = getIntent().getStringExtra(EXTRA_ERROR_MESSAGE);
        if (errorMessage != null) {
            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
        }
    }

    private void attemptLogin() {
        if (authTask != null) {
            return;
        }

        binding.usernameLayout.setError(null);
        binding.passwordLayout.setError(null);
        binding.username.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
        binding.password.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);

        String username = binding.username.getText().toString();
        char[] password = new char[binding.password.getText().length()];
        binding.password.getText().getChars(0, password.length, password, 0);

        showProgress(true);
        authTask = new UserLoginTask(username, password, mFeature, this);
        authTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void showProgress(final boolean show) {
        binding.loginForm.setVisibility(show ? View.GONE : View.VISIBLE);
        binding.loginProgress.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onBackPressed() {
        if (mDoubleBackToExitPressedOnce) {
            finishAffinity();
            return;
        }

        this.mDoubleBackToExitPressedOnce = true;
        Toast.makeText(this, R.string.double_back_to_exit_toast, Toast.LENGTH_SHORT).show();

        new Handler().postDelayed(() -> mDoubleBackToExitPressedOnce = false, 2000);
    }

    @Override
    public void onConnectionFail() {
        binding.passwordLayout.post(() -> setError(getString(R.string.cant_connect)));
    }

    @Override
    public void onConnectionRegain() {
        binding.passwordLayout.post(() -> setError(null));
    }

    @Override
    public void onResult(Boolean success) {
        authTask = null;
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
            setError(getString(R.string.login_error_incorrect_data));
        }
    }

    @Override
    public void onCancelled() {
        authTask = null;
        showProgress(false);
    }

    private void setError(String string) {
        binding.passwordLayout.setError(string);

        if (string != null) {
            binding.password.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_error_red, 0);
            binding.password.requestFocus();
        } else {
            binding.password.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
        }
    }
}

