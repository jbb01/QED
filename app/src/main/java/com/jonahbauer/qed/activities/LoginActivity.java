package com.jonahbauer.qed.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.jonahbauer.qed.R;
import com.jonahbauer.qed.databinding.ActivityLoginBinding;
import com.jonahbauer.qed.networking.Feature;
import com.jonahbauer.qed.networking.NetworkListener;
import com.jonahbauer.qed.networking.Reason;
import com.jonahbauer.qed.networking.async.QEDPageReceiver;
import com.jonahbauer.qed.networking.exceptions.InvalidCredentialsException;
import com.jonahbauer.qed.networking.login.QEDLogin;
import com.jonahbauer.qed.util.Preferences;
import com.jonahbauer.qed.util.ViewUtils;

import java.io.Serializable;

import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class LoginActivity extends AppCompatActivity implements NetworkListener, QEDPageReceiver<Boolean> {
    public static final String EXTRA_ERROR_MESSAGE = "errorMessage";
    public static final String EXTRA_DONT_START_MAIN = "dontStartMain";
    public static final String EXTRA_FEATURE = "feature";

    private ActivityLoginBinding mBinding;

    private boolean mDoubleBackToExitPressedOnce = false;
    private boolean mDontStartMain;
    private Feature mFeature;

    private final CompositeDisposable mDisposable = new CompositeDisposable();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Serializable extraFeature = getIntent().getSerializableExtra(EXTRA_FEATURE);
        if (extraFeature instanceof Feature) {
            mFeature = (Feature) extraFeature;
        } else {
            mFeature = Feature.CHAT;
        }

        mBinding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());

        mBinding.rememberMeCheckbox.setChecked(Preferences.general().isRememberMe());
        mBinding.rememberMeCheckbox.setOnCheckedChangeListener(
                (buttonView, isChecked) -> Preferences.general().edit().setRememberMe(isChecked).apply()
        );

        mBinding.password.setOnEditorActionListener((textView, id, keyEvent) -> {
            if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                attemptLogin();
                return true;
            }
            return false;
        });

        mBinding.usernameLayout.setErrorEnabled(true);
        mBinding.passwordLayout.setErrorEnabled(true);

        mBinding.signInButton.setOnClickListener(view -> attemptLogin());

        mDontStartMain = getIntent().getBooleanExtra(EXTRA_DONT_START_MAIN, false);
        String errorMessage = getIntent().getStringExtra(EXTRA_ERROR_MESSAGE);
        if (errorMessage != null) {
            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
        }
    }

    private void attemptLogin() {
        mBinding.usernameLayout.setError(null);
        mBinding.passwordLayout.setError(null);
        mBinding.username.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
        mBinding.password.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);

        String username = mBinding.username.getText().toString();
        char[] password = new char[mBinding.password.getText().length()];
        mBinding.password.getText().getChars(0, password.length, password, 0);

        showProgress(true);
        mDisposable.add(
                QEDLogin.loginAsync(username, password, mFeature, this)
        );
    }

    private void showProgress(boolean show) {
        mBinding.loginForm.setVisibility(show ? View.GONE : View.VISIBLE);
        mBinding.loginProgress.setVisibility(show ? View.VISIBLE : View.GONE);
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
        setError(getString(R.string.cant_connect));
    }

    @Override
    public void onConnectionRegain() {
        setError(null);
    }

    @Override
    public void onPageReceived(@NonNull Boolean out) {
        assert out;

        if (!mDontStartMain) {
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
        }

        finish();
    }

    @Override
    public void onError(Boolean out, @NonNull Reason reason, @Nullable Throwable cause) {
        QEDPageReceiver.super.onError(out, reason, cause);

        showProgress(false);

        if (cause instanceof InvalidCredentialsException) {
            setError(getString(R.string.login_error_incorrect_data));
        } else {
            setError(getString(reason.getStringRes()));
        }
    }

    private void setError(String string) {
        mBinding.passwordLayout.setError(string);
        ViewUtils.setError(mBinding.password, string != null);
        if (string != null) mBinding.password.requestFocus();
    }
}

